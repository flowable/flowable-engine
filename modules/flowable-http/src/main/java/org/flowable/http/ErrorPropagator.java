package org.flowable.http;

import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.engine.common.api.variable.VariableContainer;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;

import java.util.List;

/**
 * This interface provides methods to propagate an error to the execution
 */
public interface ErrorPropagator {

    void propagateError(VariableContainer execution, String code);

    boolean mapException(Exception e, VariableContainer execution, List<MapExceptionEntry> exceptionMap);
}
