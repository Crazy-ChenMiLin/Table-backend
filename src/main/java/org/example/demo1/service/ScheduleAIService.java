package org.example.demo1.service;

import org.example.demo1.model.dto.ScheduleResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ScheduleAIService {

    ScheduleResponse recognize(MultipartFile image);
}
