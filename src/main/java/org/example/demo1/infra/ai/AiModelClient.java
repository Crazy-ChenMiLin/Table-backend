package org.example.demo1.infra.ai;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.example.demo1.common.context.RecognitionTimingContext;
import org.example.demo1.common.timing.RecognitionStepTimer;
import org.example.demo1.config.AiProperties;
import org.example.demo1.entity.bo.PreparedImage;
import org.example.demo1.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AiModelClient {

    private static final String SYSTEM_PROMPT = """
            你是一个专业的课表图片识别助手。请仅根据上传的课表图片提取课程信息，并严格返回纯 JSON，不要包含解释、Markdown 或额外文本。
            JSON 必须使用以下结构，字段名必须完全一致：
            {
              "events": [
                {
                  "weekList": ["1", "2", "3"],
                  "weekDay": "1",
                  "sessionStart": "1",
                  "sessionLast": "2",
                  "eventName": "课程名称",
                  "address": "上课地点",
                  "memberName": "教师姓名"
                }
              ]
            }
            字段要求：
            - 不要输出 eventID，新增事件由后端生成。
            - 不要输出 yearTerm，学期由用户请求传入，后端统一填充。
            - weekList 必须是字符串数组，例如 ["1", "2", "3"]；不要输出 "1-16周" 这种自然语言。
            - weekDay 必须是 "1" 到 "7" 的数字字符串，"1" 表示周一。
            - sessionStart 必须是开始节次数字字符串。
            - sessionLast 必须是持续节数，不是结束节次。例如第 1-2 节输出 sessionStart="1", sessionLast="2"。
            - eventName 对应课程名称，address 对应地点，memberName 对应教师；无法识别时填 "未知"。
            - 只输出 JSON，不要加 ```json 标记。
            """;

    private final AiProperties aiProperties;
    private final RecognitionStepTimer stepTimer;

    public AiModelClient(AiProperties aiProperties, RecognitionStepTimer stepTimer) {
        this.aiProperties = aiProperties;
        this.stepTimer = stepTimer;
    }

    public String recognize(PreparedImage image, RecognitionTimingContext timingContext) throws Exception {
        String normalizedBaseUrl = normalizeBaseUrl(aiProperties.getBaseUrl());
        timingContext.putAttribute("ai_model", aiProperties.getModel());
        timingContext.putAttribute("ai_base_url", normalizedBaseUrl);
        timingContext.putAttribute("ai_timeout_seconds", aiProperties.getTimeoutSeconds());
        timingContext.putAttribute("ai_max_retries", aiProperties.getMaxRetries());

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_PROMPT));
        messages.add(UserMessage.from(ImageContent.from(image.dataUri(), ImageContent.DetailLevel.AUTO)));

        ChatLanguageModel chatModel = stepTimer.measure(timingContext, "build_ai_model", this::createChatModel);

        long invokeStartNano = System.nanoTime();
        boolean success = false;
        Throwable failure = null;
        String exceptionType = "-";
        String exceptionMessage = "-";

        try {
            String text = stepTimer.measure(timingContext, "invoke_ai_model", () -> {
                Response<AiMessage> response = chatModel.generate(messages);
                if (response == null || response.content() == null || !StringUtils.hasText(response.content().text())) {
                    throw new BusinessException(500, "AI返回结果格式异常");
                }
                return response.content().text();
            });
            success = true;
            return text;
        } catch (Exception exception) {
            failure = exception;
            exceptionType = exception.getClass().getSimpleName();
            exceptionMessage = sanitize(exception.getMessage());
            throw exception;
        } finally {
            long invokeAiModelMs = elapsedMillis(invokeStartNano);
            log.info(
                    "event=ai_model_invoke_finished traceId={} success={} current_stage=invoke_ai_model invoke_ai_model_ms={} ai_model={} ai_base_url={} ai_timeout_seconds={} ai_max_retries={} timeout_detected={} exception_type={} exception_message={}",
                    timingContext.getTraceId(),
                    success,
                    invokeAiModelMs,
                    aiProperties.getModel(),
                    normalizedBaseUrl,
                    aiProperties.getTimeoutSeconds(),
                    aiProperties.getMaxRetries(),
                    failure != null && isTimeout(failure),
                    exceptionType,
                    exceptionMessage
            );
        }
    }

    private ChatLanguageModel createChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(requiredConfig("AI_BASE_URL", normalizeBaseUrl(aiProperties.getBaseUrl())))
                .apiKey(requiredConfig("AI_API_KEY", aiProperties.getApiKey()))
                .modelName(requiredConfig("AI_MODEL", aiProperties.getModel()))
                .temperature(aiProperties.getTemperature())
                .timeout(Duration.ofSeconds(aiProperties.getTimeoutSeconds()))
                .maxRetries(aiProperties.getMaxRetries())
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }

        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String requiredConfig(String name, String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(500, "未配置 " + name + " 环境变量或 ai 配置项");
        }

        return value;
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

    private long elapsedMillis(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000;
    }

    private String sanitize(String message) {
        if (!StringUtils.hasText(message)) {
            return "-";
        }

        return message.trim().replace(' ', '_').replace('\r', '_').replace('\n', '_');
    }
}