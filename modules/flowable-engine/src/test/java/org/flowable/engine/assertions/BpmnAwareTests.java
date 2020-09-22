package org.flowable.engine.assertions;

import static java.lang.String.format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.test.AbstractFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;

/**
 * Convenience class to access only Flowable *BPMN* related Assertions
 * PLUS helper methods.
 * <p>
 * Use it with a static import:
 * <p>
 * import org.flowable.assertions.bpmn.BpmnAwareTests.*;
 */
public class BpmnAwareTests extends AbstractFlowableTestCase {

    private static final String DUPLICATED_NAME = "$DUPLICATED_NAME$";
    public static final long DEFAULT_LOCK_DURATION_EXTERNAL_TASK = 30L * 1000L;// 30 seconds
    public static final String DEFAULT_WORKER_EXTERNAL_TASK = "anonymousWorker";

    private BpmnAwareTests() {
    }

    public static ProcessEngine processEngine() {
        return processEngine;
    }

    /**
     * Assert that... the given ProcessDefinition meets your expectations.
     *
     * @param actual ProcessDefinition under test
     * @return Assert object offering ProcessDefinition specific assertions.
     */
    public static ProcessDefinitionAssert assertThat(ProcessDefinition actual) {
        return ProcessDefinitionAssert.assertThat(processEngine(), actual);
    }

    /**
     * Assert that... the given ProcessInstance meets your expectations.
     *
     * @param actual ProcessInstance under test
     * @return Assert object offering ProcessInstance specific assertions.
     */
    public static ProcessInstanceAssert assertThat(ProcessInstance actual) {
        return ProcessInstanceAssert.assertThat(processEngine(), actual);
    }

    /**
     * Assert that... the given Task meets your expectations.
     *
     * @param actual Task under test
     * @return Assert object offering Task specific assertions.
     */
    public static TaskAssert assertThat(Task actual) {
        return TaskAssert.assertThat(processEngine(), actual);
    }

    /**
     * Assert that... the given Job meets your expectations.
     *
     * @param actual Job under test
     * @return Assert object offering Job specific assertions.
     */
    public static JobAssert assertThat(Job actual) {
        return JobAssert.assertThat(processEngine(), actual);
    }

    /**
     * Helper method to easily access RuntimeService
     *
     * @return RuntimeService of process engine bound to this testing thread
     * @see org.flowable.engine.RuntimeService
     */
    public static RuntimeService runtimeService() {
        return processEngine().getRuntimeService();
    }

    /**
     * Helper method to easily access FormService
     *
     * @return FormService of process engine bound to this testing thread
     * @see org.flowable.engine.FormService
     */
    public static FormService formService() {
        return processEngine().getFormService();
    }

    /**
     * Helper method to easily access HistoryService
     *
     * @return HistoryService of process engine bound to this testing thread
     * @see org.flowable.engine.HistoryService
     */
    public static HistoryService historyService() {
        return processEngine().getHistoryService();
    }

    /**
     * Helper method to easily access IdentityService
     *
     * @return IdentityService of process engine bound to this testing thread
     * @see org.flowable.engine.IdentityService
     */
    public static IdentityService identityService() {
        return processEngine().getIdentityService();
    }

    /**
     * Helper method to easily access ManagementService
     *
     * @return ManagementService of process engine bound to this testing thread
     * @see org.flowable.engine.ManagementService
     */
    public static ManagementService managementService() {
        return processEngine().getManagementService();
    }

    /**
     * Helper method to easily access RepositoryService
     *
     * @return RepositoryService of process engine bound to this testing thread
     * @see org.flowable.engine.RepositoryService
     */
    public static RepositoryService repositoryService() {
        return processEngine().getRepositoryService();
    }

    /**
     * Helper method to easily access TaskService
     *
     * @return TaskService of process engine bound to this testing thread
     * @see org.flowable.engine.TaskService
     */
    public static TaskService taskService() {
        return processEngine().getTaskService();
    }

    /**
     * Helper method to easily create a new TaskQuery
     *
     * @return new TaskQuery for process engine bound to this testing thread
     * @see org.flowable.engine.task.TaskQuery
     */
    public static TaskQuery taskQuery() {
        return taskService().createTaskQuery();
    }

