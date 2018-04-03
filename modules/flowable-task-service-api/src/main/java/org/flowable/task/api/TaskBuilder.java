package org.flowable.task.api;

import java.util.Date;

/**
 * Wraps {@link TaskInfo} to the builder.
 * 
 */
public interface TaskBuilder {

    /**
     * Creates task instance according values set in the builder
     * 
     * @return task instance
     */
    Task create();
    
    /**
     * DB id of the task.
     */
    TaskBuilder id(String id);

    /**
     * Name or title of the task.
     */
    TaskBuilder name(String name);

    /**
     * Free text description of the task.
     */
    TaskBuilder description(String description);

    /**
     * Indication of how important/urgent this task is
     */
    TaskBuilder priority(int priority);

    /**
     * The userId of the person that is responsible for this task.
     */
    TaskBuilder owner(String ownerId);

    /**
     * The userId of the person to which this task is delegated.
     */
    TaskBuilder assignee(String assigneId);

    /**
     * Change due date of the task.
     */
    TaskBuilder dueDate(Date dueDate);

    /**
     * Change the category of the task. This is an optional field and allows to 'tag' tasks as belonging to a certain category.
     */
    TaskBuilder category(String category);

    /**
     * the parent task for which this task is a subtask
     */
    TaskBuilder parentTaskId(String parentTaskId);

    /**
     * Change the tenantId of the task
     */
    TaskBuilder tenantId(String tenantId);

    /**
     * Change the form key of the task
     */
    TaskBuilder formKey(String formKey);
    
    /**
     * task definition id to create task from
     */
    TaskBuilder taskDefinitionId(String taskDefinitionId);

    /**
     * task definition key to create task from
     */
    TaskBuilder taskDefinitionKey(String taskDefinitionKey);
}
