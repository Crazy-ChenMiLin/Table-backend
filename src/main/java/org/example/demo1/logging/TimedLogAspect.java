package org.example.demo1.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TimedLogAspect {

    private static final Logger log = LoggerFactory.getLogger(TimedLogAspect.class);

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
