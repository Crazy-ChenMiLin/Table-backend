package org.example.demo1.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.demo1.entity.request.BatchCustomEventRequest;
import org.example.demo1.entity.request.CustomEventRequest;
import org.example.demo1.entity.response.Result;
import org.example.demo1.exception.BusinessException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/courseSchedule")
@Slf4j
public class CustomEventController {

    @PostMapping("/addCustomEvent")
    public Result<String> addCustomEvent(@RequestBody CustomEventRequest request) {
        validateCustomEvent(request);

        String eventID = StringUtils.hasText(request.getEventID())
                ? request.getEventID()
                : UUID.randomUUID().toString().replace("-", "");
        log.info("event=custom_event_added eventID={} yearTerm={} eventName={}",
                eventID,
                request.getYearTerm(),
                request.getEventName()
        );
        return Result.success(eventID);
    }

    @PostMapping("/batchAddCustomEvent")
    public Result<String> batchAddCustomEvent(@RequestBody BatchCustomEventRequest request) {
        if (request == null || request.getEvents() == null || request.getEvents().isEmpty()) {
            throw new BusinessException(400, "events不能为空");
        }

        for (int i = 0; i < request.getEvents().size(); i++) {
            try {
                Result<String> result = addCustomEvent(request.getEvents().get(i));
                if (result.getCode() != 200) {
                    return Result.error(10001, "第" + (i + 1) + "条新增失败：" + result.getMessage());
                }
            } catch (BusinessException exception) {
                return Result.error(exception.getCode(), "第" + (i + 1) + "条新增失败：" + exception.getMessage());
            } catch (RuntimeException exception) {
                return Result.error(10001, "第" + (i + 1) + "条新增失败：" + exception.getMessage());
            }
        }

        return Result.success("批量新增成功");
    }

    private void validateCustomEvent(CustomEventRequest request) {
        if (request == null) {
            throw new BusinessException(10001, "事件不能为空");
        }
        if (!StringUtils.hasText(request.getYearTerm())) {
            throw new BusinessException(10001, "yearTerm不能为空");
        }
        if (request.getWeekList() == null || request.getWeekList().isEmpty()) {
            throw new BusinessException(10001, "weekList不能为空");
        }
        if (!StringUtils.hasText(request.getWeekDay())) {
            throw new BusinessException(10001, "weekDay不能为空");
        }
        if (!StringUtils.hasText(request.getSessionStart())) {
            throw new BusinessException(10001, "sessionStart不能为空");
        }
        if (!StringUtils.hasText(request.getSessionLast())) {
            throw new BusinessException(10001, "sessionLast不能为空");
        }
        if (!StringUtils.hasText(request.getEventName())) {
            throw new BusinessException(10001, "eventName不能为空");
        }
        if (!StringUtils.hasText(request.getAddress())) {
            throw new BusinessException(10001, "address不能为空");
        }
        if (!StringUtils.hasText(request.getMemberName())) {
            throw new BusinessException(10001, "memberName不能为空");
        }

        int weekDay = parsePositiveInt(request.getWeekDay(), "weekDay");
        if (weekDay < 1 || weekDay > 7) {
            throw new BusinessException(10001, "weekDay不在范围");
        }

        int sessionStart = parsePositiveInt(request.getSessionStart(), "sessionStart");
        if (sessionStart < 1 || sessionStart > 10) {
            throw new BusinessException(10001, "sessionStart不在范围");
        }

        int sessionLast = parsePositiveInt(request.getSessionLast(), "sessionLast");
        if (sessionLast < 1 || sessionStart + sessionLast > 11) {
            throw new BusinessException(10001, "sessionLast不在范围或持续节次过长");
        }

        if (request.getEventName().length() > 20
                || request.getAddress().length() > 20
                || request.getMemberName().length() > 20) {
            throw new BusinessException(10001, "事件名称、地点、成员名称不能超过20个字符");
        }
    }

    private int parsePositiveInt(String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(10001, fieldName + "必须是数字");
        }
    }
}
