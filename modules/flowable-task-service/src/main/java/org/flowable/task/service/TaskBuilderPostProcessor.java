package org.flowable.task.service;

import org.flowable.task.api.TaskBuilder;

/**
 * Function to enrich {@link TaskBuilder} instance with additional info
 */
public interface TaskBuilderPostProcessor {
    TaskBuilder enrichTaskBuilder(TaskBuilder taskBuilder);
}
