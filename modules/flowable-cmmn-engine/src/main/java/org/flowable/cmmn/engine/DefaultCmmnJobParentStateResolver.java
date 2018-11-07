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
