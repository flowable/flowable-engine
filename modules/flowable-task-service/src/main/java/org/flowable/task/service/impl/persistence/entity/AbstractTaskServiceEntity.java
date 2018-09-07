package org.flowable.task.service.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

public abstract class AbstractTaskServiceEntity extends AbstractEntity {

    public String getIdPrefix() {
        return TaskServiceEntityConstants.TASK_SERVICE_ID_PREFIX;
    }
}
