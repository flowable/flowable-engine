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
package org.flowable.engine.impl.persistence.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.engine.impl.persistence.CountingExecutionEntity;

/**
 * @author Filip Hrisafov
 */
public class CountingExecutionEntityImpl extends AbstractEntityNoRevision implements CountingExecutionEntity {

    protected final ExecutionEntity executionEntity;

    protected int eventSubscriptionCount = 0;
    protected final AtomicInteger eventSubscriptionDeltaCount = new AtomicInteger(0);

    protected int taskCount = 0;
    protected final AtomicInteger taskDeltaCount = new AtomicInteger(0);

    protected int jobCount = 0;
    protected final AtomicInteger jobDeltaCount = new AtomicInteger(0);

    protected int timerJobCount = 0;
    protected final AtomicInteger timerJobDeltaCount = new AtomicInteger(0);

    protected int suspendedJobCount = 0;
    protected final AtomicInteger suspendedJobDeltaCount = new AtomicInteger(0);

    protected int deadLetterJobCount = 0;
    protected final AtomicInteger deadLetterJobDeltaCount = new AtomicInteger(0);

    protected int externalWorkerJobCount = 0;
    protected AtomicInteger externalWorkerJobDeltaCount = new AtomicInteger(0);

    protected int variableCount = 0;
    protected AtomicInteger variableDeltaCount = new AtomicInteger(0);

    protected int identityLinkCount = 0;
    protected AtomicInteger identityLinkDeltaCount = new AtomicInteger(0);

    protected Object originalPersistentState;

    public CountingExecutionEntityImpl(ExecutionEntity executionEntity) {
        this.executionEntity = executionEntity;
    }

    @Override
    public String getIdPrefix() {
        return "";
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("eventSubscriptionDeltaCount", eventSubscriptionDeltaCount.get());
        persistentState.put("taskDeltaCount", taskDeltaCount.get());
        persistentState.put("jobDeltaCount", jobDeltaCount.get());
        persistentState.put("timerJobDeltaCount", timerJobDeltaCount.get());
        persistentState.put("suspendedJobDeltaCount", suspendedJobDeltaCount.get());
        persistentState.put("deadLetterJobDeltaCount", deadLetterJobDeltaCount.get());
        persistentState.put("externalWorkerJobDeltaCount", externalWorkerJobDeltaCount.get());
        persistentState.put("variableDeltaCount", variableDeltaCount.get());
        persistentState.put("identityLinkDeltaCount", identityLinkDeltaCount.get());

        return persistentState;
    }

    @Override
    public boolean isProcessInstanceType() {
        return executionEntity.isProcessInstanceType();
    }

    @Override
    public Object getOriginalPersistentState() {
        return originalPersistentState;
    }

    @Override
    public void setOriginalPersistentState(Object persistentState) {
        this.originalPersistentState = persistentState;
    }

    @Override
    public boolean isCountEnabled() {
        return executionEntity.isCountEnabled();
    }

    @Override
    public int getEventSubscriptionCount() {
        return eventSubscriptionCount + eventSubscriptionDeltaCount.get();
    }

    public void setEventSubscriptionCount(int eventSubscriptionCount) {
        this.eventSubscriptionCount = eventSubscriptionCount;
    }

    public int getEventSubscriptionDeltaCount() {
        return eventSubscriptionDeltaCount.get();
    }

    @Override
    public void incrementEventSubscriptionCount() {
        eventSubscriptionDeltaCount.incrementAndGet();
    }

    @Override
    public void decrementEventSubscriptionCount() {
        eventSubscriptionDeltaCount.decrementAndGet();
    }

