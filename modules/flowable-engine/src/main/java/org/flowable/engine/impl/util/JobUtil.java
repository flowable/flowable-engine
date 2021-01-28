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
package org.flowable.engine.impl.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * @author Filip Hrisafov
 */
public class JobUtil {

    public static JobEntity createJob(ExecutionEntity execution, String jobHandlerType, ProcessEngineConfigurationImpl processEngineConfiguration) {
        return createJob(execution, execution.getCurrentFlowElement(), jobHandlerType, processEngineConfiguration);
    }

    public static JobEntity createJob(ExecutionEntity execution, BaseElement baseElement, String jobHandlerType, ProcessEngineConfigurationImpl processEngineConfiguration) {
        JobService jobService = processEngineConfiguration.getJobServiceConfiguration().getJobService();
        JobEntity job = jobService.createJob();
        job.setExecutionId(execution.getId());
        job.setProcessInstanceId(execution.getProcessInstanceId());
        job.setProcessDefinitionId(execution.getProcessDefinitionId());
        job.setElementId(baseElement.getId());
        if (baseElement instanceof FlowElement) {
            job.setElementName(((FlowElement) baseElement).getName());
        }
        job.setJobHandlerType(jobHandlerType);

        List<ExtensionElement> jobCategoryElements = baseElement.getExtensionElements().get("jobCategory");
        if (jobCategoryElements != null && jobCategoryElements.size() > 0) {
            ExtensionElement jobCategoryElement = jobCategoryElements.get(0);
            if (StringUtils.isNotEmpty(jobCategoryElement.getElementText())) {
                Expression categoryExpression = processEngineConfiguration.getExpressionManager().createExpression(jobCategoryElement.getElementText());
                Object categoryValue = categoryExpression.getValue(execution);
                if (categoryValue != null) {
                    job.setCategory(categoryValue.toString());
                }
            }
        }

        // Inherit tenant id (if applicable)
        if (execution.getTenantId() != null) {
            job.setTenantId(execution.getTenantId());
        }

        return job;
    }

}
