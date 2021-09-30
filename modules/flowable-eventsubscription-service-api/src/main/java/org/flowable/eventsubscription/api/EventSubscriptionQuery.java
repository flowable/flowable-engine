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
package org.flowable.eventsubscription.api;

import java.util.Collection;
import java.util.Date;

import org.flowable.common.engine.api.query.Query;

/**
 * Allows programmatic querying of {@link EventSubscription}s.
 * 
 * @author Tijs Rademakers
 */
public interface EventSubscriptionQuery extends Query<EventSubscriptionQuery, EventSubscription> {

    /** Only select event subscriptions with the given id. **/
    EventSubscriptionQuery id(String id);

    /** Only select event subscriptions with the given type. **/
    EventSubscriptionQuery eventType(String eventType);

    /** Only select event subscriptions with the given name. **/
    EventSubscriptionQuery eventName(String eventName);

    /** Only select event subscriptions with the given execution id. **/
    EventSubscriptionQuery executionId(String executionId);

    /** Only select event subscriptions which have the given process instance id. **/
    EventSubscriptionQuery processInstanceId(String processInstanceId);
    
    /** Only select event subscriptions without a process instance id value. **/
    EventSubscriptionQuery withoutProcessInstanceId();

    /** Only select event subscriptions which have the given process definition id. **/
    EventSubscriptionQuery processDefinitionId(String processDefinitionId);
    
    /** Only select event subscriptions without a process definition id value. **/
    EventSubscriptionQuery withoutProcessDefinitionId();

    /** Only select event subscriptions which have an activity with the given id. **/
    EventSubscriptionQuery activityId(String activityId);
    
    /** Only select event subscriptions which have the given case instance id. **/
    EventSubscriptionQuery caseInstanceId(String caseInstanceId);
    
    /** Only select event subscriptions which have the given plan item instance id. **/
    EventSubscriptionQuery planItemInstanceId(String planItemInstanceId);
    
    /** Only select event subscriptions which have the given case definition id. **/
    EventSubscriptionQuery caseDefinitionId(String caseDefinitionId);
    
    /** Only select event subscriptions which have a sub scope id with the given value. **/
    EventSubscriptionQuery subScopeId(String subScopeId);
    
    /** Only select event subscriptions which have a scope id with the given value. **/
    EventSubscriptionQuery scopeId(String scopeId);
    
    /** Only select event subscriptions without a scope id value. **/
    EventSubscriptionQuery withoutScopeId();
    
    /** Only select event subscriptions which have a scope definition id with the given value. **/
    EventSubscriptionQuery scopeDefinitionId(String scopeDefinitionId);
    
    /** Only select event subscriptions without a scope definition id value. **/
    EventSubscriptionQuery withoutScopeDefinitionId();
    
    /** Only select event subscriptions which have a scope type with the given value. **/
    EventSubscriptionQuery scopeType(String scopeType);

    /** Only select event subscriptions that were created before the given start time. **/
    EventSubscriptionQuery createdBefore(Date beforeTime);

    /** Only select event subscriptions that were created after the given start time. **/
    EventSubscriptionQuery createdAfter(Date afterTime);

    /** Only select event subscriptions with the given tenant id. **/
    EventSubscriptionQuery tenantId(String tenantId);

    /** Only select event subscriptions with the given tenant id. **/
    EventSubscriptionQuery tenantIds(Collection<String> tenantIds);

    /** Only select event subscriptions without a tenant id. */
    EventSubscriptionQuery withoutTenantId();

    /** Only select event subscriptions with the given configuration. **/
    EventSubscriptionQuery configuration(String configuration);

    /** Only select event subscriptions with the given configurations. **/
    EventSubscriptionQuery configurations(Collection<String> configurations);

    /** Only select event subscriptions that have no configuration. **/
    EventSubscriptionQuery withoutConfiguration();

    /**
     * Begin an OR statement. Make sure you invoke the endOr() method at the end of your OR statement.
     */
    EventSubscriptionQuery or();

    /**
     * End an OR statement.
     */
    EventSubscriptionQuery endOr();

    // ordering //////////////////////////////////////////////////////////////

    /** Order by id (needs to be followed by {@link #asc()} or {@link #desc()}). */
    EventSubscriptionQuery orderById();

    /** Order by execution id (needs to be followed by {@link #asc()} or {@link #desc()}). */
    EventSubscriptionQuery orderByExecutionId();

    /** Order by process instance id (needs to be followed by {@link #asc()} or {@link #desc()}). */
    EventSubscriptionQuery orderByProcessInstanceId();

    /**
     * Order by process definition id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventSubscriptionQuery orderByProcessDefinitionId();

    /**
     * Order by create date (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventSubscriptionQuery orderByCreateDate();

    /**
     * Order by event name (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventSubscriptionQuery orderByEventName();

    /**
     * Order by tenant id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    EventSubscriptionQuery orderByTenantId();

}
