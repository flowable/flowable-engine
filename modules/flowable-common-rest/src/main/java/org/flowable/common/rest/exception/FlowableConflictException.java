package org.flowable.common.rest.exception;

import org.flowable.common.engine.api.FlowableException;

public class FlowableConflictException extends FlowableException {

    private static final long serialVersionUID = 1L;

    public FlowableConflictException(String message) {
        super(message);
    }
}
