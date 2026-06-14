package org.example.demo1.service.support;

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
import org.example.demo1.logging.RecognitionTimingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class MimoClient {

    private static final Logger log = LoggerFactory.getLogger(MimoClient.class);

    private static final String SYSTEM_PROMPT = """
            浣犳槸涓€涓笓涓氱殑璇捐〃淇℃伅鎻愬彇鍔╂墜銆傝浠呮牴鎹笂浼犵殑璇捐〃鍥剧墖锛屾彁鍙栧叏閮ㄨ绋嬩俊鎭紝骞朵互绾疛SON鏍煎紡杩斿洖锛屼笉鍖呭惈浠讳綍瑙ｉ噴銆丮arkdown鏍囪鎴栭澶栨枃鏈€?
            JSON蹇呴』浣跨敤浠ヤ笅缁撴瀯锛屾墍鏈夊瓧娈靛潎涓哄瓧绗︿覆绫诲瀷锛堝嵆浣垮唴瀹规槸鏁板瓧锛夛細
            {
              "semester": "瀛︽湡鍚嶇О锛屽'2025-2026瀛﹀勾绗簩瀛︽湡'",
              "week": "鏁欏鍛ㄨ寖鍥达紝濡?绗?-16鍛?",
              "courses": [
                {
                  "courseName": "璇剧▼鍏ㄧО",
                  "dayOfWeek": "鏄熸湡鍑狅紝鐢ㄦ暟瀛?-7琛ㄧず锛?=鍛ㄤ竴",
                  "startSection": "寮€濮嬭妭娆★紝濡?1'",
                  "endSection": "缁撴潫鑺傛锛屽'2'",
                  "location": "涓婅鍦扮偣",
                  "teacher": "鎺堣鏁欏笀濮撳悕",
                  "weekRange": "璇剧▼寮€璁剧殑鍛ㄦ鑼冨洿锛屽'1-16鍛?"
                }
              ]
            }
            瑕佹眰锛?
            - 濡傛灉鍥剧墖涓病鏈夋槑纭鏈燂紝鍒?semester"濉?鏈煡"
            - 濡傛灉鏃犳硶纭畾鏁欏笀鎴栧湴鐐癸紝濉?鏈煡"
            - 鑺傛鍜屾槦鏈熷繀椤绘槸鏁板瓧瀛楃涓诧紝涓嶅彲涓虹┖
            - 璇剧▼瀵硅薄椤哄簭璇锋寜鐓ф槦鏈熷拰寮€濮嬭妭娆″崌搴忔帓鍒?
            - 涓ユ牸杈撳嚭JSON锛屼笉瑕佸姞```json绛夋爣璁?
            """;

    private final MimoProperties mimoProperties;
    private final RecognitionStepTimer stepTimer;

    public MimoClient(MimoProperties mimoProperties, RecognitionStepTimer stepTimer) {
        this.mimoProperties = mimoProperties;
        this.stepTimer = stepTimer;
    }

    public String recognize(PreparedImage image, RecognitionTimingContext timingContext) throws Exception {
        String normalizedBaseUrl = normalizeBaseUrl(mimoProperties.getBaseUrl());
        timingContext.putAttribute("mimo_model", mimoProperties.getModel());
        timingContext.putAttribute("mimo_base_url", normalizedBaseUrl);
        timingContext.putAttribute("mimo_timeout_seconds", mimoProperties.getTimeoutSeconds());
        timingContext.putAttribute("mimo_max_retries", mimoProperties.getMaxRetries());

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(SYSTEM_PROMPT));
        messages.add(UserMessage.from(ImageContent.from(image.dataUri(), ImageContent.DetailLevel.AUTO)));

        ChatLanguageModel chatModel = stepTimer.measure(timingContext, "build_model", this::createChatModel);

        long invokeStartNano = System.nanoTime();
        boolean success = false;
        Throwable failure = null;
        String exceptionType = "-";
        String exceptionMessage = "-";

        try {
            String text = stepTimer.measure(timingContext, "invoke_mimo", () -> {
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
            long invokeMimoMs = elapsedMillis(invokeStartNano);
            log.info(
                    "event=mimo_invoke_finished traceId={} success={} current_stage=invoke_mimo invoke_mimo_ms={} mimo_model={} mimo_base_url={} mimo_timeout_seconds={} mimo_max_retries={} timeout_detected={} exception_type={} exception_message={}",
                    timingContext.getTraceId(),
                    success,
                    invokeMimoMs,
                    mimoProperties.getModel(),
                    normalizedBaseUrl,
                    mimoProperties.getTimeoutSeconds(),
                    mimoProperties.getMaxRetries(),
                    failure != null && isTimeout(failure),
                    exceptionType,
                    exceptionMessage
            );
        }
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

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://token-plan-cn.xiaomimimo.com/v1";
        }

        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
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