    /**
     * Helper method to easily create a new JobQuery
     *
     * @return new JobQuery for process engine bound to this testing thread
     * @see org.flowable.engine.runtime.JobQuery
     */
    public static JobQuery jobQuery() {
        return managementService().createJobQuery();
    }

    /**
     * Helper method to easily create a new ProcessInstanceQuery
     *
     * @return new ProcessInstanceQuery for process engine bound to this
     * testing thread
     * @see org.flowable.engine.runtime.ProcessInstanceQuery
     */
    public static ProcessInstanceQuery processInstanceQuery() {
        return runtimeService().createProcessInstanceQuery();
    }

    /**
     * Helper method to easily create a new ProcessDefinitionQuery
     *
     * @return new ProcessDefinitionQuery for process engine bound to this
     * testing thread
     * @see org.flowable.engine.repository.ProcessDefinitionQuery
     */
    public static ProcessDefinitionQuery processDefinitionQuery() {
        return repositoryService().createProcessDefinitionQuery();
    }

    /**
     * Helper method to easily create a new ExecutionQuery
     *
     * @return new ExecutionQuery for process engine bound to this testing thread
     * @see org.flowable.engine.runtime.ExecutionQuery
     */
    public static ExecutionQuery executionQuery() {
        return runtimeService().createExecutionQuery();
    }

    /**
     * Helper method to easily construct a map of process variables
     *
     * @param key                  (obligatory) key of first process variable
     * @param value                (obligatory) value of first process variable
     * @param furtherKeyValuePairs (optional) key/value pairs for further process variables
     * @return a map of process variables by passing a list of String, Object key value pairs.
     */
    public static Map<String, Object> withVariables(String key, Object value, Object... furtherKeyValuePairs) {
        if (key == null)
            throw new IllegalArgumentException(format("Illegal call of withVariables(key = '%s', value = '%s', ...) - key must not be null.", key, value));
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(key, value);
        if (furtherKeyValuePairs != null) {
            if (furtherKeyValuePairs.length % 2 != 0) {
                throw new IllegalArgumentException(format("Illegal call of withVariables() - must have an even number of arguments, but found length = %s.", furtherKeyValuePairs.length + 2));
            }
            for (int i = 0; i < furtherKeyValuePairs.length; i += 2) {
                if (!(furtherKeyValuePairs[i] instanceof String))
                    throw new IllegalArgumentException(format("Illegal call of withVariables() - keys must be strings, found object of type '%s'.", furtherKeyValuePairs[i] != null ? furtherKeyValuePairs[i].getClass().getName() : null));
                map.put((String) furtherKeyValuePairs[i], furtherKeyValuePairs[i + 1]);
            }
        }
        return map;
    }

    /**
     * Helper method to easily access the only task currently available in the context of the last asserted process instance.
     *
     * @return the only task of the last asserted process instance. May return null if no such task exists.
     * @throws IllegalStateException in case more than one task is delivered by the underlying query or in case no process
     *                               instance was asserted yet.
     */
    public static Task task() {
        return task(taskQuery());
    }

    /**
     * Helper method to easily access the only task currently available in the context of the given process instance.
     *
     * @param processInstance the process instance for which a task should be retrieved.
     * @return the only task of the process instance. May return null if no such task exists.
     * @throws IllegalStateException in case more than one task is delivered by the underlying query.
     */
    public static Task task(ProcessInstance processInstance) {
        return task(taskQuery(), processInstance);
    }

    /**
     * Helper method to easily access the only task with the given taskDefinitionKey currently available in the context
     * of the last asserted process instance.
     *
     * @param taskDefinitionKey the key of the task that should be retrieved.
     * @return the only task of the last asserted process instance. May return null if no such task exists.
     * @throws IllegalStateException in case more than one task is delivered by the underlying query or in case no process
     *                               instance was asserted yet.
     */
    public static Task task(String taskDefinitionKey) {
        Assertions.assertThat(taskDefinitionKey).isNotNull();
        return task(taskQuery().taskDefinitionKey(taskDefinitionKey));
    }

