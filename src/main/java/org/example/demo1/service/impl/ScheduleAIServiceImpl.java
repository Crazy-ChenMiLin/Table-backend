package org.example.demo1.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.example.demo1.config.MimoProperties;
import org.example.demo1.exception.BusinessException;
import org.example.demo1.model.dto.ScheduleResponse;
import org.example.demo1.service.ScheduleAIService;
import org.example.demo1.util.Base64Util;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleAIServiceImpl implements ScheduleAIService {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的课表信息提取助手。请仅根据上传的课表图片，提取全部课程信息，并以纯JSON格式返回，不包含任何解释、Markdown标记或额外文本。
            JSON必须使用以下结构，所有字段均为字符串类型（即使内容是数字）：
            {
              "semester": "学期名称，如'2025-2026学年第二学期'",
              "week": "教学周范围，如'第1-16周'",
              "courses": [
                {
                  "courseName": "课程全称",
                  "dayOfWeek": "星期几，用数字1-7表示，1=周一",
                  "startSection": "开始节次，如'1'",
                  "endSection": "结束节次，如'2'",
                  "location": "上课地点",
                  "teacher": "授课教师姓名",
                  "weekRange": "课程开设的周次范围，如'1-16周'"
                }
              ]
            }
            要求：
            - 如果图片中没有明确学期，则"semester"填"未知"
            - 如果无法确定教师或地点，填"未知"
            - 节次和星期必须是数字字符串，不可为空
            - 课程对象顺序请按照星期和开始节次升序排列
            - 严格输出JSON，不要加```json等标记
            """;

    private final MimoProperties mimoProperties;
    private final ObjectMapper objectMapper;

    public ScheduleAIServiceImpl(MimoProperties mimoProperties, ObjectMapper objectMapper) {
        this.mimoProperties = mimoProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ScheduleResponse recognize(MultipartFile image) {
        validateImage(image);

        if (!StringUtils.hasText(mimoProperties.getApiKey())) {
            throw new BusinessException(500, "未配置 MIMO_API_KEY 环境变量");
        }

        try {
            String dataUri = Base64Util.encodeToDataUri(image.getBytes(), image.getContentType());
            String rawJson = callMimo(dataUri);
            return parseScheduleResponse(rawJson);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(500, "图片读取失败");
        } catch (Exception exception) {
            if (isTimeout(exception)) {
                throw new BusinessException(500, "AI识别超时，请稍后重试，或换一张更清晰、文件更小的课表图片");
            }
            throw new BusinessException(500, "小米Mimo API调用失败（当前模型：" + mimoProperties.getModel() + "）：" + exception.getMessage());
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(400, "图片文件不能为空");
        }

        String contentType = image.getContentType();
        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)) {
            throw new BusinessException(400, "仅支持JPG、PNG格式的图片");
        }

        long maxBytes = (long) mimoProperties.getMaxImageSizeMb() * 1024 * 1024;
        if (image.getSize() > maxBytes) {
            throw new BusinessException(400, "图片大小不能超过" + mimoProperties.getMaxImageSizeMb() + "MB");
        }
    }

    private String callMimo(String imageDataUri) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_PROMPT));
        messages.add(UserMessage.from(ImageContent.from(imageDataUri, ImageContent.DetailLevel.AUTO)));

        Response<AiMessage> response = createChatModel().generate(messages);
        if (response == null || response.content() == null || !StringUtils.hasText(response.content().text())) {
            throw new BusinessException(500, "AI返回结果格式异常");
        }

        return response.content().text();
    }

    private ChatLanguageModel createChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(normalizeBaseUrl(mimoProperties.getBaseUrl()))
                .apiKey(mimoProperties.getApiKey())
                .modelName(mimoProperties.getModel())
                .temperature(mimoProperties.getTemperature())
                .timeout(Duration.ofSeconds(mimoProperties.getTimeoutSeconds()))
                .maxRetries(mimoProperties.getMaxRetries())
                .build();
    }

    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof java.io.InterruptedIOException) {
                return true;
            }

            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("timeout")) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private ScheduleResponse parseScheduleResponse(String rawJson) {
        String cleanedJson = cleanJson(rawJson);
        try {
            return objectMapper.readValue(cleanedJson, ScheduleResponse.class);
        } catch (IOException exception) {
            throw new BusinessException(500, "AI返回结果不是有效的JSON格式");
        }
    }

    private String cleanJson(String rawJson) {
        if (!StringUtils.hasText(rawJson)) {
            throw new BusinessException(500, "AI返回结果为空");
        }

        String cleaned = rawJson.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?\\s*", "");
            cleaned = cleaned.replaceFirst("\\s*```$", "");
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(500, "AI返回结果不是有效的JSON格式");
        }

        return cleaned.substring(start, end + 1);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://token-plan-cn.xiaomimimo.com/v1";
        }

        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
