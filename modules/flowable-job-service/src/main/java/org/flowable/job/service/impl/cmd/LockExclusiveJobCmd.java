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
package org.flowable.job.service.impl.cmd;

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.Job;
import org.flowable.job.service.InternalJobManager;
import org.flowable.job.service.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class LockExclusiveJobCmd implements Command<Object>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(LockExclusiveJobCmd.class);

    protected Job job;

    public LockExclusiveJobCmd(Job job) {
        this.job = job;
    }

    @Override
    public Object execute(CommandContext commandContext) {

        if (job == null) {
            throw new FlowableIllegalArgumentException("job is null");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Executing lock exclusive job {} {}", job.getId(), job.getExecutionId());
        }

        if (job.isExclusive()) {
            if (job.getExecutionId() != null || job.getScopeId() != null) {
                InternalJobManager internalJobManager = CommandContextUtil.getJobServiceConfiguration().getInternalJobManager();
                if (internalJobManager != null) {
                    internalJobManager.lockJobScope(job);
                }
            }
        }

        return null;
    }
}
