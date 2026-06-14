package org.example.demo1.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.demo1.exception.BusinessException;
import org.example.demo1.model.dto.ScheduleResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class ScheduleResponseParser {

    private final ObjectMapper objectMapper;

    public ScheduleResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ScheduleResponse parse(String rawJson) {
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
}
