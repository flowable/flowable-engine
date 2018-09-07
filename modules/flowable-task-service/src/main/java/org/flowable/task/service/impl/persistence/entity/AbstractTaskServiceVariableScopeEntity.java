package org.flowable.task.service.impl.persistence.entity;

import org.flowable.variable.service.impl.persistence.entity.VariableScopeImpl;

public abstract class AbstractTaskServiceVariableScopeEntity extends VariableScopeImpl {

    public String getIdPrefix() {
        return TaskServiceEntityConstants.TASK_SERVICE_ID_PREFIX;
    }
}
