package org.flowable.http.impl;

import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.engine.common.api.variable.VariableContainer;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.http.ErrorPropagator;

import java.util.List;

/**
 * This class propagates process errors
 */
public class ProcessErrorPropagator implements ErrorPropagator {
    @Override
    public void propagateError(VariableContainer execution, String code) {
            ErrorPropagation.propagateError("HTTP" + code, (DelegateExecution) execution);
    }

    @Override
    public boolean mapException(Exception e, ExecutionEntity execution, List<MapExceptionEntry> exceptionMap) {
        return ErrorPropagation.mapException(e, execution, exceptionMap);
    }
}
