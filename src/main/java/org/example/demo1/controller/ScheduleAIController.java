package org.example.demo1.controller;

import org.example.demo1.config.MimoProperties;
import org.example.demo1.model.dto.Result;
import org.example.demo1.model.dto.ScheduleResponse;
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
    private final MimoProperties mimoProperties;

    public ScheduleAIController(ScheduleAIService scheduleAIService, MimoProperties mimoProperties) {
        this.scheduleAIService = scheduleAIService;
        this.mimoProperties = mimoProperties;
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        return Result.success(Map.of(
                "baseUrl", mimoProperties.getBaseUrl(),
                "model", mimoProperties.getModel(),
                "apiKeyConfigured", StringUtils.hasText(mimoProperties.getApiKey()),
                "timeoutSeconds", mimoProperties.getTimeoutSeconds(),
                "maxRetries", mimoProperties.getMaxRetries()
        ));
    }

    @PostMapping("/recognize")
    public Result<ScheduleResponse> recognize(@RequestParam("image") MultipartFile image) {
        return Result.success(scheduleAIService.recognize(image));
    }
}
