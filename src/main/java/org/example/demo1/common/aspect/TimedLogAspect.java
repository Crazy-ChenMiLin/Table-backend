package org.example.demo1.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.example.demo1.common.filter.TraceIdFilter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class TimedLogAspect {

    @Around("@annotation(timedLog)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, TimedLog timedLog) throws Throwable {
        long startNano = System.nanoTime();
        boolean success = false;
        String exceptionType = "-";

        try {
            Object result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Throwable throwable) {
            exceptionType = throwable.getClass().getSimpleName();
            throw throwable;
        } finally {
            long durationMs = (System.nanoTime() - startNano) / 1_000_000;
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            log.info(
                    "event=timed_method_finished traceId={} name={} class={} method={} success={} duration_ms={} exception_type={}",
                    MDC.get(TraceIdFilter.TRACE_ID_KEY),
                    timedLog.value(),
                    signature.getDeclaringType().getSimpleName(),
                    signature.getMethod().getName(),
                    success,
                    durationMs,
                    exceptionType
            );
        }
    }
}