    /**
     * Helper method to easily access the only task with the given taskDefinitionKey currently available in the context
     * of the given process instance.
     *
     * @param taskDefinitionKey the key of the task that should be retrieved.
     * @param processInstance   the process instance for which a task should be retrieved.
     * @return the only task of the given process instance. May teturn null if no such task exists.
     * @throws IllegalStateException in case more than one task is delivered by the underlying query.
     */
    public static Task task(String taskDefinitionKey, ProcessInstance processInstance) {
        Assertions.assertThat(taskDefinitionKey).isNotNull();
        return task(taskQuery().taskDefinitionKey(taskDefinitionKey), processInstance);
    }

    /**
     * Helper method to easily access the only task compliant to a given taskQuery and currently available in the context
     * of the last asserted process instance.
     *
     * @param taskQuery the query with which the task should be retrieved. This query will be further narrowed to the
     *                  last asserted process instance.
     * @return the only task of the last asserted process instance and compliant to the given query. May return null
     *         in case no such task exists.
     * @throws IllegalStateException in case more than one task is delivered by the underlying query or in case no process
     *                               instance was asserted yet.
     */
    public static Task task(TaskQuery taskQuery) {
        ProcessInstanceAssert lastAssert = AbstractProcessAssert.getLastAssert(ProcessInstanceAssert.class);
        if (lastAssert == null)
            throw new IllegalStateException(
                    "Call a process instance assertion first - e.g. assertThat(processInstance)..."
            );
        return task(taskQuery, lastAssert.getActual());
    }

    /**
     * Helper method to easily access the only task compliant to a given taskQuery and currently available in the context
     * of the given process instance.
     *
     * @param taskQuery       the query with which the task should be retrieved. This query will be further narrowed
     *                        to the given process instance.
     * @param processInstance the process instance for which a task should be retrieved.
     * @return the only task of the given process instance and compliant to the given query. May return null in case no
     *         such task exists.
     * @throws IllegalStateException in case more than one task is delivered by the underlying query.
     */
    public static Task task(TaskQuery taskQuery, ProcessInstance processInstance) {
        return assertThat(processInstance).isNotNull().task(taskQuery).getActual();
    }

    /**
     * Helper method to easily access the process definition on which the last asserted process instance is based.
     *
     * @return the process definition on which the last asserted process instance is based.
     * @throws IllegalStateException in case no process instance was asserted yet.
     */
    public static ProcessDefinition processDefinition() {
        ProcessInstanceAssert lastAssert = AbstractProcessAssert.getLastAssert(ProcessInstanceAssert.class);
        if (lastAssert == null)
            throw new IllegalStateException(
                    "Call a process instance assertion first - e.g. assertThat(processInstance)..."
            );
        return processDefinition(lastAssert.getActual());
    }

    /**
     * Helper method to easily access the process definition on which the given process instance is based.
     *
     * @param processInstance the process instance for which the definition should be retrieved.
     * @return the process definition on which the given process instance is based.
     */
    public static ProcessDefinition processDefinition(ProcessInstance processInstance) {
        assertThat(processInstance).isNotNull();
        return processDefinition(processDefinitionQuery().processDefinitionId(processInstance.getProcessDefinitionId()));
    }

    /**
     * Helper method to easily access the process definition with the given processDefinitionKey.
     *
     * @param processDefinitionKey the key of the process definition that should be retrieved.
     * @return the process definition with the given key. May return null if no such process definition exists.
     */
    public static ProcessDefinition processDefinition(String processDefinitionKey) {
        Assertions.assertThat(processDefinitionKey).isNotNull();
        return processDefinition(processDefinitionQuery().processDefinitionKey(processDefinitionKey));
    }

    /**
     * Helper method to easily access the process definition compliant to a given process definition query.
     *
     * @param processDefinitionQuery the query with which the process definition should be retrieved.
     * @return the process definition compliant to the given query. May return null in case no such process definition exists.
     * @throws org.flowable.engine.ProcessEngineException in case more than one process definition is delivered by the
     *                                                    underlying query.
     */
    public static ProcessDefinition processDefinition(ProcessDefinitionQuery processDefinitionQuery) {
        return processDefinitionQuery.singleResult();
    }

    /**
     * Helper method to easily access the only called process instance currently available in the context of the last
     * asserted process instance.
     *
     * @return the only called process instance called by the last asserted process instance. May return null if no such
     *         process instance exists.
     * @throws IllegalStateException in case more than one process instance is delivered by the underlying query or in
     *                               case no process instance was asserted yet.
     */
    public static ProcessInstance calledProcessInstance() {
        return calledProcessInstance(processInstanceQuery());
    }

