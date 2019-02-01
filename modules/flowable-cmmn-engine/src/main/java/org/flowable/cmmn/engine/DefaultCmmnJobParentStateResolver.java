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
package org.flowable.cmmn.engine;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.job.api.Job;
import org.flowable.job.service.InternalJobParentStateResolver;

/**
 * @author martin.grofcik
 */
public class DefaultCmmnJobParentStateResolver implements InternalJobParentStateResolver {
    private CmmnEngineConfiguration cmmnEngineConfiguration;

    public DefaultCmmnJobParentStateResolver(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public boolean isSuspended(Job job) {
        if (!ScopeTypes.CMMN.equals(job.getScopeType()) || StringUtils.isEmpty(job.getScopeId())) {
            throw new FlowableIllegalArgumentException("Job "+ job.getId() +" parent is not CMMN case");
        }
        CaseInstance caseInstance = this.cmmnEngineConfiguration.cmmnRuntimeService.createCaseInstanceQuery().caseInstanceId(job.getScopeId()).singleResult();
        return CaseInstanceState.SUSPENDED.equals(caseInstance.getState());
    }
}
