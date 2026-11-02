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
package org.flowable.cmmn.engine.impl.cmd;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.job.SetAsyncVariablesJobHandler;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.job.service.JobService;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

public abstract class AbstractSetVariableAsyncCmd {
    
    protected void addVariable(boolean isLocal, String scopeId, String subScopeId, String varName, Object varValue, 
            String tenantId, VariableService variableService) {
        
        VariableInstanceEntity variableInstance = variableService.createVariableInstance(varName);
        variableInstance.setScopeId(scopeId);
        variableInstance.setSubScopeId(subScopeId);
        variableInstance.setScopeType(ScopeTypes.CMMN_ASYNC_VARIABLES);
        variableInstance.setMetaInfo(String.valueOf(isLocal));

        variableService.insertVariableInstanceWithValue(variableInstance, varValue, tenantId);
    }
    
    protected void createSetAsyncVariablesJob(CaseInstanceEntity caseInstanceEntity, CmmnEngineConfiguration cmmnEngineConfiguration) {
        JobServiceConfiguration jobServiceConfiguration = cmmnEngineConfiguration.getJobServiceConfiguration();
        JobService jobService = jobServiceConfiguration.getJobService();

        JobEntity job = jobService.createJob();
        job.setScopeId(caseInstanceEntity.getId());
        job.setScopeDefinitionId(caseInstanceEntity.getCaseDefinitionId());
        job.setScopeType(ScopeTypes.CMMN);
        job.setJobHandlerType(SetAsyncVariablesJobHandler.TYPE);

        // Inherit tenant id (if applicable)
        if (caseInstanceEntity.getTenantId() != null) {
            job.setTenantId(caseInstanceEntity.getTenantId());
        }

        jobService.createAsyncJob(job, true);
        jobService.scheduleAsyncJob(job);
    }
    
    protected void createSetAsyncVariablesJob(PlanItemInstanceEntity planItemInstanceEntity, CmmnEngineConfiguration cmmnEngineConfiguration) {
        JobServiceConfiguration jobServiceConfiguration = cmmnEngineConfiguration.getJobServiceConfiguration();
        JobService jobService = jobServiceConfiguration.getJobService();

        JobEntity job = jobService.createJob();
        job.setScopeId(planItemInstanceEntity.getCaseInstanceId());
        job.setSubScopeId(planItemInstanceEntity.getId());
        job.setScopeDefinitionId(planItemInstanceEntity.getCaseDefinitionId());
        job.setScopeType(ScopeTypes.CMMN);
        job.setJobHandlerType(SetAsyncVariablesJobHandler.TYPE);

        // Inherit tenant id (if applicable)
        if (planItemInstanceEntity.getTenantId() != null) {
            job.setTenantId(planItemInstanceEntity.getTenantId());
        }

        jobService.createAsyncJob(job, true);
        jobService.scheduleAsyncJob(job);
    }

}
