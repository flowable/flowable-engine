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
package org.flowable.job.service.impl;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJobAcquireBuilder;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.cmd.AcquireExternalWorkerJobsCmd;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerJobAcquireBuilderImpl implements ExternalWorkerJobAcquireBuilder {

    protected final CommandExecutor commandExecutor;
    protected final JobServiceConfiguration jobServiceConfiguration;

    protected String topic;
    protected Duration lockDuration;
    protected String scopeType;
    protected String tenantId;
    protected String authorizedUser;
    protected Collection<String> authorizedGroups;

    public ExternalWorkerJobAcquireBuilderImpl(CommandExecutor commandExecutor, JobServiceConfiguration jobServiceConfiguration) {
        this.commandExecutor = commandExecutor;
        this.jobServiceConfiguration = jobServiceConfiguration;
    }

    @Override
    public ExternalWorkerJobAcquireBuilder topic(String topic, Duration lockDuration) {
        if (topic == null) {
            throw new FlowableIllegalArgumentException("topic is null");
        }

        if (lockDuration == null) {
            throw new FlowableIllegalArgumentException("lockDuration is null");
        }

        this.topic = topic;
        this.lockDuration = lockDuration;
        return this;
    }

    @Override
    public ExternalWorkerJobAcquireBuilder onlyBpmn() {
        if (ScopeTypes.CMMN.equals(scopeType)) {
            throw new FlowableIllegalArgumentException("Cannot combine onlyCmmn() with onlyBpmn() in the same query");
        }

        if (scopeType != null) {
            throw new FlowableIllegalArgumentException("Cannot combine scopeType(String) with onlyBpmn() in the same query");
        }
        return scopeType(ScopeTypes.BPMN);
    }

    @Override
    public ExternalWorkerJobAcquireBuilder onlyCmmn() {
        if (ScopeTypes.BPMN.equals(scopeType)) {
            throw new FlowableIllegalArgumentException("Cannot combine onlyBpmn() with onlyCmmn() in the same query");
        }

        if (scopeType != null) {
            throw new FlowableIllegalArgumentException("Cannot combine scopeType(String) with onlyCmmn() in the same query");
        }
        return scopeType(ScopeTypes.CMMN);
    }

    @Override
    public ExternalWorkerJobAcquireBuilder scopeType(String scopeType) {
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public ExternalWorkerJobAcquireBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public ExternalWorkerJobAcquireBuilder forUserOrGroups(String userId, Collection<String> groups) {
        if (userId == null && (groups == null || groups.isEmpty())) {
            throw new FlowableIllegalArgumentException("at least one of userId or groups must be provided");
        }

        this.authorizedUser = userId;
        this.authorizedGroups = groups;

        return this;
    }

    @Override
    public List<AcquiredExternalWorkerJob> acquireAndLock(int numberOfTasks, String workerId, int numberOfRetries) {
        while (numberOfRetries > 0) {
            try {
                return commandExecutor.execute(new AcquireExternalWorkerJobsCmd(workerId, numberOfTasks, this, jobServiceConfiguration));
            } catch (FlowableOptimisticLockingException ignored) {
                // Query for jobs until there is no FlowableOptimisticLockingException
                // It is potentially possible multiple workers to query in the exact same time
                numberOfRetries--;
            }
        }
        return Collections.emptyList();
    }

    public String getTopic() {
        return topic;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAuthorizedUser() {
        return authorizedUser;
    }

    public Collection<String> getAuthorizedGroups() {
        return authorizedGroups;
    }
}
