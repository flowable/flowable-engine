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
package org.flowable.engine.impl.delete;

import java.util.List;

import org.flowable.batch.api.Batch;
import org.flowable.batch.api.BatchPart;
import org.flowable.batch.api.BatchPartQuery;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.JobService;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Filip Hrisafov
 */
public class ComputeDeleteHistoricProcessInstanceStatusJobHandler implements JobHandler {

    public static final String TYPE = "compute-delete-historic-process-status";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ProcessEngineConfigurationImpl engineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ManagementService managementService = engineConfiguration.getManagementService();
        Batch batch = managementService.createBatchQuery()
                .batchId(configuration)
                .singleResult();

        if (batch == null) {
            throw new FlowableIllegalArgumentException("There is no batch with the id " + configuration);
        }

        long totalBatchParts = createStatusQuery(batch, managementService).count();
        long totalCompleted = createStatusQuery(batch, managementService).completed().count();

        if (totalBatchParts == totalCompleted) {
            long totalFailed = createStatusQuery(batch, managementService)
                    .status(DeleteProcessInstanceBatchConstants.STATUS_FAILED)
                    .count();
            if (totalFailed == 0) {
                List<BatchPart> deleteBatchParts = managementService.createBatchPartQuery()
                        .batchId(batch.getId())
                        .type(DeleteProcessInstanceBatchConstants.BATCH_PART_DELETE_PROCESS_INSTANCES_TYPE)
                        .status(DeleteProcessInstanceBatchConstants.STATUS_WAITING)
                        .list();

                // Once the compute step has been done we can schedule the jobs for the actual deletion
                JobService jobService = engineConfiguration.getJobServiceConfiguration().getJobService();
                for (BatchPart deleteBatchPart : deleteBatchParts) {
                    JobEntity deleteJob = jobService.createJob();
                    deleteJob.setJobHandlerType(DeleteHistoricProcessInstanceIdsJobHandler.TYPE);
                    deleteJob.setJobHandlerConfiguration(deleteBatchPart.getId());
                    jobService.createAsyncJob(deleteJob, false);
                    jobService.scheduleAsyncJob(deleteJob);
                }

                TimerJobService timerJobService = engineConfiguration.getJobServiceConfiguration()
                        .getTimerJobService();
                TimerJobEntity timerJob = timerJobService.createTimerJob();
                timerJob.setJobType(Job.JOB_TYPE_TIMER);
                timerJob.setRevision(1);
                timerJob.setJobHandlerType(DeleteHistoricProcessInstanceIdsStatusJobHandler.TYPE);
                timerJob.setJobHandlerConfiguration(batch.getId());

                BusinessCalendar businessCalendar = engineConfiguration.getBusinessCalendarManager().getBusinessCalendar(CycleBusinessCalendar.NAME);
                timerJob.setDuedate(businessCalendar.resolveDuedate(engineConfiguration.getBatchStatusTimeCycleConfig()));
                timerJob.setRepeat(engineConfiguration.getBatchStatusTimeCycleConfig());

                timerJobService.scheduleTimerJob(timerJob);

            } else {
                completeBatch(batch, DeleteProcessInstanceBatchConstants.STATUS_FAILED, engineConfiguration);
            }

            job.setRepeat(null);
        } else if (totalBatchParts == 0) {
            completeBatch(batch, DeleteProcessInstanceBatchConstants.STATUS_COMPLETED, engineConfiguration);
            job.setRepeat(null);
        }

    }

    protected BatchPartQuery createStatusQuery(Batch batch, ManagementService managementService) {
        return managementService.createBatchPartQuery()
                .batchId(batch.getId())
                .type(DeleteProcessInstanceBatchConstants.BATCH_PART_COMPUTE_IDS_TYPE);
    }

    protected void completeBatch(Batch batch, String status, ProcessEngineConfigurationImpl engineConfiguration) {
        engineConfiguration.getBatchServiceConfiguration()
                .getBatchService()
                .completeBatch(batch.getId(), status);
    }
}
