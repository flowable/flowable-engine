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
package org.flowable.cmmn.engine.impl.callback;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.job.api.Job;
import org.flowable.job.service.InternalJobManager;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Joram Barrez
 */
public class DefaultInternalCmmnJobManager implements InternalJobManager {
    
    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    public DefaultInternalCmmnJobManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public VariableScope resolveVariableScope(Job job) {
        return cmmnEngineConfiguration.getPlanItemInstanceEntityManager().findById(job.getSubScopeId());
    }

    @Override
    public boolean handleJobInsert(Job job) {
        // Currently, nothing extra needed (but counting relationships can be added later here).
        return true;
    }

    @Override
    public void handleJobDelete(Job job) {
        // Currently, nothing extra needed (but counting relationships can be added later here).        
    }

    @Override
    public void lockJobScope(Job job) {
        // TODO Auto-generated method stub
    }

    @Override
    public void clearJobScopeLock(Job job) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void preTimerJobDelete(JobEntity jobEntity, VariableScope variableScope) {
        // Nothing additional needed (no support for endDate for cmmn timer yet)
    }

}
