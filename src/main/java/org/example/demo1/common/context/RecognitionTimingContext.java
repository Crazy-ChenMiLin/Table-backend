package org.example.demo1.common.context;

import org.example.demo1.common.filter.TraceIdFilter;

import org.slf4j.MDC;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public class RecognitionTimingContext {

    private final String traceId;
    private final String operation;
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private final Map<String, Long> stageDurations = new LinkedHashMap<>();
    private String currentStage = "init";
    private boolean success;
    private String exceptionType = "-";
    private String exceptionMessage = "-";

    private RecognitionTimingContext(String operation) {
        this.traceId = MDC.get(TraceIdFilter.TRACE_ID_KEY);
        this.operation = operation;
    }

    public static RecognitionTimingContext start(String operation) {
        return new RecognitionTimingContext(operation);
    }

    public String getTraceId() {
        return traceId;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public void putAttribute(String key, Object value) {
        attributes.put(key, sanitize(value));
    }

    public void recordStage(String key, long durationMs) {
        stageDurations.put(key, durationMs);
    }

    public void markSuccess() {
        this.success = true;
        this.exceptionType = "-";
        this.exceptionMessage = "-";
    }

    public void markFailure(Throwable throwable) {
        this.success = false;
        this.exceptionType = throwable.getClass().getSimpleName();
        this.exceptionMessage = sanitize(throwable.getMessage());
    }

    public String toLogFields() {
        StringJoiner joiner = new StringJoiner(" ");
        joiner.add("event=schedule_ai_finished");
        joiner.add("traceId=" + sanitize(traceId));
        joiner.add("operation=" + sanitize(operation));
        joiner.add("success=" + success);
        joiner.add("current_stage=" + sanitize(currentStage));
        joiner.add("exception_type=" + exceptionType);
        joiner.add("exception_message=" + exceptionMessage);
        appendEntries(joiner, attributes);
        appendEntries(joiner, stageDurations);
        return joiner.toString();
    }

    private void appendEntries(StringJoiner joiner, Map<String, ?> map) {
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            joiner.add(entry.getKey() + "=" + sanitize(entry.getValue()));
        }
    }

    private String sanitize(Object value) {
        if (value == null) {
            return "-";
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return "-";
        }

        return text.replace(' ', '_').replace('\r', '_').replace('\n', '_');
    }
}
