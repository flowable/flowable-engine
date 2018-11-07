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

import java.util.Date;

import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link Job}s.
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public interface SuspendedJobQuery extends Query<SuspendedJobQuery, Job> {

    /** Only select jobs with the given id */
    SuspendedJobQuery jobId(String jobId);

    /** Only select jobs which exist for the given process instance. **/
    SuspendedJobQuery processInstanceId(String processInstanceId);

    /** Only select jobs which exist for the given execution */
    SuspendedJobQuery executionId(String executionId);

    /** Select jobs which have given job handler type */
    SuspendedJobQuery handlerType(String handlerType);

    /** Only select jobs which exist for the given process definition id */
    SuspendedJobQuery processDefinitionId(String processDefinitionid);
    
    /** Only select jobs for the given scope identifier. */
    SuspendedJobQuery scopeId(String scopeId);

    /** Only select jobs for the given sub scope identifier. */
    SuspendedJobQuery subScopeId(String subScopeId);
    
    /** Only select jobs for the given scope type. */
    SuspendedJobQuery scopeType(String scopeType);
    
    /** Only select jobs for the given scope definition identifier. */
    SuspendedJobQuery scopeDefinitionId(String scopeDefinitionId);
    
    /** Only select jobs for the given case instance. */
    SuspendedJobQuery caseInstanceId(String caseInstanceId);
    
    /** Only select jobs for the given case definition. */
    SuspendedJobQuery caseDefinitionId(String caseDefinitionId);
    
    /** Only select jobs for the given plan item instance.  */
    SuspendedJobQuery planItemInstanceId(String planItemInstanceId);

    /** Only select jobs which have retries left */
    SuspendedJobQuery withRetriesLeft();

    /** Only select jobs which have no retries left */
    SuspendedJobQuery noRetriesLeft();

    /**
     * Only select jobs which are executable, ie. retries &gt; 0 and duedate is null or duedate is in the past
     **/
    SuspendedJobQuery executable();

    /**
     * Only select jobs that are timers. Cannot be used together with {@link #messages()}
     */
    SuspendedJobQuery timers();

    /**
     * Only select jobs that are messages. Cannot be used together with {@link #timers()}
     */
    SuspendedJobQuery messages();

    /** Only select jobs where the duedate is lower than the given date. */
    SuspendedJobQuery duedateLowerThan(Date date);

    /** Only select jobs where the duedate is higher then the given date. */
    SuspendedJobQuery duedateHigherThan(Date date);

    /** Only select jobs that failed due to an exception. */
    SuspendedJobQuery withException();

    /** Only select jobs that failed due to an exception with the given message. */
    SuspendedJobQuery exceptionMessage(String exceptionMessage);

    /**
     * Only select jobs that have the given tenant id.
     */
    SuspendedJobQuery jobTenantId(String tenantId);

    /**
     * Only select jobs with a tenant id like the given one.
     */
    SuspendedJobQuery jobTenantIdLike(String tenantIdLike);

    /**
     * Only select jobs that do not have a tenant id.
     */
    SuspendedJobQuery jobWithoutTenantId();

    // sorting //////////////////////////////////////////

    /**
     * Order by job id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    SuspendedJobQuery orderByJobId();

    /**
     * Order by duedate (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    SuspendedJobQuery orderByJobDuedate();

    /**
     * Order by retries (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    SuspendedJobQuery orderByJobRetries();

    /**
     * Order by process instance id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    SuspendedJobQuery orderByProcessInstanceId();

    /**
     * Order by execution id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    SuspendedJobQuery orderByExecutionId();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    SuspendedJobQuery orderByTenantId();

}
