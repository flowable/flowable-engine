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
package org.flowable.cmmn.engine.impl.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * @author Filip Hrisafov
 */
public class JobUtil {

    public static JobEntity createJob(CaseInstanceEntity caseInstance, BaseElement baseElement, String jobHandlerType,
            CmmnEngineConfiguration cmmnEngineConfiguration) {
        JobEntity job = createJob((VariableContainer) caseInstance, baseElement, jobHandlerType, cmmnEngineConfiguration);

        job.setScopeId(caseInstance.getId());
        job.setScopeDefinitionId(caseInstance.getCaseDefinitionId());

        return job;
    }

    public static JobEntity createJob(PlanItemInstanceEntity planItemInstance, BaseElement baseElement, String jobHandlerType, CmmnEngineConfiguration cmmnEngineConfiguration) {
        JobEntity job = createJob((VariableContainer) planItemInstance, baseElement, jobHandlerType, cmmnEngineConfiguration);
        job.setScopeId(planItemInstance.getCaseInstanceId());
        job.setSubScopeId(planItemInstance.getId());
        job.setScopeDefinitionId(planItemInstance.getCaseDefinitionId());

        return job;
    }

    protected static JobEntity createJob(VariableContainer variableContainer, BaseElement baseElement, String jobHandlerType, CmmnEngineConfiguration cmmnEngineConfiguration) {
        JobService jobService = cmmnEngineConfiguration.getJobServiceConfiguration().getJobService();
        JobEntity job = jobService.createJob();

        job.setJobHandlerType(jobHandlerType);
        job.setScopeType(ScopeTypes.CMMN);
        job.setElementId(baseElement.getId());
        if (baseElement instanceof CaseElement) {
            job.setElementName(((CaseElement) baseElement).getName());
        }

        List<ExtensionElement> jobCategoryElements = baseElement.getExtensionElements().get("jobCategory");
        if (jobCategoryElements != null && jobCategoryElements.size() > 0) {
            ExtensionElement jobCategoryElement = jobCategoryElements.get(0);
            if (StringUtils.isNotEmpty(jobCategoryElement.getElementText())) {
                Expression categoryExpression = cmmnEngineConfiguration.getExpressionManager().createExpression(jobCategoryElement.getElementText());
                Object categoryValue = categoryExpression.getValue(variableContainer);
                if (categoryValue != null) {
                    job.setCategory(categoryValue.toString());
                }
            }
        }


        job.setTenantId(variableContainer.getTenantId());

        return job;
    }

}
