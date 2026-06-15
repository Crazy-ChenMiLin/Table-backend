package org.example.demo1.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.demo1.common.aspect.TimedLog;
import org.example.demo1.common.context.RecognitionTimingContext;
import org.example.demo1.common.timing.RecognitionStepTimer;
import org.example.demo1.entity.bo.PreparedImage;
import org.example.demo1.entity.response.AiCustomEventItem;
import org.example.demo1.entity.response.AiCustomEventResponse;
import org.example.demo1.exception.BusinessException;
import org.example.demo1.infra.ai.AiCustomEventResponseParser;
import org.example.demo1.infra.ai.AiModelClient;
import org.example.demo1.infra.image.ImagePreparationService;
import org.example.demo1.service.ScheduleAIService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
public class ScheduleAIServiceImpl implements ScheduleAIService {

    private final ImagePreparationService imagePreparationService;
    private final AiModelClient aiModelClient;
    private final AiCustomEventResponseParser aiCustomEventResponseParser;
    private final RecognitionStepTimer stepTimer;

    public ScheduleAIServiceImpl(
            ImagePreparationService imagePreparationService,
            AiModelClient aiModelClient,
            AiCustomEventResponseParser aiCustomEventResponseParser,
            RecognitionStepTimer stepTimer
    ) {
        this.imagePreparationService = imagePreparationService;
        this.aiModelClient = aiModelClient;
        this.aiCustomEventResponseParser = aiCustomEventResponseParser;
        this.stepTimer = stepTimer;
    }

    @Override
    @TimedLog("schedule_ai_service_recognize")
    public AiCustomEventResponse recognize(MultipartFile image, String yearTerm) {
        RecognitionTimingContext timingContext = RecognitionTimingContext.start("schedule_ai_recognize");
        long serviceStartNano = System.nanoTime();
        timingContext.putAttribute("image_content_type", image == null ? null : image.getContentType());
        timingContext.putAttribute("image_size_bytes", image == null ? null : image.getSize());
        timingContext.putAttribute("year_term", yearTerm);

        try {
            validateYearTerm(yearTerm);
            PreparedImage preparedImage = imagePreparationService.prepare(image, timingContext);
            String rawJson = aiModelClient.recognize(preparedImage, timingContext);
            AiCustomEventResponse response = stepTimer.measure(
                    timingContext,
                    "parse_json",
                    () -> aiCustomEventResponseParser.parse(rawJson)
            );
            enrichBackendFields(response, yearTerm);
            timingContext.markSuccess();
            return response;
        } catch (BusinessException exception) {
            timingContext.markFailure(exception);
            throw exception;
        } catch (IOException exception) {
            timingContext.markFailure(exception);
            throw new BusinessException(500, "图片读取失败");
        } catch (Exception exception) {
            timingContext.markFailure(exception);
            if (isTimeout(exception)) {
                throw new BusinessException(500, "AI识别超时，请稍后重试，或换一张更清晰、文件更小的课表图片");
            }
            throw new BusinessException(500, "AI API调用失败：" + exception.getMessage());
        } finally {
            stepTimer.recordTotal(timingContext, "service_total", serviceStartNano);
            log.info(timingContext.toLogFields());
        }
    }

    private void validateYearTerm(String yearTerm) {
        if (!StringUtils.hasText(yearTerm)) {
            throw new BusinessException(400, "yearTerm不能为空");
        }
    }

    private void enrichBackendFields(AiCustomEventResponse response, String yearTerm) {
        if (response == null || response.getEvents() == null) {
            return;
        }

        for (AiCustomEventItem event : response.getEvents()) {
            event.setYearTerm(yearTerm);
            // Demo only: the real project generates event id through MyBatis-Plus IdType.ASSIGN_UUID.
            event.setEventID(UUID.randomUUID().toString().replace("-", ""));
        }
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
}
