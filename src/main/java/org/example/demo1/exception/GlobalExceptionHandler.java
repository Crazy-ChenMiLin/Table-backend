package org.example.demo1.exception;

import org.example.demo1.model.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusinessException(BusinessException exception) {
        log.warn(
                "event=request_exception traceId={} exception_type={} message={}",
                MDC.get("traceId"),
                exception.getClass().getSimpleName(),
                exception.getMessage()
        );
        return Result.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MaxUploadSizeExceededException.class,
            MultipartException.class
    })
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBadRequest(Exception exception) {
        log.warn(
                "event=request_exception traceId={} exception_type={} message={}",
                MDC.get("traceId"),
                exception.getClass().getSimpleName(),
                exception.getMessage()
        );
        return Result.error(400, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleException(Exception exception) {
        log.error(
                "event=request_exception traceId={} exception_type={} message={}",
                MDC.get("traceId"),
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                exception
        );
        return Result.error(500, exception.getMessage());
    }
}
