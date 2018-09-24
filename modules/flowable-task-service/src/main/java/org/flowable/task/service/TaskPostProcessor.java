package org.flowable.task.service;

import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * Function to enrich {@link TaskEntity} instance with additional info
 */
public interface TaskPostProcessor {
    TaskEntity enrich(TaskEntity taskEntityToEnrich);
}
