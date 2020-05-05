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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class ScopeAwareInternalJobManagerTest {

    @Test
    void invokeMethodsWithoutRegisteredScopeInternalManager() {
        TestScopeAwareInternalJobManager manager = new TestScopeAwareInternalJobManager();

        Job scopeJob = mock(Job.class, "scopeJob");
        Job insertJob = mock(Job.class, "insertJob");
        Job deleteJob = mock(Job.class, "deleteJob");
        Job lockJob = mock(Job.class, "lockJob");
        Job clearLockJob = mock(Job.class, "clearLockJob");
        JobEntity timerDeleteJob = mock(JobEntity.class, "timerDeleteJob");
        TimerJobEntity repeatedTimerSchedule = mock(TimerJobEntity.class, "repeatedTimerSchedule");

        VariableScope resolveJobScope = mock(VariableScope.class, "resolveJobScope");
        VariableScope timerDeleteJobScope = mock(VariableScope.class, "timerDeleteJobScope");
        VariableScope repeatedTimerScheduleScope = mock(VariableScope.class, "repeatedTimerScheduleScope");

        manager.variableScopeByJob.put(scopeJob, resolveJobScope);
        manager.insertJobInternalByJob.put(insertJob, Boolean.TRUE);

        InternalJobManager underTest = manager;

        assertThat(underTest.resolveVariableScope(scopeJob)).isEqualTo(resolveJobScope);
        assertThat(underTest.handleJobInsert(insertJob)).isTrue();
        underTest.handleJobDelete(deleteJob);
        underTest.lockJobScope(lockJob);
        underTest.clearJobScopeLock(clearLockJob);
        underTest.preTimerJobDelete(timerDeleteJob, timerDeleteJobScope);
        underTest.preRepeatedTimerSchedule(repeatedTimerSchedule, repeatedTimerScheduleScope);

        assertThat(manager.invokedMethods)
                .containsExactly(
                        "resolveVariableScopeInternal",
                        "handleJobInsertInternal",
                        "handleJobDeleteInternal",
                        "lockJobScopeInternal",
                        "clearJobScopeLockInternal",
                        "preTimerJobDeleteInternal",
                        "preRepeatedTimerScheduleInternal"
                );

        assertThat(manager.jobDeleteInternal).isEqualTo(deleteJob);
        assertThat(manager.lockJobScopeInternal).isEqualTo(lockJob);
        assertThat(manager.clearJobScopeInternal).isEqualTo(clearLockJob);
        assertThat(manager.timerJobDeleteInternal).isEqualTo(timerDeleteJob);
        assertThat(manager.timerJobDeleteInternalVariableScope).isEqualTo(timerDeleteJobScope);
        assertThat(manager.repeatedTimerScheduleInternal).isEqualTo(repeatedTimerSchedule);
        assertThat(manager.repeatedTimerScheduleInternalVariableScope).isEqualTo(repeatedTimerScheduleScope);
    }

    @Test
    void invokeMethodsWithBpmnScopeInternalManager() {
        TestScopeAwareInternalJobManager defaultManager = new TestScopeAwareInternalJobManager();
        TestScopeAwareInternalJobManager bpmnManager = new TestScopeAwareInternalJobManager();

        Job scopeJob = mockBpmnJob(Job.class, "scopeJob");
        Job insertJob = mockBpmnJob(Job.class, "insertJob");
        Job deleteJob = mockBpmnJob(Job.class, "deleteJob");
        Job lockJob = mockBpmnJob(Job.class, "lockJob");
        Job clearLockJob = mockBpmnJob(Job.class, "clearLockJob");
        JobEntity timerDeleteJob = mockBpmnJob(JobEntity.class, "timerDeleteJob");
        TimerJobEntity repeatedTimerSchedule = mockBpmnJob(TimerJobEntity.class, "repeatedTimerSchedule");

        VariableScope resolveJobScope = mock(VariableScope.class, "resolveJobScope");
        VariableScope timerDeleteJobScope = mock(VariableScope.class, "timerDeleteJobScope");
        VariableScope repeatedTimerScheduleScope = mock(VariableScope.class, "repeatedTimerScheduleScope");

        bpmnManager.variableScopeByJob.put(scopeJob, resolveJobScope);
        bpmnManager.insertJobInternalByJob.put(insertJob, Boolean.TRUE);

        defaultManager.registerScopedInternalJobManager(ScopeTypes.BPMN, bpmnManager);

        InternalJobManager underTest = defaultManager;

        assertThat(underTest.resolveVariableScope(scopeJob)).isEqualTo(resolveJobScope);
        assertThat(underTest.handleJobInsert(insertJob)).isTrue();
        underTest.handleJobDelete(deleteJob);
        underTest.lockJobScope(lockJob);
        underTest.clearJobScopeLock(clearLockJob);
        underTest.preTimerJobDelete(timerDeleteJob, timerDeleteJobScope);
        underTest.preRepeatedTimerSchedule(repeatedTimerSchedule, repeatedTimerScheduleScope);

        assertThat(bpmnManager.invokedMethods)
                .containsExactly(
                        "resolveVariableScopeInternal",
                        "handleJobInsertInternal",
                        "handleJobDeleteInternal",
                        "lockJobScopeInternal",
                        "clearJobScopeLockInternal",
                        "preTimerJobDeleteInternal",
                        "preRepeatedTimerScheduleInternal"
                );

        assertThat(bpmnManager.jobDeleteInternal).isEqualTo(deleteJob);
        assertThat(bpmnManager.lockJobScopeInternal).isEqualTo(lockJob);
        assertThat(bpmnManager.clearJobScopeInternal).isEqualTo(clearLockJob);
        assertThat(bpmnManager.timerJobDeleteInternal).isEqualTo(timerDeleteJob);
        assertThat(bpmnManager.timerJobDeleteInternalVariableScope).isEqualTo(timerDeleteJobScope);
        assertThat(bpmnManager.repeatedTimerScheduleInternal).isEqualTo(repeatedTimerSchedule);
        assertThat(bpmnManager.repeatedTimerScheduleInternalVariableScope).isEqualTo(repeatedTimerScheduleScope);

        assertThat(defaultManager.invokedMethods).isEmpty();
    }

    @Test
    void invokeMethodsWithCmmScopeInternalManager() {
        TestScopeAwareInternalJobManager defaultManager = new TestScopeAwareInternalJobManager();
        TestScopeAwareInternalJobManager cmmnManager = new TestScopeAwareInternalJobManager();

        Job scopeJob = mockCmmnJob(Job.class, "scopeJob");
        Job insertJob = mockCmmnJob(Job.class, "insertJob");
        Job deleteJob = mockCmmnJob(Job.class, "deleteJob");
        Job lockJob = mockCmmnJob(Job.class, "lockJob");
        Job clearLockJob = mockCmmnJob(Job.class, "clearLockJob");
        JobEntity timerDeleteJob = mockCmmnJob(JobEntity.class, "timerDeleteJob");
        TimerJobEntity repeatedTimerSchedule = mockCmmnJob(TimerJobEntity.class, "repeatedTimerSchedule");

        VariableScope resolveJobScope = mock(VariableScope.class, "resolveJobScope");
        VariableScope timerDeleteJobScope = mock(VariableScope.class, "timerDeleteJobScope");
        VariableScope repeatedTimerScheduleScope = mock(VariableScope.class, "repeatedTimerScheduleScope");

        cmmnManager.variableScopeByJob.put(scopeJob, resolveJobScope);
        cmmnManager.insertJobInternalByJob.put(insertJob, Boolean.TRUE);

        defaultManager.registerScopedInternalJobManager(ScopeTypes.CMMN, cmmnManager);

        InternalJobManager underTest = defaultManager;

        assertThat(underTest.resolveVariableScope(scopeJob)).isEqualTo(resolveJobScope);
        assertThat(underTest.handleJobInsert(insertJob)).isTrue();
        underTest.handleJobDelete(deleteJob);
        underTest.lockJobScope(lockJob);
        underTest.clearJobScopeLock(clearLockJob);
        underTest.preTimerJobDelete(timerDeleteJob, timerDeleteJobScope);
        underTest.preRepeatedTimerSchedule(repeatedTimerSchedule, repeatedTimerScheduleScope);

        assertThat(cmmnManager.invokedMethods)
                .containsExactly(
                        "resolveVariableScopeInternal",
                        "handleJobInsertInternal",
                        "handleJobDeleteInternal",
                        "lockJobScopeInternal",
                        "clearJobScopeLockInternal",
                        "preTimerJobDeleteInternal",
                        "preRepeatedTimerScheduleInternal"
                );

        assertThat(cmmnManager.jobDeleteInternal).isEqualTo(deleteJob);
        assertThat(cmmnManager.lockJobScopeInternal).isEqualTo(lockJob);
        assertThat(cmmnManager.clearJobScopeInternal).isEqualTo(clearLockJob);
        assertThat(cmmnManager.timerJobDeleteInternal).isEqualTo(timerDeleteJob);
        assertThat(cmmnManager.timerJobDeleteInternalVariableScope).isEqualTo(timerDeleteJobScope);
        assertThat(cmmnManager.repeatedTimerScheduleInternal).isEqualTo(repeatedTimerSchedule);
        assertThat(cmmnManager.repeatedTimerScheduleInternalVariableScope).isEqualTo(repeatedTimerScheduleScope);

        assertThat(defaultManager.invokedMethods).isEmpty();
    }

    protected <T extends Job> T mockBpmnJob(Class<T> jobClass, String mockName) {
        T job = mock(jobClass, mockName);
        when(job.getProcessInstanceId()).thenReturn(mockName);
        return job;
    }

    protected <T extends Job> T mockCmmnJob(Class<T> jobClass, String mockName) {
        T job = mock(jobClass, mockName);
        when(job.getScopeType()).thenReturn(ScopeTypes.CMMN);
        return job;
    }

    private static class TestScopeAwareInternalJobManager extends ScopeAwareInternalJobManager {

        protected Map<Job, VariableScope> variableScopeByJob = new HashMap<>();
        protected Map<Job, Boolean> insertJobInternalByJob = new HashMap<>();
        protected Job jobDeleteInternal;
        protected Job lockJobScopeInternal;
        protected Job clearJobScopeInternal;
        protected Job timerJobDeleteInternal;
        protected VariableScope timerJobDeleteInternalVariableScope;
        protected Job repeatedTimerScheduleInternal;
        protected VariableScope repeatedTimerScheduleInternalVariableScope;
        protected List<String> invokedMethods = new ArrayList<>();

        @Override
        protected VariableScope resolveVariableScopeInternal(Job job) {
            invokedMethods.add("resolveVariableScopeInternal");
            return variableScopeByJob.get(job);
        }

        @Override
        protected boolean handleJobInsertInternal(Job job) {
            invokedMethods.add("handleJobInsertInternal");
            return insertJobInternalByJob.get(job);
        }

        @Override
        protected void handleJobDeleteInternal(Job job) {
            invokedMethods.add("handleJobDeleteInternal");
            jobDeleteInternal = job;
        }

        @Override
        protected void lockJobScopeInternal(Job job) {
            invokedMethods.add("lockJobScopeInternal");
            lockJobScopeInternal = job;
        }

        @Override
        protected void clearJobScopeLockInternal(Job job) {
            invokedMethods.add("clearJobScopeLockInternal");
            clearJobScopeInternal = job;
        }

        @Override
        protected void preTimerJobDeleteInternal(JobEntity jobEntity, VariableScope variableScope) {
            invokedMethods.add("preTimerJobDeleteInternal");
            timerJobDeleteInternal = jobEntity;
            timerJobDeleteInternalVariableScope = variableScope;
        }

        @Override
        protected void preRepeatedTimerScheduleInternal(TimerJobEntity timerJobEntity, VariableScope variableScope) {
            invokedMethods.add("preRepeatedTimerScheduleInternal");
            repeatedTimerScheduleInternal = timerJobEntity;
            repeatedTimerScheduleInternalVariableScope = variableScope;
        }
    }

}
