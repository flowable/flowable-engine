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

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.impl.cmd.UnlockExclusiveJobCmd;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityManager;

/**
 * @author Filip Hrisafov
 */
public abstract class AbstractExternalWorkerJobCmd implements Command<Void> {

    protected final String externalJobId;
    protected final String workerId;

    protected AbstractExternalWorkerJobCmd(String externalJobId, String workerId) {
        this.externalJobId = externalJobId;
        this.workerId = workerId;
    }

    @Override
    public final Void execute(CommandContext commandContext) {
        ExternalWorkerJobEntity externalWorkerJob = resolveJob(commandContext);

        if (!ScopeTypes.CMMN.equals(externalWorkerJob.getScopeType())) {
            throw new FlowableException(
                    "External worker job with id " + externalJobId + " is not cmmn scoped. This command can only handle cmmn scoped external worker jobs");
        }

        runJobLogic(externalWorkerJob, commandContext);
        if (externalWorkerJob.isExclusive()) {
            // Part of the same transaction to avoid a race condition with the
            // potentially new jobs (wrt process instance locking) that are created
            // during the execution of the original job
            CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
            new UnlockExclusiveJobCmd(externalWorkerJob, cmmnEngineConfiguration.getJobServiceConfiguration()).execute(commandContext);
        }
        return null;
    }

    protected abstract void runJobLogic(ExternalWorkerJobEntity externalWorkerJob, CommandContext commandContext);

    protected void moveExternalWorkerJobToExecutableJob(ExternalWorkerJobEntity externalWorkerJob, CommandContext commandContext) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        cmmnEngineConfiguration.getJobServiceConfiguration().getJobManager().moveExternalWorkerJobToExecutableJob(externalWorkerJob);

        cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                .deleteIdentityLinksByScopeIdAndType(externalWorkerJob.getCorrelationId(), ScopeTypes.EXTERNAL_WORKER);
    }

    protected ExternalWorkerJobEntity resolveJob(CommandContext commandContext) {
        if (StringUtils.isEmpty(externalJobId)) {
            throw new FlowableIllegalArgumentException("externalJobId must not be empty");
        }

        if (StringUtils.isEmpty(workerId)) {
            throw new FlowableIllegalArgumentException("workerId must not be empty");
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        ExternalWorkerJobEntityManager externalWorkerJobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getExternalWorkerJobEntityManager();

        ExternalWorkerJobEntity job = externalWorkerJobEntityManager.findById(externalJobId);

        if (job == null) {
            throw new FlowableObjectNotFoundException("No External Worker job found for id: " + externalJobId, ExternalWorkerJobEntity.class);
        }

        if (!Objects.equals(workerId, job.getLockOwner())) {
            throw new FlowableIllegalArgumentException(workerId + " does not hold a lock on the requested job");
        }

        return job;
    }
}
