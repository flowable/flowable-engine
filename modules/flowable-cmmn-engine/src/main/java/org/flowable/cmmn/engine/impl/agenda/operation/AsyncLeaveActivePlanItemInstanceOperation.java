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
package org.flowable.cmmn.engine.impl.agenda.operation;

import static org.flowable.cmmn.engine.impl.agenda.operation.OperationSerializationMetadata.OPERATION_TRANSITION;

import java.util.Map;

import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.job.AsyncLeaveActivePlanItemInstanceJobHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.JobUtil;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.Task;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.CmmnLoggingSessionConstants;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class AsyncLeaveActivePlanItemInstanceOperation extends AbstractChangePlanItemInstanceStateOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncLeaveActivePlanItemInstanceOperation.class);

    protected String transition;
    protected Map<String, String> transitionMetadata;

    public AsyncLeaveActivePlanItemInstanceOperation(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity,
            String transition, Map<String, String> transitionMetadata) {
        super(commandContext, planItemInstanceEntity);
        this.transition = transition;
        this.transitionMetadata = transitionMetadata;
    }

    @Override
    protected void internalExecute() {
        PlanItemDefinition planItemDefinition = planItemInstanceEntity.getPlanItem().getPlanItemDefinition();
        if (planItemDefinition instanceof Task) {
            createAsyncJob((Task) planItemDefinition);
        } else {
            throw new FlowableException("Programmatic error: this operation can only be planned for Task plan item definitions");
        }
    }

    protected void createAsyncJob(Task task) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        JobService jobService = cmmnEngineConfiguration.getJobServiceConfiguration().getJobService();
        JobEntity job = JobUtil.createJob(planItemInstanceEntity, task, AsyncLeaveActivePlanItemInstanceJobHandler.TYPE, cmmnEngineConfiguration);

        job.setJobHandlerConfiguration(createJobConfiguration());

        jobService.createAsyncJob(job, task.isExclusive());
        jobService.scheduleAsyncJob(job);

        if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
            CmmnLoggingSessionUtil.addAsyncActivityLoggingData("Created async job for " + planItemInstanceEntity.getPlanItemDefinitionId() + ", with job id " + job.getId(),
                    CmmnLoggingSessionConstants.TYPE_SERVICE_TASK_ASYNC_JOB, job, planItemInstanceEntity.getPlanItemDefinition(),
                    planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
        }
    }

    protected String createJobConfiguration() {
        ObjectMapper objectMapper = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put(OperationSerializationMetadata.OPERATION_TRANSITION, transition);

        for (String key: transitionMetadata.keySet()) {
            objectNode.put(key, transitionMetadata.get(key));
        }

        try {
            return objectMapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Programmatic error: could not create job configuration JSON", e);
        }
        return null;
    }

    @Override
    public String getNewState() {
        return PlanItemInstanceState.ASYNC_ACTIVE_LEAVE;
    }

    @Override
    public String getLifeCycleTransition() {
        return PlanItemTransition.ASYNC_LEAVE_ACTIVE;
    }

    @Override
    public String getOperationName() {
        return "[Async leave active plan item]";
    }
}
