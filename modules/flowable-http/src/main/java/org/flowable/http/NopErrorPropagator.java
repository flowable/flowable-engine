package org.flowable.http;

import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.engine.common.api.variable.VariableContainer;

import java.util.List;

/**
 * This class do not propagate any error
 *
 * @author martin.grofcik
 */
public class NopErrorPropagator implements ErrorPropagator {
    @Override
    public void propagateError(VariableContainer execution, String code) {
        // NOP
    }

    @Override
    public boolean mapException(Exception e, VariableContainer execution, List<MapExceptionEntry> exceptionMap) {
        return false;
    }
}
