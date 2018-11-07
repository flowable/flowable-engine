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

import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link HistoryJob}s.
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface HistoryJobQuery extends Query<HistoryJobQuery, HistoryJob> {

    /** Only select jobs with the given id */
    HistoryJobQuery jobId(String jobId);

    /** Select jobs which have given job handler type */
    HistoryJobQuery handlerType(String handlerType);

    /** Only select jobs that failed due to an exception. */
    HistoryJobQuery withException();

    /** Only select jobs that failed due to an exception with the given message. */
    HistoryJobQuery exceptionMessage(String exceptionMessage);
    
    /**
     * Only select jobs with the given scope type.
     */
    HistoryJobQuery scopeType(String scopeType);

    /**
     * Only select jobs that have the given tenant id.
     */
    HistoryJobQuery jobTenantId(String tenantId);

    /**
     * Only select jobs with a tenant id like the given one.
     */
    HistoryJobQuery jobTenantIdLike(String tenantIdLike);

    /**
     * Only select jobs that do not have a tenant id.
     */
    HistoryJobQuery jobWithoutTenantId();

    /**
     * Only return jobs with the given lock owner.
     */
    HistoryJobQuery lockOwner(String lockOwner);

    /**
     * Only return jobs that are locked (i.e. they are acquired by an executor).
     */
    HistoryJobQuery locked();

    /**
     * Only return jobs that are not locked.
     */
    HistoryJobQuery unlocked();

    // sorting //////////////////////////////////////////

    /**
     * Order by job id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    HistoryJobQuery orderByJobId();

    /**
     * Order by duedate (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    HistoryJobQuery orderByJobDuedate();

    /**
     * Order by retries (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    HistoryJobQuery orderByJobRetries();

    /**
     * Order by process instance id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    HistoryJobQuery orderByProcessInstanceId();

    /**
     * Order by execution id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    HistoryJobQuery orderByExecutionId();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    HistoryJobQuery orderByTenantId();

}
