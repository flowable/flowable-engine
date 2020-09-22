package org.flowable.engine.assertions;

import java.util.Date;

import org.assertj.core.api.Assertions;
import org.flowable.engine.ProcessEngine;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;

/**
 * Assertions for a {@link Task}
 */
public class TaskAssert extends AbstractProcessAssert<TaskAssert, Task> {

    protected TaskAssert(ProcessEngine engine, Task actual) {
        super(engine, actual, TaskAssert.class);
    }

    protected static TaskAssert assertThat(ProcessEngine engine, Task actual) {
        return new TaskAssert(engine, actual);
    }

    @Override
    protected Task getCurrent() {
        return taskQuery().taskId(actual.getId()).singleResult();
    }

    /**
     * Verifies the expectation that the {@link Task} is currently not assigned to any particular user.
     *
     * @return this
     */
    public TaskAssert isNotAssigned() {
        Task current = getExistingCurrent();
        Assertions.assertThat(current.getAssignee())
                .overridingErrorMessage("Expecting %s not to be assigned, but found it to be assigned to user '%s'.",
                        toString(current),
                        current.getAssignee())
                .isNull();
        return this;
    }

    /**
     * Verifies the expectation that the {@link Task} is currently not assigned to the specified user.
     *
     * @param userId id of the user the task should not be currently assigned to.
     * @return this
     */
    public TaskAssert isNotAssignedTo(String userId) {
        Task current = getExistingCurrent();
        Assertions.assertThat(current.getAssignee())
                .overridingErrorMessage("Expecting %s not to be assigned to user '%s', but found it to be assigned to user '%s'.",
                        toString(current),
                        userId,
                        current.getAssignee())
                .isNotEqualTo(userId);
        return this;
    }

    /**
     * Verifies the expectation that the {@link Task} is currently assigned to the specified user.
     *
     * @param userId id of the user the task should be currently assigned to.
     * @return this
     */
    public TaskAssert isAssignedTo(String userId) {
        Task current = getExistingCurrent();
        Assertions
                .assertThat(current.getAssignee())
                .overridingErrorMessage("Expecting %s to be assigned to user '%s', but found it to be assigned to '%s'.",
                        toString(current),
                        userId,
                        current.getAssignee())
                .isEqualTo(userId);
        return this;
    }

    /**
     * Verifies the expectation that the {@link Task} is currently waiting to be assigned to a user of the specified candidate group.
     *
     * @param candidateGroupId id of a candidate group the task is waiting to be assigned to
     * @return this
     */
    public TaskAssert hasCandidateGroup(String candidateGroupId) {
        Assertions.assertThat(candidateGroupId).isNotNull();
        Task current = getExistingCurrent();
        TaskQuery taskQuery = taskQuery().taskId(actual.getId()).taskCandidateGroup(candidateGroupId);
        isNotAssigned();
        Task inGroup = taskQuery.singleResult();
        Assertions.assertThat(inGroup)
                .overridingErrorMessage("Expecting %s to have candidate group '%s', but found it not to have that candidate group.",
                        toString(current),
                        candidateGroupId)
                .isNotNull();
        return this;
    }

    /**
     * Verifies the expectation that the {@link Task} is currently waiting to be assigned to a specified candidate user.
     *
     * @param candidateUserId id of a candidate user the task is waiting to be assigned to
     * @return this
     */
    public TaskAssert hasCandidateUser(String candidateUserId) {
        Assertions.assertThat(candidateUserId).isNotNull();
        Task current = getExistingCurrent();
        TaskQuery taskQuery = taskQuery().taskId(actual.getId()).taskCandidateUser(candidateUserId);
        isNotAssigned();
        Task withUser = taskQuery.singleResult();
        Assertions.assertThat(withUser)
                .overridingErrorMessage("Expecting %s to have candidate user '%s', but found it not to have that candidate user.",
                        toString(current),
                        candidateUserId)
                .isNotNull();
        return this;
    }

    /**
     * Verifies the due date of a {@link Task}.
     *
     * @param dueDate the date the task should be due at
     * @return this
     */
    public TaskAssert hasDueDate(Date dueDate) {
        Task current = getExistingCurrent();
        //Assertions.assertThat(dueDate).isNotNull();
        Assertions.assertThat(current.getDueDate())
                .overridingErrorMessage("Expecting %s to be due at '%s', but found it to be due at '%s'.",
                        toString(current),
                        dueDate,
                        current.getDueDate()
                )
                .isEqualTo(dueDate);
        return this;
    }

