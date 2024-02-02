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
package org.flowable.job.service;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Filip Hrisafov
 */
public abstract class ScopeAwareInternalJobManager implements InternalJobManager {

    protected Map<String, InternalJobManager> internalJobManagerByScopeType;

    @Override
    public void registerScopedInternalJobManager(String scopeType, InternalJobManager internalJobManager) {
        if (internalJobManagerByScopeType == null) {
            internalJobManagerByScopeType = new HashMap<>();
        }

        internalJobManagerByScopeType.put(scopeType, internalJobManager);
    }

    @Override
    public final VariableScope resolveVariableScope(Job job) {
        InternalJobManager internalJobManager = findInternalJobManager(job);
        if (internalJobManager == null) {
            return resolveVariableScopeInternal(job);
        }

        return internalJobManager.resolveVariableScope(job);
    }

    protected abstract VariableScope resolveVariableScopeInternal(Job job);

    @Override
    public final boolean handleJobInsert(Job job) {
        InternalJobManager internalJobManager = findInternalJobManager(job);
        if (internalJobManager == null) {
            return handleJobInsertInternal(job);
        }
        return internalJobManager.handleJobInsert(job);
    }

    protected abstract boolean handleJobInsertInternal(Job job);

    @Override
    public final void handleJobDelete(Job job) {
        InternalJobManager internalJobManager = findInternalJobManager(job);
        if (internalJobManager == null) {
            handleJobDeleteInternal(job);
        } else {
            internalJobManager.handleJobDelete(job);
        }
    }

    protected abstract void handleJobDeleteInternal(Job job);

    @Override
    public final void lockJobScope(Job job) {
        InternalJobManager internalJobManager = findInternalJobManager(job);
        if (internalJobManager == null) {
            lockJobScopeInternal(job);
        } else {
            internalJobManager.lockJobScope(job);
        }
    }

    protected abstract void lockJobScopeInternal(Job job);

    @Override
    public final void clearJobScopeLock(Job job) {
        InternalJobManager internalJobManager = findInternalJobManager(job);
        if (internalJobManager == null) {
            clearJobScopeLockInternal(job);
        } else {
            internalJobManager.clearJobScopeLock(job);
        }
    }

    protected abstract void clearJobScopeLockInternal(Job job);

    @Override
    public final void preTimerJobDelete(JobEntity jobEntity, VariableScope variableScope) {
        InternalJobManager internalJobManager = findInternalJobManager(jobEntity);
        if (internalJobManager == null) {
            preTimerJobDeleteInternal(jobEntity, variableScope);
        } else {
            internalJobManager.preTimerJobDelete(jobEntity, variableScope);
        }
    }

    protected abstract void preTimerJobDeleteInternal(JobEntity jobEntity, VariableScope variableScope);

    @Override
    public final void preRepeatedTimerSchedule(TimerJobEntity timerJobEntity, VariableScope variableScope) {
        InternalJobManager internalJobManager = findInternalJobManager(timerJobEntity);
        if (internalJobManager == null) {
            preRepeatedTimerScheduleInternal(timerJobEntity, variableScope);
        } else {
            internalJobManager.preRepeatedTimerSchedule(timerJobEntity, variableScope);
        }
    }

    protected abstract void preRepeatedTimerScheduleInternal(TimerJobEntity timerJobEntity, VariableScope variableScope);

    protected InternalJobManager findInternalJobManager(Job job) {
        if (internalJobManagerByScopeType == null || internalJobManagerByScopeType.isEmpty()) {
            return null;
        }

        String scopeType = job.getScopeType();
        if (scopeType == null && job.getProcessInstanceId() != null) {
            scopeType = ScopeTypes.BPMN;
        }

        return internalJobManagerByScopeType.get(scopeType);
    }
}
