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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchService;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.impl.delete.ComputeDeleteHistoricProcessInstanceIdsJobHandler;
import org.flowable.engine.impl.delete.ComputeDeleteHistoricProcessInstanceStatusJobHandler;
import org.flowable.engine.impl.delete.DeleteHistoricProcessInstanceIdsJobHandler;
import org.flowable.engine.impl.delete.DeleteHistoricProcessInstanceIdsStatusJobHandler;
import org.flowable.engine.impl.delete.DeleteProcessInstanceBatchConstants;
import org.flowable.engine.impl.jobexecutor.BpmnHistoryCleanupJobHandler;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class HistoricDataEngineDeleteTest extends ResourceFlowableTestCase {

    public HistoricDataEngineDeleteTest() {
        super("org/flowable/engine/test/bpmn/HistoricDataEngineDeleteTest.flowable.cfg.xml");
    }

    protected Collection<String> batchesToRemove = new HashSet<>();

    @AfterEach
    void tearDown() {
        batchesToRemove.forEach(managementService::deleteBatch);
    }

    @Test
    @Deployment(resources="org/flowable/engine/test/bpmn/oneTask.bpmn20.xml")
    public void testHistoryCleanupTimerJob() {
        try {
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

                Batch batch = managementService.createBatchBuilder()
                        .batchType(Batch.HISTORIC_PROCESS_DELETE_TYPE)
                        .status(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS)
                        .batchDocumentJson("Batch document")
                        .searchKey(processInstance.getId())
                        .create();

                if (i >= 10) {
                    batchesToRemove.add(batch.getId());
                }

                managementService.createBatchPartBuilder(batch)
                        .type("test")
                        .status(DeleteProcessInstanceBatchConstants.STATUS_WAITING)
                        .create();
            }

            Batch caseBatch = managementService.createBatchBuilder()
                    .batchType(Batch.HISTORIC_CASE_DELETE_TYPE)
                    .status(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS)
                    .batchDocumentJson("Batch document")
                    .create();
            batchesToRemove.add(caseBatch.getId());

            managementService.createBatchPartBuilder(caseBatch)
                    .type("test")
                    .status(DeleteProcessInstanceBatchConstants.STATUS_WAITING)
                    .create();

            managementService.executeCommand(commandContext -> {
                BatchService batchService = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                        .getBatchServiceConfiguration()
                        .getBatchService();

                Batch batch = managementService.createBatchQuery().batchId(caseBatch.getId()).singleResult();
                BatchPart batchPart = managementService.createBatchPartQuery().batchId(batch.getId()).singleResult();

                batchService.completeBatchPart(batchPart.getId(), DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, "Batch part result");
                batchService.completeBatch(batch.getId(), DeleteProcessInstanceBatchConstants.STATUS_COMPLETED);

                return null;
            });

            assertThat(managementService.createBatchQuery().count()).isEqualTo(21);
            assertThat(managementService.createBatchPartQuery().count()).isEqualTo(21);
            
            if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
                
                assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);
                
                for (int i = 0; i < 10; i++) {
                    String processInstanceId = processInstanceIds.get(i);
                    Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
                    taskService.setVariableLocal(task.getId(), "taskVar", "taskValue" + (i + 1));
                    taskService.complete(task.getId());

                    managementService.executeCommand(commandContext -> {
                        BatchService batchService = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                                .getBatchServiceConfiguration()
                                .getBatchService();

                        Batch batch = managementService.createBatchQuery().searchKey(processInstanceId).singleResult();
                        BatchPart batchPart = managementService.createBatchPartQuery().batchId(batch.getId()).singleResult();

                        batchService.completeBatchPart(batchPart.getId(), DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, "Batch part result");
                        batchService.completeBatch(batch.getId(), DeleteProcessInstanceBatchConstants.STATUS_COMPLETED);

                        return null;
                    });

                }
                
                assertThat(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);
                
                Job executableJob = managementService.moveTimerToExecutableJob(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
                managementService.executeJob(executableJob.getId());
                
                assertThat(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).count()).isEqualTo(1);

                assertThat(managementService.createBatchQuery().count()).isEqualTo(12);
                assertThat(managementService.createBatchPartQuery().count()).isEqualTo(12);

                assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(20);
                assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(80);
                assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(20);

                Batch batch = managementService.createBatchQuery().searchKey("Flowable BPMN History Cleanup").singleResult();
                assertThat(batch).isNotNull();
                batchesToRemove.add(batch.getId());
                assertThat(batch.getStatus()).isEqualTo(DeleteProcessInstanceBatchConstants.STATUS_IN_PROGRESS);
                assertThat(batch.getBatchType()).isEqualTo(Batch.HISTORIC_PROCESS_DELETE_TYPE);
                assertThat(batch.getBatchSearchKey()).isEqualTo("Flowable BPMN History Cleanup");

                assertThat(managementService.createBatchPartQuery().batchId(batch.getId()).list())
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
                assertThat(managementService.createBatchPartQuery().batchId(batch.getId()).list())
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
                
                assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(10);
                assertThat(historyService.createHistoricActivityInstanceQuery().count()).isEqualTo(30);
                assertThat(historyService.createHistoricTaskInstanceQuery().count()).isEqualTo(10);
                
                for (int i = 0; i < 20; i++) {
                    if (i < 10) {
                        assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).isEmpty();
                        assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isZero();
                        
                    } else {
                        assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceIds.get(i))).hasSize(1);
                        assertThat(historyService.createHistoricTaskLogEntryQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(1);
                        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(2);
                        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceIds.get(i)).count()).isEqualTo(2);
                    }
                }
                
                managementService.deleteTimerJob(managementService.createTimerJobQuery().handlerType(BpmnHistoryCleanupJobHandler.TYPE).singleResult().getId());
            }
        
        } finally {
            processEngineConfiguration.resetClock();
        }
    }
}