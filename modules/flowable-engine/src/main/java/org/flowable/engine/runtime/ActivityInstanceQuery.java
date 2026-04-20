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

package org.flowable.engine.runtime;

import java.util.Set;

import org.flowable.common.engine.api.query.Query;
/**
 * Programmatic querying for {@link ActivityInstance}s.
 * 
 * @author martin.grofcik
 */
public interface ActivityInstanceQuery extends Query<ActivityInstanceQuery, ActivityInstance> {

    /**
     * Only select activity instances with the given id (primary key within history tables).
     */
    ActivityInstanceQuery activityInstanceId(String activityInstanceId);

    /**
     * Only select activity instances with the given process instance. {@link ProcessInstance} ids and {@link ActivityInstance#getProcessInstanceId()} ids match.
     */
    ActivityInstanceQuery processInstanceId(String processInstanceId);

    /**
     * Only select activity instances with the given process instance ids. {@link ProcessInstance} ids and {@link ActivityInstance#getProcessInstanceId()} ids match.
     */
    ActivityInstanceQuery processInstanceIds(Set<String> processInstanceIds);

    /** Only select activity instances for the given process definition */
    ActivityInstanceQuery processDefinitionId(String processDefinitionId);

    /** Only select activity instances for the given execution */
    ActivityInstanceQuery executionId(String executionId);

    /**
     * Only select activity instances for the given activity (id from BPMN 2.0 XML)
     */
    ActivityInstanceQuery activityId(String activityId);

    /**
     * Only select activity instances for activities with the given name
     */
    ActivityInstanceQuery activityName(String activityName);

    /**
     * Only select activity instances for activities with the given activity type
     */
    ActivityInstanceQuery activityType(String activityType);

    /**
     * Only select activity instances for userTask activities assigned to the given user
     */
    ActivityInstanceQuery taskAssignee(String userId);

    /**
     * Only select activity instances for userTask activities completed by the given user
     */
    ActivityInstanceQuery taskCompletedBy(String userId);

    /** Only select activity instances that are finished. */
    ActivityInstanceQuery finished();

    /** Only select activity instances that are not finished yet. */
    ActivityInstanceQuery unfinished();

    /** Only select activity instances with a specific delete reason. */
    ActivityInstanceQuery deleteReason(String deleteReason);

    /** Only select activity instances with a delete reason that matches the provided parameter. */
    ActivityInstanceQuery deleteReasonLike(String deleteReasonLike);

    /** Only select activity instances that have the given tenant id. */
    ActivityInstanceQuery activityTenantId(String tenantId);

    /**
     * Only select activity instances with a tenant id like the given one.
     */
    ActivityInstanceQuery activityTenantIdLike(String tenantIdLike);

    /** Only select activity instances that do not have a tenant id. */
    ActivityInstanceQuery activityWithoutTenantId();

    // ordering
    // /////////////////////////////////////////////////////////////////
    /** Order by id (needs to be followed by {@link #asc()} or {@link #desc()}). */
    ActivityInstanceQuery orderByActivityInstanceId();

    /**
     * Order by processInstanceId (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ActivityInstanceQuery orderByProcessInstanceId();

    /**
     * Order by executionId (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ActivityInstanceQuery orderByExecutionId();

    /**
     * Order by activityId (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ActivityInstanceQuery orderByActivityId();

    /**
     * Order by activityName (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ActivityInstanceQuery orderByActivityName();

    /**
     * Order by activityType (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ActivityInstanceQuery orderByActivityType();

    /**
     * Order by start (needs to be followed by {@link #asc()} or {@link #desc()} ).
     */
    ActivityInstanceQuery orderByActivityInstanceStartTime();

    /**
     * Order by end (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ActivityInstanceQuery orderByActivityInstanceEndTime();

    /**
     * Order by duration (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ActivityInstanceQuery orderByActivityInstanceDuration();

    /**
     * Order by processDefinitionId (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ActivityInstanceQuery orderByProcessDefinitionId();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    ActivityInstanceQuery orderByTenantId();

}