    /**
     * Helper method to easily access the only called process instance currently available in the context of the given
     * process instance.
     *
     * @param processInstance the process instance for which a called process instance should be retrieved.
     * @return the only called process instance called by the given process instance. May return null if no such process
     *         instance exists.
     * @throws IllegalStateException in case more than one process instance is delivered by the underlying query.
     */
    public static ProcessInstance calledProcessInstance(ProcessInstance processInstance) {
        return calledProcessInstance(processInstanceQuery(), processInstance);
    }

    /**
     * Helper method to easily access the only called process instance with
     * the given processDefinitionKey currently available in the context
     * of the last asserted process instance.
     *
     * @param processDefinitionKey the key of the process instance that should
     *                             be retrieved.
     * @return the only such process instance called by the last asserted process
     * instance. May return null if no such process instance exists.
     * @throws IllegalStateException in case more
     *                               than one process instance is delivered by the underlying
     *                               query or in case no process instance was asserted
     *                               yet.
     */
    public static ProcessInstance calledProcessInstance(String processDefinitionKey) {
        Assertions.assertThat(processDefinitionKey).isNotNull();
        return calledProcessInstance(processInstanceQuery().processDefinitionKey(processDefinitionKey));
    }

    /**
     * Helper method to easily access the only called process instance with the
     * given processDefinitionKey currently available in the context
     * of the given process instance.
     *
     * @param processDefinitionKey the key of the process instance that should
     *                             be retrieved.
     * @param processInstance      the process instance for which
     *                             a called process instance should be retrieved.
     * @return the only such process instance called by the given process instance.
     * May return null if no such process instance exists.
     * @throws IllegalStateException in case more
     *                               than one process instance is delivered by the underlying
     *                               query.
     */
    public static ProcessInstance calledProcessInstance(String processDefinitionKey, ProcessInstance processInstance) {
        Assertions.assertThat(processDefinitionKey).isNotNull();
        return calledProcessInstance(processInstanceQuery().processDefinitionKey(processDefinitionKey), processInstance);
    }

    /**
     * Helper method to easily access the only called process instance compliant to
     * a given processInstanceQuery and currently available in the context
     * of the last asserted process instance.
     *
     * @param processInstanceQuery the query with which the called process instance should
     *                             be retrieved. This query will be further narrowed to the last asserted
     *                             process instance.
     * @return the only such process instance called by the last asserted process instance and
     * compliant to the given query. May return null in case no such task exists.
     * @throws IllegalStateException in case more
     *                               than one process instance is delivered by the underlying query or in case no
     *                               process instance was asserted yet.
     */
    public static ProcessInstance calledProcessInstance(ProcessInstanceQuery processInstanceQuery) {
        ProcessInstanceAssert lastAssert = AbstractProcessAssert.getLastAssert(ProcessInstanceAssert.class);
        if (lastAssert == null)
            throw new IllegalStateException(
                    "Call a process instance assertion first - e.g. assertThat(processInstance)..."
            );
        return calledProcessInstance(processInstanceQuery, lastAssert.getActual());
    }

