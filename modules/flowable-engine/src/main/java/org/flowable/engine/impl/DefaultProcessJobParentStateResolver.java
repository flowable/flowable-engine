package org.flowable.engine.impl;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.runtime.CaseInstanceState;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.job.service.InternalJobParentStateResolver;

/**
 * @author martin.grofcik
 */
public class DefaultProcessJobParentStateResolver implements InternalJobParentStateResolver {
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public DefaultProcessJobParentStateResolver(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public boolean isSuspended(Job job) {
        if (StringUtils.isEmpty(job.getProcessInstanceId())) {
            throw new FlowableIllegalArgumentException("Job " + job.getId() + " parent is not process instance");
        }
        ProcessInstance processInstance = this.processEngineConfiguration.getRuntimeService().createProcessInstanceQuery().processInstanceId(job.getProcessInstanceId()).singleResult();
        return processInstance.isSuspended();
    }
}
