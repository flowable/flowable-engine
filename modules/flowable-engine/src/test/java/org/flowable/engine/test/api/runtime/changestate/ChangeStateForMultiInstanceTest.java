/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.test.api.runtime.changestate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Condition;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Dennis Federico
 */
public class ChangeStateForMultiInstanceTest extends PluggableFlowableTestCase {

    private ChangeStateEventListener changeStateEventListener = new ChangeStateEventListener();

    @BeforeEach
    protected void setUp() {
        processEngine.getRuntimeService().addEventListener(changeStateEventListener);
    }

    @AfterEach
    protected void tearDown() {
        processEngine.getRuntimeService().removeEventListener(changeStateEventListener);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceSequential.bpmn20.xml")
    public void testSetCurrentActivityToSequentialMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("sequentialMultiInstance")
            .variable("nrOfLoops", 3)
            .start();

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("beforeMultiInstance");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("beforeMultiInstance");

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("beforeMultiInstance", "seqTasks")
            .changeState();

        //First in the loop
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //One MI root and one seq Execution
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("seqTasks", "seqTasks");

        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars).extracting("nrOfActiveInstances").containsOnly(1);
        assertThat(miRootVars).extracting("nrOfCompletedInstances").containsOnly(0);
        assertThat(miRootVars).extracting("nrOfLoops").containsOnly(3);

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("seqTasks");
        assertThat(taskService.getVariable(tasks.get(0).getId(), "loopCounter")).isEqualTo(0);

        taskService.complete(tasks.get(0).getId());