    /**
     * Helper method to easily access the only called process instance compliant to
     * a given processInstanceQuery and currently available in the context of the given
     * process instance.
     *
     * @param processInstanceQuery the query with which the process instance should
     *                             be retrieved. This query will be further narrowed to the given process
     *                             instance.
     * @param processInstance      the process instance for which
     *                             a called process instance should be retrieved.
     * @return the only such process instance called by the given process instance and
     * compliant to the given query. May return null in
     * case no such process instance exists.
     * @throws IllegalStateException in case more
     *                               than one instance is delivered by the underlying
     *                               query.
     */
    public static ProcessInstance calledProcessInstance(ProcessInstanceQuery processInstanceQuery, ProcessInstance processInstance) {
        return assertThat(processInstance).isNotNull().calledProcessInstance(processInstanceQuery).getActual();
    }

//    /**
//     * Helper method to easily access the only job currently
//     * available in the context of the last asserted process
//     * instance.
//     *
//     * @return the only job of the last asserted process
//     * instance. May return null if no such job exists.
//     * @throws IllegalStateException in case more
//     *                               than one job is delivered by the underlying
//     *                               query or in case no process instance was asserted
//     *                               yet.
//     */
//    public static Job job() {
//        return job(jobQuery());
//    }

//    /**
//     * Helper method to easily access the only job currently
//     * available in the context of the given process instance.
//     *
//     * @param processInstance the process instance for which
//     *                        a job should be retrieved.
//     * @return the only job of the process instance. May
//     * return null if no such task exists.
//     * @throws IllegalStateException in case more
//     *                               than one job is delivered by the underlying
//     *                               query.
//     */
//    public static Job job(ProcessInstance processInstance) {
//        return job(jobQuery(), processInstance);
//    }

//    /**
//     * Helper method to easily access the only job with the
//     * given activityId currently available in the context
//     * of the last asserted process instance.
//     *
//     * @param activityId the id of the job that should
//     *                   be retrieved.
//     * @return the only job of the last asserted process
//     * instance. May return null if no such job exists.
//     * @throws IllegalStateException in case more
//     *                               than one job is delivered by the underlying
//     *                               query or in case no process instance was asserted
//     *                               yet.
//     */
//    public static Job job(String activityId) {
//        ProcessInstanceAssert lastAssert = AbstractProcessAssert.getLastAssert(ProcessInstanceAssert.class);
//        if (lastAssert == null)
//            throw new IllegalStateException(
//                    "Call a process instance assertion first - e.g. assertThat(processInstance)..."
//            );
//        return job(activityId, lastAssert.getActual());
//    }

//    /**
//     * Helper method to easily access the only job with the
//     * given activityId currently available in the context
//     * of the given process instance.
//     *
//     * @param activityId      the activityId of the job that should
//     *                        be retrieved.
//     * @param processInstance the process instance for which
//     *                        a job should be retrieved.
//     * @return the only job of the given process instance. May
//     * return null if no such job exists.
//     * @throws IllegalStateException in case more
//     *                               than one job is delivered by the underlying
//     *                               query.
//     */
//    public static Job job(String activityId, ProcessInstance processInstance) {
//        return assertThat(processInstance).isNotNull().job(activityId).getActual();
//    }

//    /**
//     * Helper method to easily access the only job compliant to
//     * a given jobQuery and currently available in the context
//     * of the last asserted process instance.
//     *
//     * @param jobQuery the query with which the job should
//     *                 be retrieved. This query will be further narrowed
//     *                 to the last asserted process instance.
//     * @return the only job of the last asserted process instance
//     * and compliant to the given query. May return null
//     * in case no such task exists.
//     * @throws IllegalStateException in case more
//     *                               than one job is delivered by the underlying
//     *                               query or in case no process instance was asserted
//     *                               yet.
//     */
//    public static Job job(JobQuery jobQuery) {
//        ProcessInstanceAssert lastAssert = AbstractProcessAssert.getLastAssert(ProcessInstanceAssert.class);
//        if (lastAssert == null)
//            throw new IllegalStateException(
//                    "Call a process instance assertion first - e.g. assertThat(processInstance)..."
//            );
//        return job(jobQuery, lastAssert.getActual());
//    }

//    /**
//     * Helper method to easily access the only job compliant to
//     * a given jobQuery and currently available in the context
//     * of the given process instance.
//     *
//     * @param jobQuery        the query with which the job should
//     *                        be retrieved. This query will be further narrowed
//     *                        to the given process instance.
//     * @param processInstance the process instance for which
//     *                        a job should be retrieved.
//     * @return the only job of the given process instance and
//     * compliant to the given query. May return null in
//     * case no such job exists.
//     * @throws IllegalStateException in case more
//     *                               than one job is delivered by the underlying
//     *                               query.
//     */
//    public static Job job(JobQuery jobQuery, ProcessInstance processInstance) {
//        return assertThat(processInstance).isNotNull().job(jobQuery).getActual();
//    }

    /**
     * Helper method to easily claim a task for a specific assignee.
     *
     * @param task           Task to be claimed for an assignee
     * @param assigneeUserId userId of assignee for which
     *                       the task should be claimed
     * @return the assigned task - properly refreshed to its
     * assigned state.
     */
    public static Task claim(Task task, String assigneeUserId) {
        if (task == null || assigneeUserId == null)
            throw new IllegalArgumentException(format("Illegal call " +
                    "of claim(task = '%s', assigneeUserId = '%s') - both must " +
                    "not be null.", task, assigneeUserId));
        taskService().claim(task.getId(), assigneeUserId);
        return taskQuery().taskId(task.getId()).singleResult();
    }

