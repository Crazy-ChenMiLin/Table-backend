package org.example.demo1.service;

import org.example.demo1.entity.response.AiCustomEventResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ScheduleAIService {

    AiCustomEventResponse recognize(MultipartFile image, String yearTerm);
}