    /**
     * Verifies the definition key of a {@link Task}. This key can be found
     * in the &lt;userTask id="myTaskDefinitionKey" .../&gt; attribute of the
     * process definition BPMN 2.0 XML file.
     *
     * @param taskDefinitionKey the expected value of the task/@id attribute
     * @return this
     */
    public TaskAssert hasDefinitionKey(String taskDefinitionKey) {
        Task current = getExistingCurrent();
        Assertions.assertThat(taskDefinitionKey).isNotNull();
        Assertions.assertThat(current.getTaskDefinitionKey())
                .overridingErrorMessage("Expecting %s to have definition key '%s', but found it to have '%s'.",
                        toString(current),
                        taskDefinitionKey,
                        current.getTaskDefinitionKey()
                ).isEqualTo(taskDefinitionKey);
        return this;
    }

    /**
     * Verifies the expectation that the {@link Task} has a specified form key.
     *
     * @param formKey the expected form key.
     * @return this
     */
    public TaskAssert hasFormKey(String formKey) {
        Task current = getExistingCurrent();
        String actualformKey = formService().getTaskFormKey(current.getProcessDefinitionId(), current.getTaskDefinitionKey());
        Assertions.assertThat(actualformKey)
                .overridingErrorMessage("Expecting %s to have a form key '%s', but found it to to have form key '%s'.", toString(current), formKey, actualformKey)
                .isEqualTo(formKey);
        return this;
    }

    /**
     * Verifies the internal id of a {@link Task}.
     *
     * @param id the expected value of the internal task id
     * @return this
     */
    public TaskAssert hasId(String id) {
        Task current = getExistingCurrent();
        Assertions.assertThat(id).isNotNull();
        Assertions.assertThat(current.getId())
                .overridingErrorMessage("Expecting %s to have internal id '%s', but found it to be '%s'.",
                        toString(current),
                        id,
                        current.getId()
                ).isEqualTo(id);
        return this;
    }

    /**
     * Verifies the name (label) of a {@link Task}. This name can be found
     * in the &lt;userTask name="myName" .../&gt; attribute of the
     * process definition BPMN 2.0 XML file.
     *
     * @param name the expected value of the name
     * @return this
     */
    public TaskAssert hasName(String name) {
        Task current = getExistingCurrent();
        Assertions.assertThat(name).isNotNull();
        Assertions.assertThat(current.getName())
                .overridingErrorMessage("Expecting %s to have name '%s', but found it to be '%s'.",
                        toString(current),
                        name,
                        current.getName()
                ).isEqualTo(name);
        return this;
    }

    /**
     * Verifies the description of a {@link Task}. This description can be found in the
     * &lt;userTask&gt;&lt;documentation&gt;description&lt;/documentation&gt;&lt;/userTask&gt;
     * element of the process definition BPMN 2.0 XML file.
     *
     * @param description the expected value of the description
     * @return this
     */
    public TaskAssert hasDescription(String description) {
        Task current = getExistingCurrent();
        //Assertions.assertThat(description).isNotNull();
        Assertions.assertThat(current.getDescription())
                .overridingErrorMessage("Expecting %s to have description '%s', but found it to be '%s'.",
                        toString(current),
                        description,
                        current.getDescription())
                .isEqualTo(description);
        return this;
    }

    @Override
    protected String toString(Task task) {
        return task != null ?
                String.format("%s {" +
                                "id='%s', " +
                                "processInstanceId='%s', " +
                                "taskDefinitionKey='%s', " +
                                "name='%s'}",
                        Task.class.getSimpleName(),
                        task.getId(),
                        task.getProcessInstanceId(),
                        task.getTaskDefinitionKey(),
                        task.getName()
                ) : null;
    }

    /*
     * TaskQuery, automatically narrowed to {@link ProcessInstance} of actual {@link Task}.
     */
    @Override
    protected TaskQuery taskQuery() {
        return super.taskQuery().processInstanceId(actual.getProcessInstanceId());
    }

}
