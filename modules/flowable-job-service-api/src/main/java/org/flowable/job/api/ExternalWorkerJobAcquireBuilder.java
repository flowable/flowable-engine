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
package org.flowable.job.api;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * @author Filip Hrisafov
 */
public interface ExternalWorkerJobAcquireBuilder {

    /**
     * The topic and lock duration for the requested jobs
     *
     * @param topic the topic of the jobs
     * @param lockDuration the duration for locking the jobs
     */
    ExternalWorkerJobAcquireBuilder topic(String topic, Duration lockDuration);

    /**
     * Acquire only jobs which are linked to a process instance.
     * Cannot be combined with {@link #onlyCmmn()} and {@link #scopeType(String)}
     */
    ExternalWorkerJobAcquireBuilder onlyBpmn();

    /**
     * Acquire only jobs which are linked to a case instance.
     * Cannot be combined with {@link #onlyBpmn()} and {@link #scopeType(String)}
     */
    ExternalWorkerJobAcquireBuilder onlyCmmn();

    /**
     * Acquire only jobs which are linked to the given scope type.
     * Cannot be combined with {@link #onlyBpmn()} or {@link #onlyCmmn()}
     */
    ExternalWorkerJobAcquireBuilder scopeType(String scopeType);

    /**
     * Acquire only jobs which are within the given tenant.
     */
    ExternalWorkerJobAcquireBuilder tenantId(String tenantId);

    /**
     * Acquire only jobs where the given user or groups are authorized to execute.
     */
    ExternalWorkerJobAcquireBuilder forUserOrGroups(String userId, Collection<String> groups);

    /**
     * Acquire and lock the given number of jobs for the given worker id.
     * By default it will try to acquire jobs 5 times.
     * Use {@link #acquireAndLock(int, String, int)} if you need more retries.
     * If it fails to lock the jobs / scope after 5 retries it will return an empty list
     *
     * @param numberOfTasks the number of jobs to acquire
     * @param workerId the id of the worker acquiring the jobs
     */
    default List<AcquiredExternalWorkerJob> acquireAndLock(int numberOfTasks, String workerId) {
        return acquireAndLock(numberOfTasks, workerId, 5);
    }

    /**
     * Acquire and lock the given number of jobs for the given worker id.
     * If it fails to lock the jobs / scope after {@code numberOfRetries} it will return an empty list
     *
     * @param numberOfTasks the number of jobs to acquire
     * @param workerId the id of the worker acquiring the jobs
     * @param numberOfRetries the number of retries if an optimistic lock exception occurs during acquiring
     */
    List<AcquiredExternalWorkerJob> acquireAndLock(int numberOfTasks, String workerId, int numberOfRetries);

}
