package org.flowable.cdi.test.impl.task;

import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

public class CdiTaskServiceTest extends CdiFlowableTestCase {

    @Test
    public void testClaimTask() {
        Task newTask = taskService.newTask();
        taskService.saveTask(newTask);
        taskService.claim(newTask.getId(), "kermit");
        taskService.deleteTask(newTask.getId(), true);
    }

}