    @Override
    public int getTaskCount() {
        return taskCount + taskDeltaCount.get();
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public int getTaskDeltaCount() {
        return taskDeltaCount.get();
    }

    @Override
    public void incrementTaskCount() {
        taskDeltaCount.incrementAndGet();
    }

    @Override
    public void decrementTaskCount() {
        taskDeltaCount.decrementAndGet();
    }

    @Override
    public int getJobCount() {
        return jobCount + timerJobDeltaCount.get();
    }

    public void setJobCount(int jobCount) {
        this.jobCount = jobCount;
    }

    public int getJobDeltaCount() {
        return jobDeltaCount.get();
    }

    @Override
    public void incrementJobCount() {
        jobDeltaCount.incrementAndGet();
    }

    @Override
    public void decrementJobCount() {
        jobDeltaCount.decrementAndGet();
    }

    @Override
    public int getTimerJobCount() {
        return timerJobCount + timerJobDeltaCount.get();
    }

    public void setTimerJobCount(int timerJobCount) {
        this.timerJobCount = timerJobCount;
    }

    public int getTimerJobDeltaCount() {
        return timerJobDeltaCount.get();
    }

    @Override
    public void incrementTimerJobCount() {
        timerJobDeltaCount.incrementAndGet();
    }

    @Override
    public void decrementTimerJobCount() {
        timerJobDeltaCount.decrementAndGet();
    }

    @Override
    public int getSuspendedJobCount() {
        return suspendedJobCount + suspendedJobDeltaCount.get();
    }

    public void setSuspendedJobCount(int suspendedJobCount) {
        this.suspendedJobCount = suspendedJobCount;
    }

    public int getSuspendedJobDeltaCount() {
        return suspendedJobDeltaCount.get();
    }

    @Override
    public void incrementSuspendedJobCount() {
        suspendedJobDeltaCount.incrementAndGet();
    }

    @Override
    public void decrementSuspendedJobCount() {
        suspendedJobDeltaCount.decrementAndGet();
    }

    @Override
    public int getDeadLetterJobCount() {
        return deadLetterJobCount + deadLetterJobDeltaCount.get();
    }

    public void setDeadLetterJobCount(int deadLetterJobCount) {
        this.deadLetterJobCount = deadLetterJobCount;
    }

    public int getDeadLetterJobDeltaCount() {
        return deadLetterJobDeltaCount.get();
    }

    @Override
    public void incrementDeadLetterJobCount() {
        deadLetterJobDeltaCount.incrementAndGet();
    }

    @Override
    public void decrementDeadLetterJobCount() {
        deadLetterJobDeltaCount.decrementAndGet();
    }

    @Override
    public int getExternalWorkerJobCount() {
        return externalWorkerJobCount + externalWorkerJobDeltaCount.get();
    }

    public void setExternalWorkerJobCount(int externalWorkerJobCount) {
        this.externalWorkerJobCount = externalWorkerJobCount;
    }

    public int getExternalWorkerJobDeltaCount() {
        return externalWorkerJobDeltaCount.get();
    }

    @Override
    public void incrementExternalWorkerJobCount() {
        externalWorkerJobDeltaCount.incrementAndGet();
    }

    @Override
    public void decrementExternalWorkerJobCount() {
        externalWorkerJobDeltaCount.decrementAndGet();
    }

    @Override
    public int getVariableCount() {
        return variableCount + variableDeltaCount.get();
    }

    public void setVariableCount(int variableCount) {
        this.variableCount = variableCount;
    }

    public int getVariableDeltaCount() {
        return variableDeltaCount.get();
    }

    @Override
    public void incrementVariableCount() {
        variableDeltaCount.incrementAndGet();
    }

    @Override
    public void decrementVariableCount() {
        variableDeltaCount.decrementAndGet();
    }

    @Override
    public int getIdentityLinkCount() {
        return identityLinkCount + identityLinkDeltaCount.get();
    }

    public void setIdentityLinkCount(int identityLinkCount) {
        this.identityLinkCount = identityLinkCount;
    }

    public int getIdentityLinkDeltaCount() {
        return identityLinkDeltaCount.get();
    }

    @Override
    public void incrementIdentityLinkCount() {
        identityLinkDeltaCount.incrementAndGet();
    }

    @Override
    public void decrementIdentityLinkCount() {
        identityLinkDeltaCount.decrementAndGet();
    }
}
