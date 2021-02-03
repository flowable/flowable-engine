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
package org.flowable.engine.test.bpmn.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.flowable.engine.impl.jobexecutor.ParallelMultiInstanceActivityCompletionJobHandler;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class AsyncTaskTest extends PluggableFlowableTestCase {

    public static boolean INVOCATION;

    @Test
    @Deployment
    public void testAsyncServiceNoListeners() {
        INVOCATION = false;
        // start process
        runtimeService.startProcessInstanceByKey("asyncService");
        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        // the service was not invoked:
        assertThat(INVOCATION).isFalse();

        waitForJobExecutorToProcessAllJobs(7000L, 100L);

        // the service was invoked
        assertThat(INVOCATION).isTrue();
        // and the job is done
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testAsyncServiceListeners() {
        String pid = runtimeService.startProcessInstanceByKey("asyncService").getProcessInstanceId();
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        // the listener was not yet invoked:
        assertThat(runtimeService.getVariable(pid, "listener")).isNull();

        waitForJobExecutorToProcessAllJobs(7000L, 100L);

        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testAsyncServiceConcurrent() {
        INVOCATION = false;
        // start process
        runtimeService.startProcessInstanceByKey("asyncService");
        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        // the service was not invoked:
        assertThat(INVOCATION).isFalse();

        waitForJobExecutorToProcessAllJobs(7000L, 100L);

        // the service was invoked
        assertThat(INVOCATION).isTrue();
        // and the job is done
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testAsyncServiceMultiInstance() {
        INVOCATION = false;
        // start process
        runtimeService.startProcessInstanceByKey("asyncService");
        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        // the service was not invoked:
        assertThat(INVOCATION).isFalse();

        waitForJobExecutorToProcessAllJobs(7000L, 100L);

        // the service was invoked
        assertThat(INVOCATION).isTrue();
        // and the job is done
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testFailingAsyncServiceTimer() {
        // start process
        runtimeService.startProcessInstanceByKey("asyncService");
        // now there should be one job in the database, and it is a message
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        Job job = managementService.createJobQuery().singleResult();

        assertThatThrownBy(() -> managementService.executeJob(job.getId()))
                .isInstanceOf(FlowableException.class);

        // the service failed: the execution is still sitting in the service task:
        Execution execution = null;
        for (Execution e : runtimeService.createExecutionQuery().list()) {
            if (e.getParentId() != null) {
                execution = e;
            }
        }
        assertThat(execution).isNotNull();

        // there is still a single job because the timer was created in the same
        // transaction as the service was executed (which rolled back)
        assertThat(managementService.createTimerJobQuery().count()).isEqualTo(1);

        runtimeService.deleteProcessInstance(execution.getId(), "dead");
    }

    // TODO: Think about this:
    @Deployment
    public void FAILING_testFailingAsycServiceTimer() {
        // start process
        runtimeService.startProcessInstanceByKey("asyncService");
        // now there are two jobs the message and a timer:
        assertThat(managementService.createJobQuery().count()).isEqualTo(2);

        // let 'max-retires' on the message be reached
        waitForJobExecutorToProcessAllJobs(7000L, 100L);

        // the service failed: the execution is still sitting in the service task:
        Execution execution = runtimeService.createExecutionQuery().singleResult();
        assertThat(execution).isNotNull();
        assertThat(runtimeService.getActiveActivityIds(execution.getId()).get(0)).isEqualTo("service");

        // there are tow jobs, the message and the timer (the message will not
        // be retried anymore, max retires is reached.)
        assertThat(managementService.createJobQuery().count()).isEqualTo(2);

        // now the timer triggers:
        Context.getProcessEngineConfiguration().getClock().setCurrentTime(new Date(System.currentTimeMillis() + 10000));
        waitForJobExecutorToProcessAllJobs(7000L, 100L);

        // and we are done:
        assertThat(runtimeService.createExecutionQuery().singleResult()).isNull();
        // and there are no more jobs left:
        assertThat(managementService.createJobQuery().count()).isZero();

    }

    @Test
    @Deployment
    public void testAsyncServiceSubProcessTimer() {
        INVOCATION = false;
        // start process
        runtimeService.startProcessInstanceByKey("asyncService");
        // now there should be two jobs in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);

        // the service was not invoked:
        assertThat(INVOCATION).isFalse();

        waitForJobExecutorToProcessAllJobs(7000L, 200L);

        // the service was invoked
        assertThat(INVOCATION).isTrue();

        // both the timer and the message are cancelled
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testAsyncServiceSubProcess() {
        // start process
        runtimeService.startProcessInstanceByKey("asyncService");

        assertThat(managementService.createJobQuery().count()).isEqualTo(1);

        waitForJobExecutorToProcessAllJobs(7000L, 100L);

        // both the timer and the message are cancelled
        assertThat(managementService.createJobQuery().count()).isZero();

    }

    @Test
    @Deployment
    public void testAsyncTask() {
        // start process
        runtimeService.startProcessInstanceByKey("asyncTask");
        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        
        Job job = managementService.createJobQuery().singleResult();
        assertThat(job.getElementId()).isEqualTo("task");
        assertThat(job.getElementName()).isEqualTo("Test task");

        waitForJobExecutorToProcessAllJobs(7000L, 200L);

        // the job is done
        assertThat(managementService.createJobQuery().count()).isZero();
    }
    
    @Test
    @Deployment
    public void testAsyncTaskWithJobCategory() {
        // start process
        runtimeService.startProcessInstanceByKey("asyncTask");
        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        
        Job job = managementService.createJobQuery().singleResult();
        assertThat(job.getElementId()).isEqualTo("task");
        assertThat(job.getCategory()).isEqualTo("testCategory");

        waitForJobExecutorToProcessAllJobs(7000L, 200L);

        // the job is done
        assertThat(managementService.createJobQuery().count()).isZero();
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testAsyncTaskWithJobCategoryWithCategoryConfigurationSet() {
        try {
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("myCategory");
            // start process
            runtimeService.startProcessInstanceByKey("asyncTask");

            assertThatThrownBy(() -> waitForJobExecutorToProcessAllJobs(4000L, 200L))
                    .isInstanceOf(FlowableException.class);
            
            // job is not executed because of different job category value
            assertThat(managementService.createJobQuery().count()).isEqualTo(1);
            
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(Collections.singletonList("testCategory"));
    
            waitForJobExecutorToProcessAllJobs(4000L, 200L);
            
            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();
            
        } finally {
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testAsyncTaskWithJobCategoryWithCategoryConfigurationSetAndRunningAsyncExecutor() throws Exception {
        try {
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("myCategory");
            
            processEngineConfiguration.getAsyncExecutor().start();
            processEngineConfiguration.setAsyncExecutorActivate(true);
            
            // start process
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncTask");
            
            JobEntity job = (JobEntity) managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(job.getLockOwner()).isNull();
            assertThat(job.getLockExpirationTime()).isNull();
            
            Thread.sleep(4000);
            
            // job is not executed because of different job category value
            assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
            
            job = (JobEntity) managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(job.getLockOwner()).isNull();
            assertThat(job.getLockExpirationTime()).isNull();
            
            processEngineConfiguration.getAsyncExecutor().shutdown();
            
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(Collections.singletonList("testCategory"));
    
            processEngineConfiguration.getAsyncExecutor().start();
            JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 4000, 300, false);
            
            // the job is done
            assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isZero();
            
            assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            
        } finally {
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
            processEngineConfiguration.setAsyncExecutorActivate(false);
            processEngineConfiguration.getAsyncExecutor().shutdown();
        }
    }
    
    @Test
    @Deployment
    public void testAsyncTaskWithJobCategoryExpression() {
        // start process
        runtimeService.startProcessInstanceByKey("asyncTask", Collections.singletonMap("categoryValue", "myCategory"));
        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        
        Job job = managementService.createJobQuery().singleResult();
        assertThat(job.getCategory()).isEqualTo("myCategory");

        waitForJobExecutorToProcessAllJobs(5000L, 200L);

        // the job is done
        assertThat(managementService.createJobQuery().count()).isZero();
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategoryExpression.bpmn20.xml")
    public void testAsyncTaskWithJobCategoryExpressionWithCategoryConfigurationSet() {
        try {
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("myCategory");
            // start process
            runtimeService.startProcessInstanceByKey("asyncTask", Collections.singletonMap("categoryValue", "myCategory"));
    
            waitForJobExecutorToProcessAllJobs(4000L, 200L);
            
            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();
            
        } finally {
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategoryExpression.bpmn20.xml")
    public void testAsyncTaskWithJobCategoryWithMultipleCategoryConfigurationSet() {
        try {
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("myCategory");
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("anotherCategory");
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory("testCategory");
            
            // start process
            runtimeService.startProcessInstanceByKey("asyncTask", Collections.singletonMap("categoryValue", "testCategory"));
    
            waitForJobExecutorToProcessAllJobs(4000L, 200L);
            
            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();
            
        } finally {
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @Deployment
    public void testAsyncEndEvent() {
        // start process
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncEndEvent");
        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        
        Job job = managementService.createJobQuery().singleResult();
        assertThat(job.getElementId()).isEqualTo("theEnd");
        assertThat(job.getElementName()).isEqualTo("End event");

        Object value = runtimeService.getVariable(processInstance.getId(), "variableSetInExecutionListener");
        assertThat(value).isNull();

        waitForJobExecutorToProcessAllJobs(2000L, 200L);

        // the job is done
        assertThat(managementService.createJobQuery().count()).isZero();

        assertProcessEnded(processInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricVariableInstance> variables = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list();
            assertThat(variables).hasSize(3);

            assertThat(variables)
                    .extracting(HistoricVariableInstance::getVariableName, HistoricVariableInstance::getValue)
                    .contains(tuple("variableSetInExecutionListener", "firstValue"));
        }
    }

    @Test
    @Deployment
    public void testAsyncScript() {
        // start process
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncScript");
        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        
        Job job = managementService.createJobQuery().singleResult();
        assertThat(job.getElementId()).isEqualTo("script");
        assertThat(job.getElementName()).isEqualTo("Script");
        
        // the script was not invoked:
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        String eid = null;
        for (Execution e : executions) {
            if (e.getParentId() != null) {
                eid = e.getId();
            }
        }
        assertThat(runtimeService.getVariable(eid, "invoked")).isNull();

        waitForJobExecutorToProcessAllJobs(7000L, 100L);

        // and the job is done
        assertThat(managementService.createJobQuery().count()).isZero();

        // the script was invoked
        assertThat(runtimeService.getVariable(eid, "invoked")).isEqualTo("true");

        runtimeService.trigger(eid);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncCallActivity.bpmn20.xml",
            "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncServiceNoListeners.bpmn20.xml" })
    public void testAsyncCallActivity() throws Exception {
        // start process
        runtimeService.startProcessInstanceByKey("asyncCallactivity");
        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);

        waitForJobExecutorToProcessAllJobs(20000L, 250L);

        assertThat(managementService.createJobQuery().count()).isZero();
        assertThat(runtimeService.createProcessInstanceQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testBasicAsyncCallActivity.bpmn20.xml", "org/flowable/engine/test/bpmn/StartToEndTest.testStartToEnd.bpmn20.xml" })
    public void testBasicAsyncCallActivity() {
        runtimeService.startProcessInstanceByKey("myProcess");
        assertThat(managementService.createJobQuery().count()).as("There should be one job available.").isEqualTo(1);
        waitForJobExecutorToProcessAllJobs(7000L, 250L);
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testAsyncUserTask() {
        // start process
        String pid = runtimeService.startProcessInstanceByKey("asyncUserTask").getId();
        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        // the listener was not yet invoked:
        assertThat(runtimeService.getVariable(pid, "listener")).isNull();
        // the task listener was not yet invoked:
        assertThat(runtimeService.getVariable(pid, "taskListener")).isNull();
        // there is no usertask
        assertThat(taskService.createTaskQuery().singleResult()).isNull();

        waitForJobExecutorToProcessAllJobs(7000L, 250L);
        // the listener was now invoked:
        assertThat(runtimeService.getVariable(pid, "listener")).isNotNull();
        // the task listener was now invoked:
        assertThat(runtimeService.getVariable(pid, "taskListener")).isNotNull();

        // there is a usertask
        assertThat(taskService.createTaskQuery().singleResult()).isNotNull();
        // and no more job
        assertThat(managementService.createJobQuery().count()).isZero();

        String taskId = taskService.createTaskQuery().singleResult().getId();
        taskService.complete(taskId);

    }

    @Test
    @Deployment
    public void testMultiInstanceAsyncTask() {
        // start process
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncTask");

        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Job::getJobHandlerType )
                .containsExactlyInAnyOrder(AsyncContinuationJobHandler.TYPE, AsyncContinuationJobHandler.TYPE, AsyncContinuationJobHandler.TYPE);

        // execute first of 3 parallel multi instance tasks
        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstance.getId()).list().get(0).getId());
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Job::getJobHandlerType )
                .containsExactlyInAnyOrder(AsyncContinuationJobHandler.TYPE, AsyncContinuationJobHandler.TYPE, ParallelMultiInstanceActivityCompletionJobHandler.TYPE);

        // execute second of 3 parallel multi instance tasks
        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstance.getId()).handlerType(AsyncContinuationJobHandler.TYPE).list().get(0).getId());
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).list())
                .extracting(Job::getJobHandlerType )
                .containsExactlyInAnyOrder(AsyncContinuationJobHandler.TYPE, ParallelMultiInstanceActivityCompletionJobHandler.TYPE, ParallelMultiInstanceActivityCompletionJobHandler.TYPE);

        // execute third of 3 parallel multi instance tasks
        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstance.getId()).handlerType(AsyncContinuationJobHandler.TYPE).singleResult().getId());

        // the job is done
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();

            int startCount = 0;
            int taskCount = 0;
            int endCount = 0;
            int sequenceFlowCount = 0;
            for (HistoricActivityInstance historicActivityInstance : historicActivities) {
                if ("task".equals(historicActivityInstance.getActivityId())) {
                    taskCount++;

                } else if ("theStart".equals(historicActivityInstance.getActivityId())) {
                    startCount++;

                } else if ("theEnd".equals(historicActivityInstance.getActivityId())) {
                    endCount++;

                } else if (historicActivityInstance.getActivityId().contains("_flow_")) {
                    sequenceFlowCount++;

                } else {
                    Assert.fail("Unexpected activity found " + historicActivityInstance.getActivityId());
                }
            }

            assertThat(startCount).isEqualTo(1);
            assertThat(taskCount).isEqualTo(3);
            assertThat(sequenceFlowCount).isEqualTo(2);
            assertThat(endCount).isEqualTo(1);
        }
    }

    @Test
    @Deployment
    public void testMultiInstanceTask() {
        // start process
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncTask");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();

            int startCount = 0;
            int taskCount = 0;
            int endCount = 0;
            int sequenceFlowCount = 0;
            for (HistoricActivityInstance historicActivityInstance : historicActivities) {
                if ("task".equals(historicActivityInstance.getActivityId())) {
                    taskCount++;

                } else if ("theStart".equals(historicActivityInstance.getActivityId())) {
                    startCount++;

                } else if ("theEnd".equals(historicActivityInstance.getActivityId())) {
                    endCount++;

                } else if (historicActivityInstance.getActivityId().contains("_flow_")) {
                    sequenceFlowCount++;

                } else {
                    Assert.fail("Unexpected activity found " + historicActivityInstance.getActivityId());
                }
            }

            assertThat(startCount).isEqualTo(1);
            assertThat(taskCount).isEqualTo(3);
            assertThat(sequenceFlowCount).isEqualTo(2);
            assertThat(endCount).isEqualTo(1);
        }
    }

    @Test
    @Deployment
    public void testMultiInstanceAsyncSequentialTask() {
        // start process
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncTask");

        // now there should be one job in the database:
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        // execute first of 3 sequential multi instance tasks
        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult().getId());
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        // execute second of 3 sequential multi instance tasks
        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult().getId());
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

        // execute third of 3 sequential multi instance tasks
        managementService.executeJob(managementService.createJobQuery().processInstanceId(processInstance.getId()).singleResult().getId());

        // the job is done
        assertThat(managementService.createJobQuery().processInstanceId(processInstance.getId()).count()).isZero();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();

            int startCount = 0;
            int taskCount = 0;
            int endCount = 0;
            int sequenceFlowCount = 0;
            for (HistoricActivityInstance historicActivityInstance : historicActivities) {
                if ("task".equals(historicActivityInstance.getActivityId())) {
                    taskCount++;

                } else if ("theStart".equals(historicActivityInstance.getActivityId())) {
                    startCount++;

                } else if ("theEnd".equals(historicActivityInstance.getActivityId())) {
                    endCount++;

                } else if (historicActivityInstance.getActivityId().contains("_flow_")) {
                    sequenceFlowCount++;

                } else {
                    Assert.fail("Unexpected activity found " + historicActivityInstance.getActivityId());
                }
            }

            assertThat(startCount).isEqualTo(1);
            assertThat(taskCount).isEqualTo(3);
            assertThat(sequenceFlowCount).isEqualTo(2);
            assertThat(endCount).isEqualTo(1);
        }
    }

    @Test
    @Deployment
    public void testMultiInstanceSequentialTask() {
        // start process
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("asyncTask");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();

            int startCount = 0;
            int taskCount = 0;
            int endCount = 0;
            int sequenceFlowCount = 0;
            for (HistoricActivityInstance historicActivityInstance : historicActivities) {
                if ("task".equals(historicActivityInstance.getActivityId())) {
                    taskCount++;

                } else if ("theStart".equals(historicActivityInstance.getActivityId())) {
                    startCount++;

                } else if ("theEnd".equals(historicActivityInstance.getActivityId())) {
                    endCount++;

                } else if (historicActivityInstance.getActivityId().contains("_flow_")) {
                    sequenceFlowCount++;

                } else {
                    Assert.fail("Unexpected activity found " + historicActivityInstance.getActivityId());
                }
            }

            assertThat(startCount).isEqualTo(1);
            assertThat(taskCount).isEqualTo(3);
            assertThat(sequenceFlowCount).isEqualTo(2);
            assertThat(endCount).isEqualTo(1);
        }
    }

}
