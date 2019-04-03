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

import java.util.Date;

/**
 * Represent an event subscription for a process definition or a process instance.
 * 
 * @author Tijs Rademakers
 */
public interface EventSubscription {

    /**
     * The unique identifier of the execution.
     */
    String getId();

    /**
     * Returns the type of subscription, for example signal or message.
     */
    String getEventType();

    /**
     * The event name for the signal or message event.
     */
    String getEventName();

    /**
     * Gets the id of the execution for this event subscription.
     */
    String getExecutionId();

    /**
     * Gets the activity id of the BPMN definition where this event subscription is defined.
     */
    String getActivityId();

    /**
     * Id of the process instance for this event subscription.
     */
    String getProcessInstanceId();

    /**
     * Id of the process definition for this event subscription.
     */
    String getProcessDefinitionId();
    
    /**
     * Id of the sub scope for this event subscription.
     */
    String getSubScopeId();
    
    /**
     * Id of the scope for this event subscription.
     */
    String getScopeId();
    
    /**
     * Id of the scope definition for this event subscription.
     */
    String getScopeDefinitionId();
    
    /**
     * Scope type for this event subscription.
     */
    String getScopeType();

    /**
     * Returns the configuration with additional info about this event subscription.
     */
    String getConfiguration();

    /**
     * Gets the date/time when this event subscription was created.
     */
    Date getCreated();

    /**
     * The tenant identifier of this process instance
     */
    String getTenantId();
}
