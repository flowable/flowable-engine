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
 * Allows programmatic querying of {@link Job}s.
 * Summarizes the base query params for all JobQuery classes except {@link HistoryJobQuery}
 *
 * @author Christopher Welsch
 */
public interface BaseJobQuery<U extends BaseJobQuery<U, T>, T extends Job> extends Query<U, T> {

    /**
     * Only select jobs with the given id
     */
    U jobId(String jobId);

    /**
     * Only select jobs which exist for the given process instance.
     **/
    U processInstanceId(String processInstanceId);

    /**
     * Only select jobs without a process instance id value.
     **/
    U withoutProcessInstanceId();

    /**
     * Only select jobs which exist for the given execution
     */
    U executionId(String executionId);

    /**
     * Select jobs which have given job handler type
     */
    U handlerType(String handlerType);

    /**
     * Select jobs which have one of the given job handler type
     */
    U handlerTypes(Collection<String> handlerTypes);

    /**
     * Only select jobs which exist for the given process definition id
     */
    U processDefinitionId(String processDefinitionId);

    /**
     * Only select jobs which exist for the given category
     */
    U category(String category);

    /**
     * Only select jobs like for the given category value
     */
    U categoryLike(String categoryLike);

    /**
     * Only select jobs which exist for the given element id
     */
    U elementId(String elementId);

    /**
     * Only select jobs which exist for the given element name
     */
    U elementName(String elementName);

    /**
     * Only select tasks for the given scope identifier.
     */
    U scopeId(String scopeId);

    /**
     * Only select jobs without a scope id value.
     **/
    U withoutScopeId();

    /**
     * Only select tasks for the given sub scope identifier.
     */
    U subScopeId(String subScopeId);

    /**
     * Only select tasks for the given scope type.
     */
    U scopeType(String scopeType);

    /**
     * Only return jobs that do not have a scope type.
     */
    U withoutScopeType();

    /**
     * Only select tasks for the given scope definition identifier.
     */
    U scopeDefinitionId(String scopeDefinitionId);

    /**
     * Only select jobs for the given case instance.
     */
    U caseInstanceId(String caseInstanceId);

    /**
     * Only select jobs for the given case definition.
     */
    U caseDefinitionId(String caseDefinitionId);

    /**
     * Only select jobs for the given plan item instance.
     */
    U planItemInstanceId(String planItemInstanceId);

    /**
     * Only select jobs with the given correlationId.
     */
    U correlationId(String correlationId);

    /**
     * Only select jobs where the duedate is lower than the given date.
     */
    U duedateLowerThan(Date date);

    /**
     * Only select jobs where the duedate is higher then the given date.
     */
    U duedateHigherThan(Date date);

    /**
     * Only select jobs that failed due to an exception.
     */
    U withException();

    /**
     * Only select jobs that failed due to an exception with the given message.
     */
    U exceptionMessage(String exceptionMessage);

    /**
     * Only select jobs that have the given tenant id.
     */
    U jobTenantId(String tenantId);

    /**
     * Only select jobs with a tenant id like the given one.
     */
    U jobTenantIdLike(String tenantIdLike);

    /**
     * Only select jobs that do not have a tenant id.
     */
    U jobWithoutTenantId();

    // sorting //////////////////////////////////////////

    /**
     * Order by job id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    U orderByJobId();

    /**
     * Order by duedate (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    U orderByJobDuedate();

    /**
     * Order by create time (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    U orderByJobCreateTime();

    /**
     * Order by retries (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    U orderByJobRetries();

    /**
     * Order by process instance id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    U orderByProcessInstanceId();

    /**
     * Order by execution id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    U orderByExecutionId();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    U orderByTenantId();
}
