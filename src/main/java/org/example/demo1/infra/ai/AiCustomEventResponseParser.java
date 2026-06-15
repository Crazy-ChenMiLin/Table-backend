package org.example.demo1.infra.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.demo1.entity.response.AiCustomEventResponse;
import org.example.demo1.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class AiCustomEventResponseParser {

    private final ObjectMapper objectMapper;

    public AiCustomEventResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiCustomEventResponse parse(String rawJson) {
        String cleanedJson = cleanJson(rawJson);
        try {
            return objectMapper.readValue(cleanedJson, AiCustomEventResponse.class);
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