        //Second in the loop
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //One MI root and one seq Execution
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("seqTasks", "seqTasks");

        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars).extracting("nrOfActiveInstances").containsOnly(1);
        assertThat(miRootVars).extracting("nrOfCompletedInstances").containsOnly(1);
        assertThat(miRootVars).extracting("nrOfLoops").containsOnly(3);

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("seqTasks");
        assertThat(taskService.getVariable(tasks.get(0).getId(), "loopCounter")).isEqualTo(1);

        //Complete second and third
        taskService.complete(tasks.get(0).getId());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        taskService.complete(tasks.get(0).getId());

        //After the MI
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("nextTask");
        assertThat((ExecutionEntity) executions.get(0)).extracting(ExecutionEntity::isMultiInstanceRoot).isEqualTo(false);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("nextTask");
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isNull();

        //Complete the process
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceSequential.bpmn20.xml")
    public void testSetCurrentActivityOfSequentialMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("sequentialMultiInstance")
            .variable("nrOfLoops", 5)
            .start();

        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        List<Execution> seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, seqExecutions.size());
        List<Task> activeSeqTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(1, activeSeqTasks.size());

        //First in the loop
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(0, taskService.getVariable(task.getId(), "loopCounter"));
        taskService.complete(task.getId());

        //Second in the loop
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(1, taskService.getVariable(task.getId(), "loopCounter"));

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("seqTasks", "nextTask")
            .changeState();

        seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, seqExecutions.size());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());
        assertNull(taskService.getVariable(task.getId(), "loopCounter"));
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceSequential.bpmn20.xml")
    public void testSetCurrentParentExecutionOfSequentialMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("sequentialMultiInstance")
            .variable("nrOfLoops", 5)
            .start();

        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        List<Execution> seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, seqExecutions.size());
        List<Task> activeSeqTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(1, activeSeqTasks.size());

        //First in the loop
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(0, taskService.getVariable(task.getId(), "loopCounter"));
        taskService.complete(task.getId());

        //Second in the loop
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(1, taskService.getVariable(task.getId(), "loopCounter"));

        //move the parent execution - otherwise the parent multi instance execution remains, although active==false.
        String parentExecutionId = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult().getParentId();
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(parentExecutionId, "nextTask")
            .changeState();

        seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, seqExecutions.size());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());
        assertNull(taskService.getVariable(task.getId(), "loopCounter"));
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallel.bpmn20.xml")
    public void testSetCurrentActivityToParallelMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstance")
            .variable("nrOfLoops", 3)
            .start();

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("beforeMultiInstance");
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("beforeMultiInstance");

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("beforeMultiInstance", "parallelTasks")
            .changeState();

        //First in the loop
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        //One MI root and 3 parallel Executions
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("parallelTasks", "parallelTasks", "parallelTasks", "parallelTasks");

        Execution miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        Map<String, Object> miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars).extracting("nrOfActiveInstances").containsOnly(3);
        assertThat(miRootVars).extracting("nrOfCompletedInstances").containsOnly(0);
        assertThat(miRootVars).extracting("nrOfLoops").containsOnly(3);

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("parallelTasks", "parallelTasks", "parallelTasks");
        assertThat(tasks).extracting(task -> taskService.getVariable(task.getId(), "loopCounter")).isNotNull();
        //        assertThat(taskService.getVariable(tasks.get(0).getId(), "loopCounter")).isNotNull()

        //Complete one execution
        taskService.complete(tasks.get(0).getId());

        //Confirm new state
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("parallelTasks", "parallelTasks", "parallelTasks", "parallelTasks");

        miRoot = executions.stream().filter(e -> ((ExecutionEntity) e).isMultiInstanceRoot()).findFirst().get();
        miRootVars = runtimeService.getVariables(miRoot.getId());
        assertThat(miRootVars).extracting("nrOfActiveInstances").containsOnly(2);
        assertThat(miRootVars).extracting("nrOfCompletedInstances").containsOnly(1);
        assertThat(miRootVars).extracting("nrOfLoops").containsOnly(3);

        //Two executions are inactive, the completed before and the MI root
        assertThat(executions).haveExactly(2, new Condition<>((Execution execution) -> !((ExecutionEntity) execution).isActive(), "inactive"));

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertThat(tasks).extracting(Task::getTaskDefinitionKey).containsExactly("parallelTasks", "parallelTasks");
        assertThat(taskService.getVariable(tasks.get(0).getId(), "loopCounter")).isEqualTo(1);

        //Complete the rest of the Tasks
        tasks.forEach(this::completeTask);

        //After the MI
        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertThat(executions).extracting(Execution::getActivityId).containsExactly("nextTask");
        assertThat((ExecutionEntity) executions.get(0)).extracting(ExecutionEntity::isMultiInstanceRoot).isEqualTo(false);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().singleResult();
        assertThat(task).extracting(Task::getTaskDefinitionKey).isEqualTo("nextTask");
        assertThat(taskService.getVariable(task.getId(), "loopCounter")).isNull();

        //Complete the process
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallel.bpmn20.xml")
    public void testSetCurrentActivityOfParallelMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstance")
            .variable("nrOfLoops", 3)
            .start();

        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        List<Execution> parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        List<Task> activeParallelTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(3, activeParallelTasks.size());

        //Complete one of the tasks
        taskService.complete(activeParallelTasks.get(1).getId());
        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        activeParallelTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(2, activeParallelTasks.size());

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("parallelTasks", "nextTask")
            .changeState();

        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, parallelExecutions.size());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallel.bpmn20.xml")
    public void testSetCurrentParentExecutionOfParallelMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstance")
            .variable("nrOfLoops", 3)
            .start();

        completeTask(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult());

        List<Execution> parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        List<Task> activeParallelTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(3, activeParallelTasks.size());

        //Complete one of the tasks
        taskService.complete(activeParallelTasks.get(1).getId());
        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        activeParallelTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(2, activeParallelTasks.size());

        //Fetch the parent execution of the multi instance task execution
        String parentExecutionId = runtimeService.createExecutionQuery().executionId(activeParallelTasks.get(0).getExecutionId()).singleResult().getParentId();
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(parentExecutionId, "nextTask")
            .changeState();

        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, parallelExecutions.size());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentExecutionWithinMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Move one of the executions within the multiInstance subProcess
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(subTask1Executions.get(0).getId(), "subTask2")
            .changeState();

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(1, subTask2Executions.size());

        //Complete one of the parallel subProcesses "subTask2"
        Task task = taskService.createTaskQuery().executionId(subTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(5, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(2, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(0, subTask2Executions.size());

        //Move the other two executions, one by one
        ChangeActivityStateBuilder changeActivityStateBuilder = runtimeService.createChangeActivityStateBuilder();
        subTask1Executions.forEach(e -> changeActivityStateBuilder.moveExecutionToActivityId(e.getId(), "subTask2"));
        changeActivityStateBuilder.changeState();

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(5, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(2, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(0, subTask1Executions.size());
        subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(2, subTask2Executions.size());

        //Complete the rest of the SubProcesses
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(2, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentActivityWithinMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Move one of the executions within the multiInstance subProcess
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask1", "subTask2")
            .changeState();

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(0, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the parallel subProcesses "subTask2"
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        Task task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentExecutionWithinNestedMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Move one of the executions within of the nested multiInstance subProcesses
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(nestedSubTask1Executions.get(0).getId(), "nestedSubTask2")
            .moveExecutionToActivityId(nestedSubTask1Executions.get(3).getId(), "nestedSubTask2")
            .moveExecutionToActivityId(nestedSubTask1Executions.get(6).getId(), "nestedSubTask2")
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Complete one of the outer subProcesses
        Task task = taskService.createTaskQuery().executionId(nestedSubTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        //One less task execution and one less nested instance
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(2, nestedSubTask2Executions.size());

        //Move the rest of the nestedSubTask1 executions
        ChangeActivityStateBuilder changeActivityStateBuilder = runtimeService.createChangeActivityStateBuilder();
        nestedSubTask1Executions.forEach(e -> changeActivityStateBuilder.moveExecutionToActivityId(e.getId(), "nestedSubTask2"));
        changeActivityStateBuilder.changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(0, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(8, nestedSubTask2Executions.size());

        //Complete all the nestedSubTask2
        tasks = taskService.createTaskQuery().taskDefinitionKey("nestedSubTask2").list();
        assertEquals(8, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentActivityWithinNestedMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Complete one task for each nestedSubProcess
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(0).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(3).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(6).getId()).singleResult().getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Moving the nestedSubTask1 activity should move all its executions
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("nestedSubTask1", "nestedSubTask2")
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(0, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(9, nestedSubTask2Executions.size());

        //Complete all the nestedSubTask2
        tasks = taskService.createTaskQuery().taskDefinitionKey("nestedSubTask2").list();
        assertEquals(9, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        Task task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentMultiInstanceSubProcessParentExecutionWithinProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Complete one of the Tasks
        Task task = taskService.createTaskQuery().executionId(subTask1Executions.get(1).getId()).singleResult();
        taskService.complete(task.getId());

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(1, subTask2Executions.size());

        //Move the parallelSubProcess via the parentExecution Ids
        String ParallelSubProcessParentExecutionId = runtimeService.createExecutionQuery()
            .processInstanceId(processInstance.getId())
            .activityId("parallelSubProcess")
            .list()
            .stream()
            .findFirst()
            .map(Execution::getParentId)
            .get();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(ParallelSubProcessParentExecutionId, "lastTask")
            .changeState();

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentMultiInstanceSubProcessParentActivityWithinProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Complete one of the Tasks
        Task task = taskService.createTaskQuery().executionId(subTask1Executions.get(1).getId()).singleResult();
        taskService.complete(task.getId());

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(1, subTask2Executions.size());

        //Move the parallelSubProcess
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("parallelSubProcess", "lastTask")
            .changeState();

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentMultiInstanceNestedSubProcessParentExecutionWithinSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Complete some of the Nested tasks
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(0).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(3).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(6).getId()).singleResult().getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Complete one of the nested subProcesses
        Task task = taskService.createTaskQuery().executionId(nestedSubTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        //One less task execution and one less nested instance
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(2, nestedSubTask2Executions.size());

        //Move each nested multiInstance parent
        Stream<String> parallelNestedSubProcessesParentIds = runtimeService.createExecutionQuery()
            .processInstanceId(processInstance.getId())
            .activityId("parallelNestedSubProcess")
            .list()
            .stream()
            .map(Execution::getParentId)
            .distinct();

        parallelNestedSubProcessesParentIds.forEach(parentId -> {
            runtimeService.createChangeActivityStateBuilder()
                .moveExecutionToActivityId(parentId, "subTask2")
                .changeState();
        });

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentMultiInstanceNestedSubProcessParentActivityWithinSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Complete some of the Nested tasks
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(0).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(3).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(6).getId()).singleResult().getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Complete one of the nested subProcesses
        Task task = taskService.createTaskQuery().executionId(nestedSubTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        //One less task execution and one less nested instance
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(2, nestedSubTask2Executions.size());

        //Move the activity nested multiInstance parent
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("parallelNestedSubProcess", "subTask2")
            .changeState();

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelGatewayInsideMultiInstanceSubProcess.bpmn20.xml" })
    public void testSetCurrentActivitiesUsingParallelGatewayNestedInMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGatewayInsideMultiInstanceSubProcess");

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> preForkTaskExecutions = runtimeService.createExecutionQuery().activityId("preForkTask").list();
        assertEquals(3, preForkTaskExecutions.size());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        assertEquals("preForkTask", tasks.get(0).getTaskDefinitionKey());

        //Move a task before the fork within the multiInstance subProcess
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleActivityIdToActivityIds("preForkTask", Arrays.asList("forkTask1", "forkTask2"))
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(10, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> forkTask1Executions = runtimeService.createExecutionQuery().activityId("forkTask1").list();
        assertEquals(3, forkTask1Executions.size());
        List<Execution> forkTask2Executions = runtimeService.createExecutionQuery().activityId("forkTask2").list();
        assertEquals(3, forkTask2Executions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(6, tasks.size());
        Map<String, List<Task>> taskGroups = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(2, taskGroups.keySet().size());
        assertEquals(new HashSet<>(Arrays.asList("forkTask1", "forkTask2")), taskGroups.keySet());
        assertEquals(3, taskGroups.get("forkTask1").size());
        assertEquals(3, taskGroups.get("forkTask2").size());

        //Move the parallel gateway task forward
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(Arrays.asList("forkTask1", "forkTask2"), "parallelJoin")
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> postForkTaskExecutions = runtimeService.createExecutionQuery().activityId("postForkTask").list();
        assertEquals(3, postForkTaskExecutions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Complete one of the tasks
        taskService.complete(tasks.get(1).getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(5, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(2, parallelSubProcessCount);
        postForkTaskExecutions = runtimeService.createExecutionQuery().activityId("postForkTask").list();
        assertEquals(2, postForkTaskExecutions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Finish the rest since we cannot move out of a multiInstance subProcess
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/parallelGatewayInsideMultiInstanceSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionsUsingParallelGatewayNestedInMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGatewayInsideMultiInstanceSubProcess");

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> preForkTaskExecutions = runtimeService.createExecutionQuery().activityId("preForkTask").list();
        assertEquals(3, preForkTaskExecutions.size());
        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        assertEquals("preForkTask", tasks.get(0).getTaskDefinitionKey());

        //Move a task before the fork within the multiInstance subProcess
        preForkTaskExecutions.forEach(e -> runtimeService.createChangeActivityStateBuilder()
            .moveSingleExecutionToActivityIds(e.getId(), Arrays.asList("forkTask1", "forkTask2"))
            .changeState());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(10, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> forkTask1Executions = runtimeService.createExecutionQuery().activityId("forkTask1").list();
        assertEquals(3, forkTask1Executions.size());
        List<Execution> forkTask2Executions = runtimeService.createExecutionQuery().activityId("forkTask2").list();
        assertEquals(3, forkTask2Executions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(6, tasks.size());
        Map<String, List<Task>> taskGroups = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(2, taskGroups.keySet().size());
        assertEquals(new HashSet<>(Arrays.asList("forkTask1", "forkTask2")), taskGroups.keySet());
        assertEquals(3, taskGroups.get("forkTask1").size());
        assertEquals(3, taskGroups.get("forkTask2").size());

        //Move the parallel gateway task forward
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(
                Stream.concat(forkTask1Executions.stream().map(Execution::getId), forkTask2Executions.stream().map(Execution::getId)).collect(Collectors.toList()), "postForkTask")
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> postForkTaskExecutions = runtimeService.createExecutionQuery().activityId("postForkTask").list();
        assertEquals(3, postForkTaskExecutions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Complete one of the tasks
        taskService.complete(tasks.get(1).getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(5, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(2, parallelSubProcessCount);
        postForkTaskExecutions = runtimeService.createExecutionQuery().activityId("postForkTask").list();
        assertEquals(2, postForkTaskExecutions.size());
        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Finish the rest since we cannot move out of a multiInstance subProcess
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayNestedInsideMultiInstanceSubProcess.bpmn20.xml" })
    public void testCompleteSetCurrentActivitiesUsingInclusiveGatewayNestedInMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGatewayInsideMultiInstanceSubProcess");

        //1x MI subProc root, 3x parallel MI subProc, 9x Task executions (3 tasks per Gw path)
        List<Execution> childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(13, childExecutions.size());
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(3, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(3, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(3, classifiedExecutions.get("taskInclusive3").size());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));

        //Move all activities
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(Arrays.asList("taskInclusive1", "taskInclusive2", "taskInclusive3"), "inclusiveJoin")
            .changeState();

        //Still 3 subProcesses running, all of them past the gateway fork/join
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(7, childExecutions.size());
        classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(3, classifiedExecutions.get("postForkTask").size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Finish the remaining subProcesses tasks
        tasks.forEach(t -> taskService.complete(t.getId()));

        //Only one execution and task remaining
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, childExecutions.size());
        assertEquals("lastTask", childExecutions.get(0).getActivityId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());

        //Complete the process
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/inclusiveGatewayNestedInsideMultiInstanceSubProcess.bpmn20.xml" })
    public void testCompleteSetCurrentExecutionsUsingInclusiveGatewayNestedInMultiInstanceSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("inclusiveGatewayInsideMultiInstanceSubProcess");

        //1x MI subProc root, 3x parallel MI subProc, 9x Task executions (3 tasks per Gw path)
        List<Execution> childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(13, childExecutions.size());
        Map<String, List<Execution>> classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(3, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(3, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(3, classifiedExecutions.get("taskInclusive3").size());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        Map<String, List<Task>> classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));

        //Finish one activity in two MI subProcesses
        taskService.complete(classifiedTasks.get("taskInclusive1").get(1).getId());
        taskService.complete(classifiedTasks.get("taskInclusive2").get(2).getId());

        //1x MI subProc root, 3x parallel MI subProc, 7x Gw Task executions, 2x Gw join executions
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(13, childExecutions.size());
        classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(2, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(2, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(3, classifiedExecutions.get("taskInclusive3").size());
        assertNotNull(classifiedExecutions.get("inclusiveJoin"));
        assertEquals(2, classifiedExecutions.get("inclusiveJoin").size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(3, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(2, classifiedExecutions.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(2, classifiedExecutions.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(3, classifiedExecutions.get("taskInclusive3").size());

        //TEST 1 (move all)... change state of "all" executions in a gateway at once
        //Move the executions of the gateway that still contains 3 tasks in execution
        Stream<Execution> tempStream = Stream.concat(classifiedExecutions.get("taskInclusive1").stream(), classifiedExecutions.get("taskInclusive2").stream());
        Map<String, List<Execution>> taskExecutionsByParent = Stream.concat(tempStream, classifiedExecutions.get("taskInclusive3").stream())
            .collect(Collectors.groupingBy(Execution::getParentId));

        List<String> ids = taskExecutionsByParent.values().stream()
            .filter(l -> l.size() == 3)
            .findFirst().orElseGet(ArrayList::new)
            .stream().map(Execution::getId)
            .collect(Collectors.toList());

        //Move into the synchronizing gateway
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(ids, "inclusiveJoin")
            .changeState();

        //There'll be still 3 subProcesses running, 2 with "gateways" still in execution, one with task3 & task1 & join, one with task3, task2 and join
        // the 3rd subProcess should be past the gateway fork/join
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(11, childExecutions.size());
        classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("taskInclusive1"));
        assertEquals(1, classifiedExecutions.get("taskInclusive1").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(1, classifiedExecutions.get("taskInclusive2").size());
        assertNotNull(classifiedExecutions.get("taskInclusive3"));
        assertEquals(2, classifiedExecutions.get("taskInclusive3").size());
        assertNotNull(classifiedExecutions.get("inclusiveJoin"));
        assertEquals(2, classifiedExecutions.get("inclusiveJoin").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(1, classifiedExecutions.get("postForkTask").size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(4, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("taskInclusive1"));
        assertEquals(1, classifiedTasks.get("taskInclusive1").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(1, classifiedTasks.get("taskInclusive2").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive3"));
        assertEquals(2, classifiedTasks.get("taskInclusive3").size());
        assertTrue(classifiedTasks.containsKey("postForkTask"));
        assertEquals(1, classifiedTasks.get("postForkTask").size());

        //TEST 2 (complete last execution)... complete the last execution of a gateway were a task execution was already moved into the synchronizing join
        ids = classifiedExecutions.get("taskInclusive1").stream().map(Execution::getId).collect(Collectors.toList());
        //Move into the synchronizing gateway
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(ids, "inclusiveJoin")
            .changeState();

        //Complete remaining task3, the next inline test needs the task to be completed too
        for (Task t : tasks) {
            if (t.getTaskDefinitionKey().equals("taskInclusive3")) {
                taskService.complete(t.getId());
            }
        }

        //Still 3 subProcesses running, two of them past the gateway fork/join and the remaining one with a task2 pending
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(9, childExecutions.size());
        classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(2, classifiedExecutions.get("postForkTask").size());
        assertNotNull(classifiedExecutions.get("inclusiveJoin"));
        assertEquals(2, classifiedExecutions.get("inclusiveJoin").size());
        assertNotNull(classifiedExecutions.get("taskInclusive2"));
        assertEquals(1, classifiedExecutions.get("taskInclusive2").size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        classifiedTasks = tasks.stream().collect(Collectors.groupingBy(Task::getTaskDefinitionKey));
        assertEquals(2, classifiedTasks.size());
        assertTrue(classifiedTasks.containsKey("postForkTask"));
        assertEquals(2, classifiedTasks.get("postForkTask").size());
        assertTrue(classifiedTasks.containsKey("taskInclusive2"));
        assertEquals(1, classifiedTasks.get("taskInclusive2").size());

        //TEST 3 (move last execution)... move the remaining execution of a gateway with previously completed executions into the synchronizing join
        ids = classifiedExecutions.get("taskInclusive2").stream().map(Execution::getId).collect(Collectors.toList());
        //Move into the synchronizing gateway
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(ids, "inclusiveJoin")
            .changeState();

        //Still 3 subProcesses running, all of them past the gateway fork/join
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(7, childExecutions.size());
        classifiedExecutions = groupListContentBy(childExecutions, Execution::getActivityId);
        assertNotNull(classifiedExecutions.get("parallelSubProcess"));
        assertEquals(4, classifiedExecutions.get("parallelSubProcess").size());
        assertNotNull(classifiedExecutions.get("postForkTask"));
        assertEquals(3, classifiedExecutions.get("postForkTask").size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> assertEquals("postForkTask", t.getTaskDefinitionKey()));

        //Finish the remaining subProcesses tasks
        tasks.forEach(t -> taskService.complete(t.getId()));

        //Only one execution and task remaining
        childExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, childExecutions.size());
        assertEquals("lastTask", childExecutions.get(0).getActivityId());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());

        //Complete the process
        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

}
