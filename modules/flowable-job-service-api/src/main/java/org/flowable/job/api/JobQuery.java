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
 * @author Falko Menge
 */
public interface JobQuery extends Query<JobQuery, Job> {

    /** Only select jobs with the given id */
    JobQuery jobId(String jobId);

    /** Only select jobs which exist for the given process instance. **/
    JobQuery processInstanceId(String processInstanceId);

    /** Only select jobs which exist for the given execution */
    JobQuery executionId(String executionId);

    /** Select jobs which have given job handler type */
    JobQuery handlerType(String handlerType);

    /** Only select jobs which exist for the given process definition id */
    JobQuery processDefinitionId(String processDefinitionId);
    
    /** Only select tasks for the given scope identifier. */
    JobQuery scopeId(String scopeId);

    /** Only select tasks for the given sub scope identifier. */
    JobQuery subScopeId(String subScopeId);
    
    /** Only select tasks for the given scope type. */
    JobQuery scopeType(String scopeType);
    
    /** Only select tasks for the given scope definition identifier. */
    JobQuery scopeDefinitionId(String scopeDefinitionId);
    
    /** Only select jobs for the given case instance. */
    JobQuery caseInstanceId(String caseInstanceId);
    
    /** Only select jobs for the given case definition. */
    JobQuery caseDefinitionId(String caseDefinitionId);
    
    /** Only select jobs for the given plan item instance.  */
    JobQuery planItemInstanceId(String planItemInstanceId);
    
    /**
     * Only select jobs that are timers. Cannot be used together with {@link #messages()}
     */
    JobQuery timers();

    /**
     * Only select jobs that are messages. Cannot be used together with {@link #timers()}
     */
    JobQuery messages();

    /** Only select jobs where the duedate is lower than the given date. */
    JobQuery duedateLowerThan(Date date);

    /** Only select jobs where the duedate is higher then the given date. */
    JobQuery duedateHigherThan(Date date);

    /** Only select jobs that failed due to an exception. */
    JobQuery withException();

    /** Only select jobs that failed due to an exception with the given message. */
    JobQuery exceptionMessage(String exceptionMessage);

    /**
     * Only select jobs that have the given tenant id.
     */
    JobQuery jobTenantId(String tenantId);

    /**
     * Only select jobs with a tenant id like the given one.
     */
    JobQuery jobTenantIdLike(String tenantIdLike);

    /**
     * Only select jobs that do not have a tenant id.
     */
    JobQuery jobWithoutTenantId();

    /**
     * Only return jobs with the given lock owner.
     */
    JobQuery lockOwner(String lockOwner);

    /**
     * Only return jobs that are locked (i.e. they are acquired by an executor).
     */
    JobQuery locked();

    /**
     * Only return jobs that are not locked.
     */
    JobQuery unlocked();

    // sorting //////////////////////////////////////////

    /**
     * Order by job id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    JobQuery orderByJobId();

    /**
     * Order by duedate (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    JobQuery orderByJobDuedate();

    /**
     * Order by retries (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    JobQuery orderByJobRetries();

    /**
     * Order by process instance id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    JobQuery orderByProcessInstanceId();

    /**
     * Order by execution id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    JobQuery orderByExecutionId();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    JobQuery orderByTenantId();

}
