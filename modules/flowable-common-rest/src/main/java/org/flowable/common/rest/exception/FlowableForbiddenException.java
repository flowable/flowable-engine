package org.flowable.common.rest.exception;

import org.flowable.engine.common.api.FlowableException;

public class FlowableForbiddenException extends FlowableException {

    private static final long serialVersionUID = 1L;

    public FlowableForbiddenException(String message) {
        super(message);
    }
}
