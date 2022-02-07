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
package org.flowable.engine.test.bpmn;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.delete.ComputeDeleteHistoricProcessInstanceIdsJobHandler;
import org.flowable.engine.impl.delete.ComputeDeleteHistoricProcessInstanceStatusJobHandler;
import org.flowable.engine.impl.delete.DeleteHistoricProcessInstanceIdsJobHandler;
import org.flowable.engine.impl.delete.DeleteHistoricProcessInstanceIdsStatusJobHandler;
import org.flowable.engine.impl.delete.DeleteProcessInstanceBatchConstants;
import org.flowable.engine.impl.jobexecutor.BpmnHistoryCleanupJobHandler;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.history.SerializableVariable;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class HistoricDataDeleteTest extends PluggableFlowableTestCase {

    protected Collection<String> batchesToRemove = new HashSet<>();

    @AfterEach
    void tearDown() {
        batchesToRemove.forEach(managementService::deleteBatch);
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/StartToEndTest.testStartToEnd.bpmn20.xml")
    public void testDeleteSingleHistoricProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
            
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.delete();

            HistoricActivityInstanceQuery activityQuery = historyService.createHistoricActivityInstanceQuery();
            activityQuery.finishedBefore(cal.getTime());
            activityQuery.delete();
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
        }
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteSingleHistoricInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        runtimeService.setVariable(processInstance.getId(), "testVar", "testValue");
        runtimeService.setVariable(processInstance.getId(), "numVar", 43);
        runtimeService.setVariable(processInstance.getId(), "serializableVar", new SerializableVariable("test"));
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);

            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).isNotEmpty();
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            }
            
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.delete();

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
            
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.setVariableLocal(task.getId(), "taskVar", "taskValue");
            taskService.setVariableLocal(task.getId(), "taskSerializableVar", new SerializableVariable("test"));
            taskService.complete(task.getId());
            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }
                    
            query.delete();

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).isNotEmpty();
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            }
                    
            historyService.deleteTaskAndActivityDataOfRemovedHistoricProcessInstances();

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();

            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).isNotEmpty();
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isGreaterThan(0);
            }
            
            historyService.deleteRelatedDataOfRemovedHistoricProcessInstances();
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).isEmpty();
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isZero();
            }
        }
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteSingleHistoricInstanceWithSingleMethod() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        runtimeService.setVariable(processInstance.getId(), "testVar", "testValue");
        runtimeService.setVariable(processInstance.getId(), "numVar", 43);
        runtimeService.setVariable(processInstance.getId(), "serializableVar", new SerializableVariable("test"));
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
            
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.setVariableLocal(task.getId(), "taskVar", "taskValue");
            taskService.setVariableLocal(task.getId(), "taskSerializableVar", new SerializableVariable("testTask"));
            taskService.complete(task.getId());

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }
                    
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.deleteWithRelatedData();
            
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).isEmpty();
            assertThat(historyService.getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId())).isEmpty();
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
            assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).count()).isZero();
        }
    }
    
    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteHistoricInstances() {
        List<String> processInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
            processInstanceIds.add(processInstance.getId());
            runtimeService.setVariable(processInstance.getId(), "testVar", "testValue" + (i + 1));
            runtimeService.setVariable(processInstance.getId(), "numVar", (i + 1));
            runtimeService.setVariable(processInstance.getId(), "serializableVar", new SerializableVariable("test" + (i+1)));
        }
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            
            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);
            
            for (int i = 0; i < 10; i++) {
                Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(i)).singleResult();
                taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                taskService.setVariableLocal(task.getId(), "taskSerializableVar", new SerializableVariable("test" + (i + 1)));
                taskService.complete(task.getId());
            }

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }
                    
            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            query.deleteWithRelatedData();
            
            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(10);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(30);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);
            
            for (int i = 0; i < 20; i++) {
                if (i < 10) {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    }
                    
                } else {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                    }
                }
            }
        }
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteHistoricInstancesUsingBatch() {
        List<String> processInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
            processInstanceIds.add(processInstance.getId());
            runtimeService.setVariable(processInstance.getId(), "testVar", "testValue" + (i + 1));
            runtimeService.setVariable(processInstance.getId(), "numVar", (i + 1));
            runtimeService.setVariable(processInstance.getId(), "serializableVar", new SerializableVariable("test" + (i+1)));
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);

            for (int i = 0; i < 10; i++) {
                Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(i)).singleResult();
                taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                taskService.setVariableLocal(task.getId(), "taskSerializableVar", new SerializableVariable("test" + (i + 1)));
                taskService.complete(task.getId());
            }

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            String batchId = query.deleteInParallelUsingBatch(5, "Test Deletion");
            batchesToRemove.add(batchId);

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(80);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

            Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);

            assertThat(managementService.createBatchPartQuery().list())
                    .hasSize(2)
                    .allSatisfy(part -> {
                        assertThat(part.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_WAITING);
                        assertThat(part.getType()).isEqualTo(DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE);
                    });

            assertThat(managementService.createJobQuery().list())
                    .hasSize(2)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
            assertThat(managementService.createJobQuery().list()).isEmpty();
            Job timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNotNull();
            assertThat(timer.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricProcessInstanceStatusJobHandler.TYPE);
            assertThat(timer.getJobHandlerConfiguration()).isEqualTo(batchId);
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());
            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );
            assertThat(managementService.createJobQuery().list())
                    .hasSize(2)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
            assertThat(managementService.createJobQuery().list()).isEmpty();
            timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNotNull();
            assertThat(timer.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstanceIdsStatusJobHandler.TYPE);
            assertThat(timer.getJobHandlerConfiguration()).isEqualTo(batchId);
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());
            timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNull();

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(10);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(30);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

            for (int i = 0; i < 20; i++) {
                if (i < 10) {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    }

                } else {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                    }
                }
            }
        }
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteHistoricInstancesUsingBatchWithAuthenticatedUser() {
        Authentication.setAuthenticatedUserId("test-user");

        String batchId = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("dummy")
                .deleteInParallelUsingBatch(5, "Test Deletion");
        batchesToRemove.add(batchId);

        Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
        assertThat(batch).isNotNull();
        assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED);
        assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
        assertThat(batch.getBatchSearchKey()).isEqualTo("Test Deletion");
        assertThat(batch.getBatchSearchKey2()).isEqualTo("test-user");
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteHistoricInstancesUsingBatchAndDeleteFails() {
        List<String> processInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
            processInstanceIds.add(processInstance.getId());
            runtimeService.setVariable(processInstance.getId(), "testVar", "testValue" + (i + 1));
            runtimeService.setVariable(processInstance.getId(), "numVar", (i + 1));
            runtimeService.setVariable(processInstance.getId(), "serializableVar", new SerializableVariable("test" + (i+1)));
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);

            for (int i = 0; i < 10; i++) {
                Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(i)).singleResult();
                taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                taskService.setVariableLocal(task.getId(), "taskSerializableVar", new SerializableVariable("test" + (i + 1)));
                taskService.complete(task.getId());
            }

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            String batchId = historyService.createHistoricProcessInstanceQuery()
                    .deleteInParallelUsingBatch(5, "Test Deletion with fail");
            batchesToRemove.add(batchId);

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(80);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

            Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.BPMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 20,"
                            + "  batchSize: 5,"
                            + "  query: { }"
                            + "}");

            assertThat(managementService.createBatchPartQuery().list())
                    .hasSize(4)
                    .allSatisfy(part -> {
                        assertThat(part.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_WAITING);
                        assertThat(part.getType()).isEqualTo(DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE);
                    });

            assertThat(managementService.createJobQuery().list())
                    .hasSize(4)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
            assertThat(managementService.createJobQuery().list()).isEmpty();
            Job timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNotNull();
            assertThat(timer.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricProcessInstanceStatusJobHandler.TYPE);
            assertThat(timer.getJobHandlerConfiguration()).isEqualTo(batchId);
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());
            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );
            assertThat(managementService.createJobQuery().list())
                    .hasSize(4)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
            assertThat(managementService.createJobQuery().list()).isEmpty();
            timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNotNull();
            assertThat(timer.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstanceIdsStatusJobHandler.TYPE);
            assertThat(timer.getJobHandlerConfiguration()).isEqualTo(batchId);
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());
            timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNull();

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_FAILED);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.BPMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 20,"
                            + "  numberOfFailedInstances: 10,"
                            + "  batchSize: 5,"
                            + "  query: { }"
                            + "}");

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(10);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(30);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

            for (int i = 0; i < 20; i++) {
                if (i < 10) {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    }

                } else {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                    }
                }
            }
        }
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteHistoricInstancesWithOrAndVariableValueUsingBatch() {
        List<String> processInstanceIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
            processInstanceIds.add(processInstance.getId());
            runtimeService.setVariable(processInstance.getId(), "testVar", "testValue" + (i + 1));
            runtimeService.setVariable(processInstance.getId(), "numVar", (i + 1));
            runtimeService.setVariable(processInstance.getId(), "serializableVar", new SerializableVariable("test" + (i+1)));
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(10);

            for (int i = 0; i < 5; i++) {
                Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(i)).singleResult();
                taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                taskService.setVariableLocal(task.getId(), "taskSerializableVar", new SerializableVariable("test" + (i + 1)));
                taskService.complete(task.getId());
            }

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            String batchId = historyService.createHistoricProcessInstanceQuery()
                    .finished()
                    .or()
                        .variableValueEquals("numVar", 1)
                        .variableValueEquals("numVar", 2)
                        .variableValueEquals("numVar", 3)
                        .variableValueEquals("numVar", 6)
                        .variableValueEquals("numVar", 7)
                    .endOr()
                    .deleteInParallelUsingBatch(5, "Test Deletion");
            batchesToRemove.add(batchId);

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(10);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(40);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

            Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.BPMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 3,"
                            + "  batchSize: 5,"
                            + "  query: {"
                            + "    finished: true,"
                            + "    orQueryObjects: ["
                            + "      {"
                            + "        queryVariableValues: ["
                            + "          { name: 'numVar', operator: 'EQUALS', textValue: '1', longValue: 1, type: 'integer' },"
                            + "          { name: 'numVar', operator: 'EQUALS', textValue: '2', longValue: 2, type: 'integer' },"
                            + "          { name: 'numVar', operator: 'EQUALS', textValue: '3', longValue: 3, type: 'integer' },"
                            + "          { name: 'numVar', operator: 'EQUALS', textValue: '6', longValue: 6, type: 'integer' },"
                            + "          { name: 'numVar', operator: 'EQUALS', textValue: '7', longValue: 7, type: 'integer' }"
                            + "        ]"
                            + "      }"
                            + "    ]"
                            + "  }"
                            + "}");

            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
            Job timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNotNull();
            assertThat(timer.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricProcessInstanceStatusJobHandler.TYPE);
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());
            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
            timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNotNull();
            assertThat(timer.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstanceIdsStatusJobHandler.TYPE);
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(7);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(25);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(7);

            for (int i = 0; i < 5; i++) {
                if (i < 3) {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    }

                } else {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(2);
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(5);
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(5);
                    }
                }
            }
        }
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteHistoricInstancesUsingBatchWhenNothingMatches() {
        List<String> processInstanceIds = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
            processInstanceIds.add(processInstance.getId());
            runtimeService.setVariable(processInstance.getId(), "testVar", "testValue" + (i + 1));
            runtimeService.setVariable(processInstance.getId(), "numVar", (i + 1));
            runtimeService.setVariable(processInstance.getId(), "serializableVar", new SerializableVariable("test" + (i+1)));
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(6);

            for (int i = 0; i < 3; i++) {
                Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(i)).singleResult();
                taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                taskService.setVariableLocal(task.getId(), "taskSerializableVar", new SerializableVariable("test" + (i + 1)));
                taskService.complete(task.getId());
            }

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            String batchId = historyService.createHistoricProcessInstanceQuery()
                    .finished()
                    .processDefinitionKey("dummy")
                    .deleteInParallelUsingBatch(5, "Test Deletion");
            batchesToRemove.add(batchId);

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(6);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(24);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(6);

            Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED);
            assertThat(batch.getCompleteTime()).isNotNull();
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.BPMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 0,"
                            + "  batchSize: 5,"
                            + "  query: {"
                            + "    finished: true,"
                            + "    processDefinitionKey: 'dummy'"
                            + "  }"
                            + "}");

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            assertThat(managementService.createBatchPartQuery().batchId(batchId).list()).isEmpty();
            assertThat(managementService.createJobQuery().list()).isEmpty();
            assertThat(managementService.createTimerJobQuery().list()).isEmpty();

        }
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteHistoricInstancesUsingUnevenBatch() {
        List<String> processInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
            processInstanceIds.add(processInstance.getId());
            runtimeService.setVariable(processInstance.getId(), "testVar", "testValue" + (i + 1));
            runtimeService.setVariable(processInstance.getId(), "numVar", (i + 1));
            runtimeService.setVariable(processInstance.getId(), "serializableVar", new SerializableVariable("test" + (i+1)));
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);

            for (int i = 0; i < 10; i++) {
                Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(i)).singleResult();
                taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                taskService.setVariableLocal(task.getId(), "taskSerializableVar", new SerializableVariable("test" + (i + 1)));
                taskService.complete(task.getId());
            }

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            String batchId = query.deleteInParallelUsingBatch(7, "Test Deletion Uneven");
            batchesToRemove.add(batchId);

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(80);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

            Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNull();
            assertThat(batch.getBatchSearchKey()).isEqualTo("Test Deletion Uneven");
            assertThat(batch.getBatchSearchKey2()).isNull();
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.BPMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 10,"
                            + "  batchSize: 7,"
                            + "  query: {"
                            + "    finished: true,"
                            + "    finishedBefore: '${json-unit.any-string}'"
                            + "  }"
                            + "}");

            assertThat(managementService.createBatchPartQuery().list())
                    .hasSize(2)
                    .allSatisfy(part -> {
                        assertThat(part.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_WAITING);
                        assertThat(part.getType()).isEqualTo(DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE);
                    });

            assertThat(managementService.createJobQuery().list())
                    .hasSize(2)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
            assertThat(managementService.createJobQuery().list()).isEmpty();
            Job timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNotNull();
            assertThat(timer.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricProcessInstanceStatusJobHandler.TYPE);
            assertThat(timer.getJobHandlerConfiguration()).isEqualTo(batchId);
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());
            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );
            assertThat(managementService.createJobQuery().list())
                    .hasSize(2)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
            assertThat(managementService.createJobQuery().list()).isEmpty();
            timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNotNull();
            assertThat(timer.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstanceIdsStatusJobHandler.TYPE);
            assertThat(timer.getJobHandlerConfiguration()).isEqualTo(batchId);
            managementService.moveTimerToExecutableJob(timer.getId());
            managementService.executeJob(timer.getId());
            timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNull();

            batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNotNull();
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.BPMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 10,"
                            + "  batchSize: 7,"
                            + "  query: {"
                            + "    finished: true,"
                            + "    finishedBefore: '${json-unit.any-string}'"
                            + "  }"
                            + "}");

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(10);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(30);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

            for (int i = 0; i < 20; i++) {
                if (i < 10) {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    }

                } else {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                    }
                }
            }
        }
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteHistoricInstancesSequentiallyUsingBatch() {
        List<String> processInstanceIds = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
            processInstanceIds.add(processInstance.getId());
            runtimeService.setVariable(processInstance.getId(), "testVar", "testValue" + (i + 1));
            runtimeService.setVariable(processInstance.getId(), "numVar", (i + 1));
            runtimeService.setVariable(processInstance.getId(), "serializableVar", new SerializableVariable("test" + (i+1)));
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);

            for (int i = 0; i < 10; i++) {
                Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(i)).singleResult();
                taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                taskService.setVariableLocal(task.getId(), "taskSerializableVar", new SerializableVariable("test" + (i + 1)));
                taskService.complete(task.getId());
            }

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
            query.finishedBefore(cal.getTime());
            String batchId = query.deleteSequentiallyUsingBatch(5, "Test Deletion");
            batchesToRemove.add(batchId);

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(80);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

            Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
            assertThat(batch.getBatchSearchKey()).isEqualTo("Test Deletion");
            assertThat(batch.getBatchSearchKey2()).isNull();
            assertThat(batch.getCompleteTime()).isNull();
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.BPMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 10,"
                            + "  batchSize: 5,"
                            + "  sequential: true,"
                            + "  query: {"
                            + "    finished: true,"
                            + "    finishedBefore: '${json-unit.any-string}'"
                            + "  }"
                            + "}");

            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE)
                    );

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    });

            Job timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNull();

            Job batchJob = managementService.createJobQuery().singleResult();
            managementService.executeJob(batchJob.getId());

            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    });

            timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNull();

            batchJob = managementService.createJobQuery().singleResult();
            managementService.executeJob(batchJob.getId());

            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    });
            timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNull();

            batchJob = managementService.createJobQuery().singleResult();
            managementService.executeJob(batchJob.getId());

            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    });

            batchJob = managementService.createJobQuery().singleResult();
            managementService.executeJob(batchJob.getId());

            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricProcessInstanceStatusJobHandler.TYPE);
                    });

            batchJob = managementService.createJobQuery().singleResult();
            managementService.executeJob(batchJob.getId());

            timer = managementService.createTimerJobQuery().singleResult();
            assertThat(timer).isNotNull();
            assertThat(timer.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstanceIdsStatusJobHandler.TYPE);
            managementService.moveTimerToExecutableJob(timer.getId());

            batchJob = managementService.createJobQuery().singleResult();
            managementService.executeJob(batchJob.getId());
            assertThat(managementService.createJobQuery().list()).isEmpty();

            batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNotNull();

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(10);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(30);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);

            for (int i = 0; i < 20; i++) {
                if (i < 10) {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    }

                } else {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(1);
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                    }
                }
            }
        }
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testHistoryCleanupTimerJob() {
        try {
            processEngineConfiguration.setEnableHistoryCleaning(true);
            Clock clock = processEngineConfiguration.getClock();
            Calendar cal = clock.getCurrentCalendar();
            cal.add(Calendar.DAY_OF_YEAR, -400);
            clock.setCurrentCalendar(cal);
            
            List<String> processInstanceIds = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
                processInstanceIds.add(processInstance.getId());
                runtimeService.setVariable(processInstance.getId(), "testVar", "testValue" + (i + 1));
                runtimeService.setVariable(processInstance.getId(), "numVar", (i + 1));
                runtimeService.setVariable(processInstance.getId(), "serializableVar", new SerializableVariable("test" + (i + 1)));
            }
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                
                assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);
                
                for (int i = 0; i < 10; i++) {
                    Task task = taskService.createTaskQuery().processInstanceId(processInstanceIds.get(i)).singleResult();
                    taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                    taskService.setVariableLocal(task.getId(), "taskSerializableVar", new SerializableVariable("test" + (i + 1)));
                    taskService.complete(task.getId());
                }

                if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                    waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
                }
                        
                managementService.handleHistoryCleanupTimerJob();
                
                assertThat(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);
                
                Job executableJob = managementService.moveTimerToExecutableJob(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
                managementService.executeJob(executableJob.getId());
                
                assertThat(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);

                assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);
                assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(80);
                assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

                Batch batch = managementService.createBatchQuery().singleResult();
                assertThat(batch).isNotNull();
                batchesToRemove.add(batch.getId());
                assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS);
                assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
                assertThat(batch.getBatchSearchKey()).isEqualTo("Flowable BPMN History Cleanup");
                assertThat(batch.getBatchSearchKey2()).isNull();
                assertThatJson(batch.getBatchDocumentJson(ScopeTypes.BPMN))
                        .isEqualTo("{"
                                + "  numberOfInstances: 10,"
                                + "  batchSize: 100,"
                                + "  query: {"
                                + "    finished: true,"
                                + "    finishedBefore: '${json-unit.any-string}'"
                                + "  }"
                                + "}");

                assertThat(managementService.createBatchPartQuery().list())
                        .hasSize(1)
                        .allSatisfy(part -> {
                            assertThat(part.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_WAITING);
                            assertThat(part.getType()).isEqualTo(DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE);
                        });

                assertThat(managementService.createJobQuery().list())
                        .hasSize(1)
                        .allSatisfy(job -> {
                            assertThat(job.getJobHandlerType()).isEqualTo(ComputeDeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                        });

                waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
                assertThat(managementService.createJobQuery().list()).isEmpty();
                Job timer = managementService.createTimerJobQuery().handlerType(ComputeDeleteHistoricProcessInstanceStatusJobHandler.TYPE).singleResult();
                assertThat(timer).isNotNull();
                managementService.moveTimerToExecutableJob(timer.getId());
                managementService.executeJob(timer.getId());
                assertThat(managementService.createBatchPartQuery().list())
                        .extracting(BatchPart::getStatus, BatchPart::getType)
                        .containsExactlyInAnyOrder(
                                tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE),
                                tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                        );
                assertThat(managementService.createJobQuery().list())
                        .hasSize(1)
                        .allSatisfy(job -> {
                            assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                        });

                waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
                assertThat(managementService.createJobQuery().list()).isEmpty();
                timer = managementService.createTimerJobQuery().handlerType(DeleteHistoricProcessInstanceIdsStatusJobHandler.TYPE).singleResult();
                assertThat(timer).isNotNull();
                managementService.moveTimerToExecutableJob(timer.getId());
                managementService.executeJob(timer.getId());

                if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                    waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
                }

                assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(10);
                assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(30);
                assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);
                
                for (int i = 0; i < 20; i++) {
                    if (i < 10) {
                        assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                        assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                            assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                        }
                        
                    } else {
                        assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                        assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(1);
                        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                            assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(3);
                        }
                    }
                }
                
                managementService.deleteTimerJob(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
            }
        
        } finally {
            processEngineConfiguration.setEnableHistoryCleaning(false);
            processEngineConfiguration.resetClock();
        }
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteSingleHistoricInstanceWithSingleMethodOnHistoryService() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        runtimeService.setVariable(processInstance.getId(), "testVar", "testValue");
        runtimeService.setVariable(processInstance.getId(), "numVar", 43);
        runtimeService.setVariable(processInstance.getId(), "serializableVar", new SerializableVariable("test"));
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(3);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);

            runtimeService.deleteProcessInstance(processInstance.getProcessInstanceId(), "for test");
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                // nothing
            }
            
            historyService.deleteHistoricProcessInstance(processInstance.getId());

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
            assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).isEmpty();
            assertThat(historyService.getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId())).isEmpty();
            assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
            assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).list()).isEmpty();
        }
    }

    @Test
    void testHistoryCleanupTimerJobCorrectlyUpdated() {
        String originalConfig = processEngineConfiguration.getHistoryCleaningTimeCycleConfig();
        String initialConfig = "0 0 1 * * ?";
        processEngineConfiguration.setHistoryCleaningTimeCycleConfig(initialConfig);

        try {
            assertThat(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count()).isZero();

            managementService.handleHistoryCleanupTimerJob();

            assertThat(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);
            Job job = managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).singleResult();
            TimerJobEntity timerJob = (TimerJobEntity) job;
            assertThat(timerJob.getRepeat()).isEqualTo(initialConfig);

            managementService.handleHistoryCleanupTimerJob();
            assertThat(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);
            job = managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).singleResult();
            assertThat(job.getId()).isEqualTo(timerJob.getId());

            processEngineConfiguration.setHistoryCleaningTimeCycleConfig("0 0 2 * * ?");

            managementService.handleHistoryCleanupTimerJob();
            assertThat(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);
            job = managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).singleResult();
            assertThat(job.getId()).isNotEqualTo(timerJob.getId());

            timerJob = (TimerJobEntity) job;
            assertThat(timerJob.getRepeat()).isEqualTo("0 0 2 * * ?");

        } finally {
            processEngineConfiguration.setHistoryCleaningTimeCycleConfig(originalConfig);
            managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).list()
                    .forEach(job -> managementService.deleteTimerJob(job.getId()));
        }
    }

}
