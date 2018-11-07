package org.flowable.common.rest.exception;

import org.flowable.common.engine.api.FlowableException;

public class FlowableContentNotSupportedException extends FlowableException {

    private static final long serialVersionUID = 1L;

    public FlowableContentNotSupportedException(String message) {
        super(message);
    }
}
