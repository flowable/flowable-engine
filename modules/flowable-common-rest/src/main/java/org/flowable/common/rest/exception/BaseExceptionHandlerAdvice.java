package org.flowable.common.rest.exception;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.FlowableTaskAlreadyClaimedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Base class for ExceptionHandlerAdvice controllers From http://spring.io/blog/2013/11/01/exception-handling-in-spring-mvc
 * 
 * @author Tijs Rademakers
 */
@ControllerAdvice
public class BaseExceptionHandlerAdvice {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseExceptionHandlerAdvice.class);

    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE) // 415
    @ExceptionHandler(FlowableContentNotSupportedException.class)
    @ResponseBody
    public ErrorInfo handleNotSupported(FlowableContentNotSupportedException e) {
        return new ErrorInfo("Content is not supported", e);
    }

    @ResponseStatus(HttpStatus.CONFLICT) // 409
    @ExceptionHandler(FlowableConflictException.class)
    @ResponseBody
    public ErrorInfo handleConflict(FlowableConflictException e) {
        return new ErrorInfo("Conflict", e);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND) // 404
    @ExceptionHandler(FlowableObjectNotFoundException.class)
    @ResponseBody
    public ErrorInfo handleNotFound(FlowableObjectNotFoundException e) {
        return new ErrorInfo("Not found", e);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN) // 403
    @ExceptionHandler(FlowableForbiddenException.class)
    @ResponseBody
    public ErrorInfo handleForbidden(FlowableForbiddenException e) {
        return new ErrorInfo("Forbidden", e);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    @ExceptionHandler(FlowableIllegalArgumentException.class)
    @ResponseBody
    public ErrorInfo handleIllegal(FlowableIllegalArgumentException e) {
        return new ErrorInfo("Bad request", e);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400
    @ExceptionHandler(HttpMessageConversionException.class)
    @ResponseBody
    public ErrorInfo handleBadMessageConversion(HttpMessageConversionException e) {
        return new ErrorInfo("Bad request", e);
    }

    @ResponseStatus(HttpStatus.CONFLICT) // 409
    @ExceptionHandler(FlowableTaskAlreadyClaimedException.class)
    @ResponseBody
    public ErrorInfo handleTaskAlreadyClaimed(FlowableTaskAlreadyClaimedException e) {
        return new ErrorInfo("Task was already claimed", e);
    }

    // Fall back

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // 500
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ErrorInfo handleOtherException(Exception e) {
        LOGGER.error("Unhandled exception", e);
        return new ErrorInfo("Internal server error", e);
    }

}
