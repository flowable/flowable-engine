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

import java.util.Collection;
import java.util.Date;

import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of External Worker {@link Job}s.
 *
 * @author Filip Hrisafov
 */
public interface ExternalWorkerJobQuery extends Query<ExternalWorkerJobQuery, ExternalWorkerJob> {

    /**
     * Only select jobs with the given id
     */
    ExternalWorkerJobQuery jobId(String jobId);

    /**
     * Only select jobs which exist for the given process instance.
     **/
    ExternalWorkerJobQuery processInstanceId(String processInstanceId);

    /**
     * Only select jobs which exist for the given execution
     */
    ExternalWorkerJobQuery executionId(String executionId);

    /**
     * Select jobs which have given job handler type
     */
    ExternalWorkerJobQuery handlerType(String handlerType);

    /**
     * Only select jobs which exist for the given process definition id
     */
    ExternalWorkerJobQuery processDefinitionId(String processDefinitionId);

    /**
     * Only select jobs which exist for the given category
     */
    ExternalWorkerJobQuery category(String category);

    /**
     * Only select jobs like for the given category value
     */
    ExternalWorkerJobQuery categoryLike(String categoryLike);

    /**
     * Only select jobs which exist for the given element id
     */
    ExternalWorkerJobQuery elementId(String elementId);

    /**
     * Only select jobs which exist for the given element name
     */
    ExternalWorkerJobQuery elementName(String elementName);

    /**
     * Only select tasks for the given scope identifier.
     */
    ExternalWorkerJobQuery scopeId(String scopeId);

    /**
     * Only select tasks for the given sub scope identifier.
     */
    ExternalWorkerJobQuery subScopeId(String subScopeId);

    /**
     * Only select tasks for the given scope type.
     */
    ExternalWorkerJobQuery scopeType(String scopeType);

    /**
     * Only select tasks for the given scope definition identifier.
     */
    ExternalWorkerJobQuery scopeDefinitionId(String scopeDefinitionId);

    /**
     * Only select jobs for the given case instance.
     */
    ExternalWorkerJobQuery caseInstanceId(String caseInstanceId);

    /**
     * Only select jobs for the given case definition.
     */
    ExternalWorkerJobQuery caseDefinitionId(String caseDefinitionId);

    /**
     * Only select jobs for the given plan item instance.
     */
    ExternalWorkerJobQuery planItemInstanceId(String planItemInstanceId);

    /**
     * Only select jobs with the given correlationId.
     */
    ExternalWorkerJobQuery correlationId(String correlationId);

    /**
     * Only select jobs where the duedate is lower than the given date.
     */
    ExternalWorkerJobQuery duedateLowerThan(Date date);

    /**
     * Only select jobs where the duedate is higher then the given date.
     */
    ExternalWorkerJobQuery duedateHigherThan(Date date);

    /**
     * Only select jobs that failed due to an exception.
     */
    ExternalWorkerJobQuery withException();

    /**
     * Only select jobs that failed due to an exception with the given message.
     */
    ExternalWorkerJobQuery exceptionMessage(String exceptionMessage);

    /**
     * Only select jobs that have the given tenant id.
     */
    ExternalWorkerJobQuery jobTenantId(String tenantId);

    /**
     * Only select jobs with a tenant id like the given one.
     */
    ExternalWorkerJobQuery jobTenantIdLike(String tenantIdLike);

    /**
     * Only select jobs that do not have a tenant id.
     */
    ExternalWorkerJobQuery jobWithoutTenantId();

    /**
     * Only select jobs for the given user or groups.
     */
    ExternalWorkerJobQuery forUserOrGroups(String userId, Collection<String> groups);

    /**
     * Only return jobs with the given lock owner.
     */
    ExternalWorkerJobQuery lockOwner(String lockOwner);

    /**
     * Only return jobs that are locked (i.e. they are acquired by an executor).
     */
    ExternalWorkerJobQuery locked();

    /**
     * Only return jobs that are not locked.
     */
    ExternalWorkerJobQuery unlocked();

    // sorting //////////////////////////////////////////

    /**
     * Order by job id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ExternalWorkerJobQuery orderByJobId();

    /**
     * Order by duedate (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ExternalWorkerJobQuery orderByJobDuedate();

    /**
     * Order by create time (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ExternalWorkerJobQuery orderByJobCreateTime();

    /**
     * Order by retries (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ExternalWorkerJobQuery orderByJobRetries();

    /**
     * Order by process instance id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ExternalWorkerJobQuery orderByProcessInstanceId();

    /**
     * Order by execution id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ExternalWorkerJobQuery orderByExecutionId();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ExternalWorkerJobQuery orderByTenantId();

}
