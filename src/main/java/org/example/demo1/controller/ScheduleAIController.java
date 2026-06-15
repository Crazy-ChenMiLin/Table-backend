package org.example.demo1.controller;

import org.example.demo1.common.aspect.TimedLog;
import org.example.demo1.config.AiProperties;
import org.example.demo1.entity.response.AiCustomEventResponse;
import org.example.demo1.entity.response.Result;
import org.example.demo1.service.ScheduleAIService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/ai/schedule")
public class ScheduleAIController {

    private final ScheduleAIService scheduleAIService;
    private final AiProperties aiProperties;

    public ScheduleAIController(ScheduleAIService scheduleAIService, AiProperties aiProperties) {
        this.scheduleAIService = scheduleAIService;
        this.aiProperties = aiProperties;
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        return Result.success(Map.of(
                "baseUrl", aiProperties.getBaseUrl(),
                "model", aiProperties.getModel(),
                "apiKeyConfigured", StringUtils.hasText(aiProperties.getApiKey()),
                "timeoutSeconds", aiProperties.getTimeoutSeconds(),
                "maxRetries", aiProperties.getMaxRetries()
        ));
    }

    @PostMapping("/recognize")
    @TimedLog("schedule_ai_controller_recognize")
    public Result<AiCustomEventResponse> recognize(
            @RequestParam("image") MultipartFile image,
            @RequestParam("yearTerm") String yearTerm
    ) {
        return Result.success(scheduleAIService.recognize(image, yearTerm));
    }
}
