package org.flowable.task.service.impl;

import org.flowable.task.service.TaskPostProcessor;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

public class DefaultTaskPostProcessor implements TaskPostProcessor {
    @Override
    public TaskEntity enrich(TaskEntity taskEntity) {
        return taskEntity;
    }
}
