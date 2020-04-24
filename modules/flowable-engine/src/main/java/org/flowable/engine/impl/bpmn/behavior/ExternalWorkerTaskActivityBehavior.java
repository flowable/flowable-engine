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

package org.flowable.engine.impl.bpmn.behavior;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.jobexecutor.ExternalWorkerTaskCompleteJobHandler;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerTaskActivityBehavior extends TaskActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected Expression jobTopicExpression;
    protected Expression skipExpression;
    protected boolean exclusive;

    public ExternalWorkerTaskActivityBehavior(ExternalWorkerServiceTask externalWorkerServiceTask, Expression jobTopicExpression, Expression skipExpression) {
        this.jobTopicExpression = jobTopicExpression;
        this.skipExpression = skipExpression;
        this.exclusive = externalWorkerServiceTask.isExclusive();
    }

    @Override
    public void execute(DelegateExecution execution) {
        CommandContext commandContext = CommandContextUtil.getCommandContext();
        String skipExpressionText = null;
        if (skipExpression != null) {
            skipExpressionText = skipExpression.getExpressionText();
        }

        FlowElement currentFlowElement = execution.getCurrentFlowElement();
        String elementId = currentFlowElement.getId();
        boolean isSkipExpressionEnabled = SkipExpressionUtil.isSkipExpressionEnabled(skipExpressionText, elementId, execution, commandContext);
        if (!isSkipExpressionEnabled || !SkipExpressionUtil.shouldSkipFlowElement(skipExpressionText, elementId, execution, commandContext)) {
            JobService jobService = CommandContextUtil.getJobService(commandContext);

            ExternalWorkerJobEntity job = jobService.createExternalWorkerJob();
            job.setExecutionId(execution.getId());
            job.setProcessInstanceId(execution.getProcessInstanceId());
            job.setProcessDefinitionId(execution.getProcessDefinitionId());
            job.setElementId(elementId);
            job.setElementName(currentFlowElement.getName());
            job.setJobHandlerType(ExternalWorkerTaskCompleteJobHandler.TYPE);
            job.setExclusive(exclusive);

            List<ExtensionElement> jobCategoryElements = currentFlowElement.getExtensionElements().get("jobCategory");
            if (jobCategoryElements != null && jobCategoryElements.size() > 0) {
                ExtensionElement jobCategoryElement = jobCategoryElements.get(0);
                if (StringUtils.isNotEmpty(jobCategoryElement.getElementText())) {
                    Expression categoryExpression = CommandContextUtil.getProcessEngineConfiguration(commandContext)
                            .getExpressionManager()
                            .createExpression(jobCategoryElement.getElementText());
                    Object categoryValue = categoryExpression.getValue(execution);
                    if (categoryValue != null) {
                        job.setCategory(categoryValue.toString());
                    }
                }
            }

            job.setJobType(JobEntity.JOB_TYPE_EXTERNAL_WORKER);
            job.setRetries(1);

            // Inherit tenant id (if applicable)
            if (execution.getTenantId() != null) {
                job.setTenantId(execution.getTenantId());
            }

            Object topicValue = jobTopicExpression.getValue(execution);
            if (topicValue != null && !topicValue.toString().isEmpty()) {
                job.setJobHandlerConfiguration(topicValue.toString());
            } else {
                throw new FlowableException("Expression " + jobTopicExpression + " did not evaluate to a valid value (non empty String). Was: " + topicValue);
            }

            jobService.insertExternalWorkerJob(job);
        } else {
            leave(execution);
        }
    }

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        leave(execution);
    }

}
