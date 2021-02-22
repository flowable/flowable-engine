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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.service.InternalJobManager;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.AcquiredExternalWorkerJobImpl;
import org.flowable.job.service.impl.ExternalWorkerJobAcquireBuilderImpl;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Filip Hrisafov
 */
public class AcquireExternalWorkerJobsCmd implements Command<List<AcquiredExternalWorkerJob>> {

    protected final String workerId;
    protected final int numberOfJobs;
    protected final ExternalWorkerJobAcquireBuilderImpl builder;
    protected final JobServiceConfiguration jobServiceConfiguration;

    public AcquireExternalWorkerJobsCmd(String workerId, int numberOfJobs, ExternalWorkerJobAcquireBuilderImpl builder,
            JobServiceConfiguration jobServiceConfiguration) {
        
        this.workerId = workerId;
        this.numberOfJobs = numberOfJobs;
        this.builder = builder;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public List<AcquiredExternalWorkerJob> execute(CommandContext commandContext) {
        String topic = builder.getTopic();
        if (StringUtils.isEmpty(topic)) {
            throw new FlowableIllegalArgumentException("topic must not be empty");
        }

        if (numberOfJobs < 1) {
            throw new FlowableIllegalArgumentException("requested number of jobs must not be smaller than 1");
        }

        if (StringUtils.isEmpty(workerId)) {
            throw new FlowableIllegalArgumentException("workerId must not be empty");
        }

        ExternalWorkerJobEntityManager externalWorkerJobEntityManager = jobServiceConfiguration.getExternalWorkerJobEntityManager();
        InternalJobManager internalJobManager = jobServiceConfiguration.getInternalJobManager();

        List<ExternalWorkerJobEntity> jobs = externalWorkerJobEntityManager.findExternalJobsToExecute(builder, numberOfJobs);

        int lockTimeInMillis = (int) builder.getLockDuration().abs().toMillis();
        List<AcquiredExternalWorkerJob> acquiredJobs = new ArrayList<>(jobs.size());

        for (ExternalWorkerJobEntity job : jobs) {
            lockJob(commandContext, job, lockTimeInMillis);
            Map<String, Object> variables = null;
            if (internalJobManager != null) {
                VariableScope variableScope = internalJobManager.resolveVariableScope(job);
                if (variableScope != null) {
                    variables = variableScope.getVariables();
                }

                if (job.isExclusive()) {
                    internalJobManager.lockJobScope(job);
                }
            }

            acquiredJobs.add(new AcquiredExternalWorkerJobImpl(job, variables));
        }

        return acquiredJobs;
    }

    protected void lockJob(CommandContext commandContext, JobInfoEntity job, int lockTimeInMillis) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(jobServiceConfiguration.getClock().getCurrentTime());
        gregorianCalendar.add(Calendar.MILLISECOND, lockTimeInMillis);
        job.setLockOwner(workerId);
        job.setLockExpirationTime(gregorianCalendar.getTime());
    }
}
