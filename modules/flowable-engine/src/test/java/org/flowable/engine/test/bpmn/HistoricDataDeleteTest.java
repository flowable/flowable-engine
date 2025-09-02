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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.delete.DeleteHistoricProcessInstancesSequentialJobHandler;
import org.flowable.engine.impl.delete.DeleteProcessInstanceBatchConstants;
import org.flowable.engine.impl.jobexecutor.BpmnHistoryCleanupJobHandler;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.history.SerializableVariable;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

import net.javacrumbs.jsonunit.core.Option;

public class HistoricDataDeleteTest extends PluggableFlowableTestCase {

    protected Collection<String> batchesToRemove = new HashSet<>();

    @AfterEach
    void tearDown() {
        batchesToRemove.forEach(managementService::deleteBatch);
        
        List<Job> jobs = managementService.createJobQuery().list();
        for (Job job : jobs) {
            try {
                managementService.deleteJob(job.getId());
            } catch(Exception e) {}
        }
        
        List<Job> timerJobs = managementService.createTimerJobQuery().list();
        for (Job job : timerJobs) {
            try {
                managementService.deleteTimerJob(job.getId());
            } catch(Exception e) {}
        }
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
            String batchId = query.deleteSequentiallyUsingBatch(5, "Test Deletion");
            batchesToRemove.add(batchId);

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(80);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

            Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);

