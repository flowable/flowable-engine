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
package org.flowable.engine.impl;

import org.apache.commons.lang3.StringUtils;
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