    /**
     * Helper method to easily unclaim a task.
     *
     * @param task Task to be claimed for an assignee
     * @return the assigned task - properly refreshed to its
     * unassigned state.
     */
    public static Task unclaim(Task task) {
        if (task == null)
            throw new IllegalArgumentException(format("Illegal call " +
                    "of unclaim(task = '%s') - task must not be null.", task));
        taskService().claim(task.getId(), null);
        return taskQuery().taskId(task.getId()).singleResult();
    }

    /**
     * Helper method to easily complete a task and pass some process variables.
     *
     * @param task      Task to be completed
     * @param variables Process variables to be passed to the
     *                  process instance when completing the task. For
     *                  setting those variables, you can use
     *                  withVariables(String key, Object value, ...)
     */
    public static void complete(Task task, Map<String, Object> variables) {
        if (task == null || variables == null)
            throw new IllegalArgumentException(format("Illegal call of complete(task = '%s', variables = '%s') - both must not be null.", task, variables));
        taskService().complete(task.getId(), variables);
    }

    /**
     * Helper method to easily complete a task.
     *
     * @param task Task to be completed
     */
    public static void complete(Task task) {
        if (task == null)
            throw new IllegalArgumentException("Illegal call of complete(task = 'null') - must not be null.");
        taskService().complete(task.getId());
    }

    /**
     * Helper method to easily execute a job.
     *
     * @param job Job to be executed.
     */
    public static void execute(Job job) {
        if (job == null)
            throw new IllegalArgumentException(format("Illegal call of execute(job = '%s') - must not be null.", job));
        Job current = jobQuery().jobId(job.getId()).singleResult();
        if (current == null)
            throw new IllegalStateException(format("Illegal state when calling execute(job = '%s') - job does not exist anymore.", job));
        managementService().executeJob(job.getId());
    }

    /**
     * Maps any element (task, event, gateway) from the name to the ID.
     *
     * @param name
     * @return the ID of the element
     */
    public static String findId(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Illegal call of findId(name = 'null') - must not be null.");
        }
        Map<String, String> nameToIDMapping = new HashMap<>();

        // find deployed process models
        List<ProcessDefinition> processDefinitions = repositoryService()
                .createProcessDefinitionQuery()
                .orderByProcessDefinitionVersion().asc()
                .list();
        // parse process models
        for (ProcessDefinition processDefinition : processDefinitions) {
            BpmnModel bpmnModelInstance = repositoryService().getBpmnModel(processDefinition.getId());
            List<Process> processes = bpmnModelInstance.getProcesses();

            for (Process process : processes) {
                List<Activity> activities =  process.findFlowElementsOfType(Activity.class, true);
                for (Activity activity : activities) {
                    insertAndCheckForDuplicateNames(nameToIDMapping, activity.getName(), activity.getId());
                }
                List<Gateway> gateways = process.findFlowElementsOfType(Gateway.class, true);
                for (Gateway gateway : gateways) {
                    insertAndCheckForDuplicateNames(nameToIDMapping, gateway.getName(), gateway.getId());
                }
                List<Event> events = process.findFlowElementsOfType(Event.class, true);
                for (Event event : events) {
                    insertAndCheckForDuplicateNames(nameToIDMapping, event.getName(), event.getId());
                }
            }
        }
        // look for name and return ID
        Assertions.assertThat(nameToIDMapping.containsKey(name))
                .overridingErrorMessage("Element with name '%s' doesn't exist", name)
                .isTrue();
        Assertions.assertThat(nameToIDMapping.get(name))
                .overridingErrorMessage("Name '%s' is not unique", name)
                .isNotEqualTo(DUPLICATED_NAME);
        return nameToIDMapping.get(name);
    }

    private static void insertAndCheckForDuplicateNames(Map<String, String> nameToIDMapping, String name, String id) {
        if (nameToIDMapping.containsKey(name)) {
            if (nameToIDMapping.get(name).equals(id)) {
                // already inserted as diagram includes two pools
            } else {
                nameToIDMapping.put(name, DUPLICATED_NAME);
            }
        } else {
            nameToIDMapping.put(name, id);
        }
    }
}