            assertThat(managementService.createBatchPartQuery().list())
                    .hasSize(1)
                    .allSatisfy(part -> {
                        assertThat(part.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_WAITING);
                        assertThat(part.getType()).isEqualTo(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE);
                    });

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstancesSequentialJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
            assertThat(managementService.createJobQuery().list()).isEmpty();
            assertThat(managementService.createTimerJobQuery().list()).isEmpty();
            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

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
                .deleteSequentiallyUsingBatch(5, "Test Deletion");
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
                    .deleteSequentiallyUsingBatch(5, "Test Deletion with fail");
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
                            + "  query: { },"
                            + "  sequential: true"
                            + "}");

            assertThat(managementService.createBatchPartQuery().list())
                    .hasSize(1)
                    .allSatisfy(part -> {
                        assertThat(part.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_WAITING);
                        assertThat(part.getType()).isEqualTo(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE);
                    });

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstancesSequentialJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
            
            assertThat(managementService.createJobQuery().list()).isEmpty();
            assertThat(managementService.createTimerJobQuery().list()).isEmpty();
            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

            batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
            assertThatJson(batch.getBatchDocumentJson(ScopeTypes.BPMN))
                    .isEqualTo("{"
                            + "  numberOfInstances: 20,"
                            + "  batchSize: 5,"
                            + "  query: { },"
                            + "  sequential: true"
                            + "}");

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(0);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(0);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(0);

            for (int i = 0; i < 20; i++) {
                assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                    assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
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
                    .deleteSequentiallyUsingBatch(5, "Test Deletion");
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
                            + "  },"
                            + "  sequential: true"
                            + "}");

            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);

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
                    .deleteSequentiallyUsingBatch(5, "Test Deletion");
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
                            + "  },"
                            + "  sequential: true"
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
            String batchId = query.deleteSequentiallyUsingBatch(7, "Test Deletion Uneven");
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
                            + "  },"
                            + "  sequential: true"
                            + "}");

            assertThat(managementService.createBatchPartQuery().list())
                    .hasSize(1)
                    .allSatisfy(part -> {
                        assertThat(part.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_WAITING);
                        assertThat(part.getType()).isEqualTo(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE);
                    });

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstancesSequentialJobHandler.TYPE);
                    });

            waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
            assertThat(managementService.createJobQuery().list()).isEmpty();
            assertThat(managementService.createTimerJobQuery().list()).isEmpty();
            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );
            
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
                            + "  },"
                            + "  sequential: true"
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
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstancesSequentialJobHandler.TYPE);
                    });

            Job batchJob = managementService.createJobQuery().singleResult();
            managementService.executeJob(batchJob.getId());

            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstancesSequentialJobHandler.TYPE);
                    });

            batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNull();

            batchJob = managementService.createJobQuery().singleResult();
            managementService.executeJob(batchJob.getId());

            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstancesSequentialJobHandler.TYPE);
                    });

            batch = managementService.createBatchQuery().batchId(batchId).singleResult();
            assertThat(batch).isNotNull();
            assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS);
            assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
            assertThat(batch.getCompleteTime()).isNull();

            batchJob = managementService.createJobQuery().singleResult();
            managementService.executeJob(batchJob.getId());

            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

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

                processEngineConfiguration.resetClock();

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
                                + "  },"
                                + "  sequential: true"
                                + "}");

                assertThat(managementService.createBatchPartQuery().list())
                        .hasSize(1)
                        .allSatisfy(part -> {
                            assertThat(part.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_WAITING);
                            assertThat(part.getType()).isEqualTo(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE);
                        });

                assertThat(managementService.createJobQuery().list())
                        .hasSize(1)
                        .allSatisfy(job -> {
                            assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstancesSequentialJobHandler.TYPE);
                        });

                waitForJobExecutorToProcessAllJobsAndAllTimerJobs(10000, 200);
                
                assertThat(managementService.createBatchPartQuery().list())
                        .extracting(BatchPart::getStatus, BatchPart::getType)
                        .containsExactlyInAnyOrder(
                                tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                                tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                        );
                
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
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testBulkDeleteHistoricInstanceWith() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("startToEnd");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("startToEnd");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult();
        taskService.complete(task.getId());

        Set<String> instanceIds = new HashSet<>();
        instanceIds.add(processInstance.getId());
        instanceIds.add(processInstance2.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.INSTANCE, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(3);
        }

        historyService.bulkDeleteHistoricProcessInstances(instanceIds);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.INSTANCE, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(1);
        }

    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testInvalidHistoricProcessInstanceBulkDeleted() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startToEnd");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("startToEnd");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("startToEnd");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult();
        taskService.complete(task.getId());

        Set<String> instanceIds = new HashSet<>();
        instanceIds.add(processInstance.getId());
        instanceIds.add(processInstance2.getId());
        instanceIds.add("inValidId");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.INSTANCE, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(3);
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.INSTANCE, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(3);
        }

        assertThatThrownBy(() -> historyService.bulkDeleteHistoricProcessInstances(null))
                .isInstanceOf(FlowableIllegalArgumentException.class).hasMessage("processInstanceIds is null");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.INSTANCE, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(3);
        }

    }

    @Test
    void testDeleteHistoricInstancesWithAllQueryOptions() throws InvocationTargetException, IllegalAccessException {
        // This test is meant to validate that all query options are present when doing delete
        // If this test fails verify that the properties that are missing are added to DeleteHistoricProcessInstancesUsingBatchesCmd and BatchDeleteProcessConfig
        Map<String, String> methodNameToExpectedQueryPropertyName = new HashMap<>();
        methodNameToExpectedQueryPropertyName.put("deploymentIdIn", "deploymentIds");
        methodNameToExpectedQueryPropertyName.put("processDefinitionKeyNotIn", "processKeyNotIn");
        methodNameToExpectedQueryPropertyName.put("processInstanceBusinessKey", "businessKey");
        methodNameToExpectedQueryPropertyName.put("processInstanceBusinessKeyLike", "businessKeyLike");
        methodNameToExpectedQueryPropertyName.put("processInstanceBusinessKeyLikeIgnoreCase", "businessKeyLikeIgnoreCase");
        methodNameToExpectedQueryPropertyName.put("processInstanceBusinessStatus", "businessStatus");
        methodNameToExpectedQueryPropertyName.put("processInstanceBusinessStatusLike", "businessStatusLike");
        methodNameToExpectedQueryPropertyName.put("processInstanceBusinessStatusLikeIgnoreCase", "businessStatusLikeIgnoreCase");
        methodNameToExpectedQueryPropertyName.put("processInstanceCallbackType", "callbackType");
        methodNameToExpectedQueryPropertyName.put("processInstanceCallbackId", "callbackId");
        methodNameToExpectedQueryPropertyName.put("processInstanceCallbackIds", "callbackIds");
        methodNameToExpectedQueryPropertyName.put("withoutProcessInstanceCallbackId", "withoutCallbackId");
        methodNameToExpectedQueryPropertyName.put("processInstanceReferenceType", "referenceType");
        methodNameToExpectedQueryPropertyName.put("processInstanceReferenceId", "referenceId");
        methodNameToExpectedQueryPropertyName.put("processInstanceName", "name");
        methodNameToExpectedQueryPropertyName.put("processInstanceNameLike", "nameLike");
        methodNameToExpectedQueryPropertyName.put("processInstanceNameLikeIgnoreCase", "nameLikeIgnoreCase");
        methodNameToExpectedQueryPropertyName.put("processInstanceWithoutTenantId", "withoutTenantId");
        methodNameToExpectedQueryPropertyName.put("processInstanceTenantId", "tenantId");
        methodNameToExpectedQueryPropertyName.put("processInstanceTenantIdLike", "tenantIdLike");
        methodNameToExpectedQueryPropertyName.put("processInstanceTenantIdLikeIgnoreCase", "tenantIdLikeIgnoreCase");
        Set<String> methodsToIgnore = new HashSet<>();
        methodsToIgnore.add("parentCaseInstanceId");
        methodsToIgnore.add("limitProcessInstanceVariables");
        methodsToIgnore.add("includeProcessVariables");
        methodsToIgnore.add("locale");
        methodsToIgnore.add("withLocalizationFallback");
        methodsToIgnore.add("withoutSorting");
        methodsToIgnore.add("returnIdsOnly");
        methodsToIgnore.add("asc");
        methodsToIgnore.add("desc");
        methodsToIgnore.add("or");
        methodsToIgnore.add("endOr");
        methodsToIgnore.add("singleResult");
        Set<String> methodsWith2ParametersToIgnore = new HashSet<>();
        methodsWith2ParametersToIgnore.add("involvedUser");
        methodsWith2ParametersToIgnore.add("involvedGroup");
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
        Map<String, String> expectedParameters = new LinkedHashMap<>();

        Map<Method, Pair<String, Object>> methodsAndParametersForOr = new LinkedHashMap<>();

        for (Method method : HistoricProcessInstanceQuery.class.getMethods()) {
            String methodName = method.getName();
            if (methodsToIgnore.contains(methodName)
                    || methodName.startsWith("orderBy")
                    || methodName.startsWith("variable")
                    || methodName.startsWith("localVariable")
                    || (method.getParameterCount() == 2 && methodsWith2ParametersToIgnore.contains(methodName))
            ) {
                continue;
            }
            Class<?> returnType = method.getReturnType();
            if (!returnType.isInstance(query)) {
                // We only care about methods that return the query itself
                continue;
            }
            
            System.out.println(methodName);

            Instant baseTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
                    .minus(365, ChronoUnit.DAYS)
                    .with(ChronoField.MILLI_OF_SECOND, 563);

            Parameter[] parameters = method.getParameters();
            String propertyName = methodNameToExpectedQueryPropertyName.getOrDefault(methodName, methodName);
            if (parameters.length == 0) {
                expectedParameters.put(propertyName, "true");
                method.invoke(query);
                methodsAndParametersForOr.put(method, Pair.of("true", null));
            } else if (parameters.length == 1) {
                Parameter parameter = parameters[0];
                Class<?> parameterType = parameter.getType();
                Object parameterValue;
                Object parameterOrValue;
                String expectedValue;
                String expectedOrValue;
                if (parameterType.isAssignableFrom(String.class)) {
                    parameterValue = methodName + "Value";
                    expectedValue = "'" + parameterValue + "'";
                    parameterOrValue = methodName + "OrValue";
                    expectedOrValue = "'" + parameterOrValue + "'";
                    if (methodName.equals("processInstanceNameLikeIgnoreCase")) {
                        expectedValue = expectedValue.toLowerCase();
                        expectedOrValue = expectedOrValue.toLowerCase();
                    }
                } else if (parameterType.isAssignableFrom(Set.class)) {
                    String value1 = methodName + "SetValue1";
                    String value2 = methodName + "SetValue2";
                    parameterValue = new LinkedHashSet<>(Arrays.asList(value1, value2));
                    expectedValue = "["
                            + "  '" + value1 + "',"
                            + "  '" + value2 + "'"
                            + "]";

                    String value1Or = value1 + "Or";
                    String value2Or = value2 + "Or";
                    parameterOrValue = new LinkedHashSet<>(Arrays.asList(value1Or, value2Or));
                    expectedOrValue = "["
                            + "  '" + value1Or + "',"
                            + "  '" + value2Or + "'"
                            + "]";
                } else if (parameterType.isAssignableFrom(List.class)) {
                    String value1 = methodName + "ListValue1";
                    String value2 = methodName + "ListValue2";
                    parameterValue = Arrays.asList(value1, value2);
                    expectedValue = "["
                            + "  '" + value1 + "',"
                            + "  '" + value2 + "'"
                            + "]";

                    String value1Or = value1 + "Or";
                    String value2Or = value2 + "Or";
                    parameterOrValue = Arrays.asList(value1Or, value2Or);
                    expectedOrValue = "["
                            + "  '" + value1Or + "',"
                            + "  '" + value2Or + "'"
                            + "]";
                } else if (parameterType.isAssignableFrom(Integer.class)) {
                    parameterValue = methodName.hashCode();
                    expectedValue = parameterValue.toString();

                    parameterOrValue = methodName.hashCode() * 21;
                    expectedOrValue = parameterOrValue.toString();
                } else if (parameterType.isAssignableFrom(Date.class)) {
                    baseTime = baseTime.plus(10, ChronoUnit.DAYS);
                    parameterValue = Date.from(baseTime);
                    expectedValue = "'" + baseTime + "'";

                    Instant orTime = baseTime.plus(3, ChronoUnit.DAYS);
                    parameterOrValue = Date.from(orTime);
                    expectedOrValue = "'" + orTime + "'";
                } else if (parameterType.isAssignableFrom(boolean.class)) {
                    parameterValue = true;
                    expectedValue = parameterValue.toString();

                    parameterOrValue = true;
                    expectedOrValue = parameterOrValue.toString();
                } else {
                    throw new AssertionFailedError("No value could be resolved for method " + method);
                }

                expectedParameters.put(propertyName, expectedValue);
                method.invoke(query, parameterValue);
                methodsAndParametersForOr.put(method, Pair.of(expectedOrValue, parameterOrValue));
            } else {
                throw new AssertionFailedError("No value could be resolved for method " + method);
            }
        }

        query.or();
        for (Map.Entry<Method, Pair<String, Object>> entry : methodsAndParametersForOr.entrySet()) {
            Object argument = entry.getValue().getRight();
            if (argument == null) {
                entry.getKey().invoke(query);
            } else {
                entry.getKey().invoke(query, argument);
            }
        }
        query.endOr();

        String batchId = query.deleteSequentiallyUsingBatch(5, "Test Deletion");
        batchesToRemove.add(batchId);

        Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
        assertThat(batch).isNotNull();

        Function<Method, String> propertyNameProvider = m -> methodNameToExpectedQueryPropertyName.getOrDefault(m.getName(), m.getName());
        String expectedOrQueryValue = methodsAndParametersForOr.entrySet()
                .stream()
                .map(entry -> propertyNameProvider.apply(entry.getKey()) + ": " + entry.getValue().getLeft())
                .collect(Collectors.joining(","));

        expectedParameters.put("orQueryObjects", "[{" + expectedOrQueryValue + "}]");

        String expectedQueryValue = expectedParameters.entrySet()
                .stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(","));

        assertThatJson(batch.getBatchDocumentJson(ScopeTypes.BPMN))
                .isEqualTo("{"
                        + "  numberOfInstances: 0,"
                        + "  batchSize: 5,"
                        + "  sequential: true,"
                        + "  query: {" + expectedQueryValue + "}"
                        + "}");
    }

    @Test
    void testDeleteHistoricInstancesWithInvolvedOptions()  {
        String batchId = historyService.createHistoricProcessInstanceQuery()
                .involvedUser("kermit")
                .involvedUser("fozzie", IdentityLinkType.ASSIGNEE)
                .involvedGroups(new HashSet<>(Arrays.asList("sales", "hr")))
                .involvedGroup("admin", IdentityLinkType.CANDIDATE)
                .or()
                .involvedUser("kermitOr")
                .involvedUser("fozzieOr", IdentityLinkType.ASSIGNEE)
                .involvedGroups(new HashSet<>(Arrays.asList("salesOr", "hrOr")))
                .involvedGroup("adminOr", IdentityLinkType.CANDIDATE)
                .endOr()
                .deleteSequentiallyUsingBatch(10, "Test");
        batchesToRemove.add(batchId);

        Batch batch = managementService.createBatchQuery().batchId(batchId).singleResult();
        assertThat(batch).isNotNull();

        assertThatJson(batch.getBatchDocumentJson(ScopeTypes.BPMN))
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo("{"
                        + "  numberOfInstances: 0,"
                        + "  batchSize: 10,"
                        + "  sequential: true,"
                        + "  query: {"
                        + "    involvedUser: 'kermit',"
                        + "    involvedUserIdentityLink: {"
                        + "      userId: 'fozzie',"
                        + "      type: 'assignee'"
                        + "    },"
                        + "    involvedGroups: [ 'hr', 'sales' ],"
                        + "    involvedGroupIdentityLink: {"
                        + "      groupId: 'admin',"
                        + "      type: 'candidate'"
                        + "    },"
                        + "    orQueryObjects: ["
                        + "      {"
                        + "        involvedUser: 'kermitOr',"
                        + "        involvedUserIdentityLink: {"
                        + "          userId: 'fozzieOr',"
                        + "          type: 'assignee'"
                        + "        },"
                        + "        involvedGroups: [ 'hrOr', 'salesOr' ],"
                        + "        involvedGroupIdentityLink: {"
                        + "          groupId: 'adminOr',"
                        + "          type: 'candidate'"
                        + "        }"
                        + "      }"
                        + "    ]"
                        + "  }"
                        + "}");
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

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testDeleteHistoricInstancesUsingBatchWithStoppedBatch() {
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

            assertThat(managementService.createBatchPartQuery().list())
                    .hasSize(1)
                    .allSatisfy(part -> {
                        assertThat(part.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_WAITING);
                        assertThat(part.getType()).isEqualTo(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE);
                    });

            assertThat(managementService.createJobQuery().list())
                    .hasSize(1)
                    .allSatisfy(job -> {
                        assertThat(job.getJobHandlerType()).isEqualTo(DeleteHistoricProcessInstancesSequentialJobHandler.TYPE);
                    });

            Job job = managementService.createJobQuery().singleResult();
            managementService.executeJob(job.getId());

            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_WAITING, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

            managementService.executeCommand(commandContext -> {
                CommandContextUtil.getBatchService(commandContext)
                        .completeBatch(batchId, DeleteProcessInstanceBatchConstants.STATUS_STOPPED);
                return null;
            });

            job = managementService.createJobQuery().singleResult();
            managementService.executeJob(job.getId());

            assertThat(managementService.createJobQuery().list()).isEmpty();
            assertThat(managementService.createTimerJobQuery().list()).isEmpty();
            assertThat(managementService.createBatchPartQuery().list())
                    .extracting(BatchPart::getStatus, BatchPart::getType)
                    .containsExactlyInAnyOrder(
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE),
                            tuple(DeleteProcessInstanceBatchConstants.STATUS_STOPPED, DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                    );

            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                waitForHistoryJobExecutorToProcessAllJobs(7000, 300);
            }

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(15);
            assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(55);
            assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(15);

            for (int i = 0; i < 20; i++) {
                if (i < 5) {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                    }

                } else if (i < 10) {
                    assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                    assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(2);
                    assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(5);
                    if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(5);
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


}
