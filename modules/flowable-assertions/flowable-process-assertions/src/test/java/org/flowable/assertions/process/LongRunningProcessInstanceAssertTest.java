package org.flowable.assertions.process;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.Test;

/**
 * @author martin.grofcik
 */
@FlowableTest
class LongRunningProcessInstanceAssertTest {

    @Test
    @Deployment(resources = "twoTasks.bpmn20.xml")
    void useOneAssertInstanceThroughAllTest(RuntimeService runtimeService, TaskService taskService) {
        ProcessInstance twoTasksProcess = runtimeService.createProcessInstanceBuilder().processDefinitionKey("twoTasksProcess").start();
        ProcessInstanceAssert asserThatProcessInstance = FlowableProcessAssertions.assertThat(
            twoTasksProcess
        );
        asserThatProcessInstance.isRunning()
                .activities().extracting(ActivityInstance::getActivityId).containsExactlyInAnyOrder(
                    "theStart", "theStart-theTask", "theTask1"
                );

        taskService.complete(geTaskIdForProcessInstance(twoTasksProcess.getId(), taskService));

        asserThatProcessInstance.isRunning().activities().extracting(ActivityInstance::getActivityId).containsExactlyInAnyOrder(
                "theStart", "theStart-theTask", "theTask1", "theTask1-theTask2", "theTask2"
        );

        taskService.complete(geTaskIdForProcessInstance(twoTasksProcess.getId(), taskService));

        asserThatProcessInstance.doesNotExist().inHistory()
                .activities().extracting(HistoricActivityInstance::getActivityId).containsExactlyInAnyOrder(
                    "theStart", "theStart-theTask", "theTask1", "theTask1-theTask2", "theTask2", "theTask-theEnd",
                        "theEnd"
                );
    }

    private String geTaskIdForProcessInstance(String processInstanceId, TaskService taskService) {
        return taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult().getId();
    }

}