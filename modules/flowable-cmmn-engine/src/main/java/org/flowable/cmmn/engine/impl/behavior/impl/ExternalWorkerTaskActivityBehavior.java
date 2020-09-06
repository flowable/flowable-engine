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
package org.flowable.cmmn.engine.impl.behavior.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.job.ExternalWorkerTaskCompleteJobHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.interceptor.CreateCmmnExternalWorkerJobAfterContext;
import org.flowable.cmmn.engine.interceptor.CreateCmmnExternalWorkerJobBeforeContext;
import org.flowable.cmmn.engine.interceptor.CreateCmmnExternalWorkerJobInterceptor;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.ExternalWorkerServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobService;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerTaskActivityBehavior extends TaskActivityBehavior {

    protected ExternalWorkerServiceTask serviceTask;

    public ExternalWorkerTaskActivityBehavior(ExternalWorkerServiceTask serviceTask) {
        super(serviceTask.isBlocking(), serviceTask.getBlockingExpression());
        this.serviceTask = serviceTask;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        CreateCmmnExternalWorkerJobInterceptor interceptor = cmmnEngineConfiguration.getCreateCmmnExternalWorkerJobInterceptor();

        CreateCmmnExternalWorkerJobBeforeContext beforeContext = new CreateCmmnExternalWorkerJobBeforeContext(
                serviceTask,
                planItemInstanceEntity,
                getJobCategory(serviceTask),
                serviceTask.getTopic()
        );

        if (interceptor != null) {
            interceptor.beforeCreateExternalWorkerJob(beforeContext);
        }

        String jobTopicExpression = beforeContext.getJobTopicExpression();
        if (StringUtils.isEmpty(jobTopicExpression)) {
            throw new FlowableException("no topic expression configured");
        }

        JobServiceConfiguration jobServiceConfiguration = cmmnEngineConfiguration.getJobServiceConfiguration();
        JobService jobService = jobServiceConfiguration.getJobService();

        ExternalWorkerJobEntity job = jobService.createExternalWorkerJob();
        job.setSubScopeId(planItemInstanceEntity.getId());
        job.setScopeId(planItemInstanceEntity.getCaseInstanceId());
        job.setScopeDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
        job.setScopeType(ScopeTypes.CMMN);
        job.setElementId(serviceTask.getId());
        job.setElementName(serviceTask.getName());
        job.setJobHandlerType(ExternalWorkerTaskCompleteJobHandler.TYPE);
        job.setExclusive(serviceTask.isExclusive());

        if (StringUtils.isNotEmpty(beforeContext.getJobCategory())) {
                Expression categoryExpression = CommandContextUtil.getExpressionManager(commandContext)
                        .createExpression(beforeContext.getJobCategory());
                Object categoryValue = categoryExpression.getValue(planItemInstanceEntity);
                if (categoryValue != null) {
                    job.setCategory(categoryValue.toString());
                }
        }

        job.setJobType(JobEntity.JOB_TYPE_EXTERNAL_WORKER);
        job.setRetries(jobServiceConfiguration.getAsyncExecutorNumberOfRetries());

        // Inherit tenant id (if applicable)
        if (planItemInstanceEntity.getTenantId() != null) {
            job.setTenantId(planItemInstanceEntity.getTenantId());
        }

        Expression expression = CommandContextUtil.getExpressionManager(commandContext).createExpression(jobTopicExpression);
        Object expressionValue = expression.getValue(planItemInstanceEntity);
        if (expressionValue != null && !expressionValue.toString().isEmpty()) {
            job.setJobHandlerConfiguration(expressionValue.toString());
        } else {
            throw new FlowableException("Expression " + jobTopicExpression + " did not evaluate to a valid value (non empty String). Was: " + expressionValue);
        }

        jobService.insertExternalWorkerJob(job);

        if (interceptor != null) {
            interceptor.afterCreateExternalWorkerJob(new CreateCmmnExternalWorkerJobAfterContext(
                    serviceTask,
                    job,
                    planItemInstanceEntity
            ));
        }
    }

    protected String getJobCategory(BaseElement baseElement) {
        List<ExtensionElement> jobCategoryElements = baseElement.getExtensionElements().get("jobCategory");
        if (jobCategoryElements != null && jobCategoryElements.size() > 0) {
            return jobCategoryElements.get(0).getElementText();
        }

        return null;
    }

}
