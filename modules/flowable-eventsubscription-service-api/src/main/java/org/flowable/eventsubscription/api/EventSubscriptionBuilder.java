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

import org.flowable.bpmn.model.Signal;

public interface EventSubscriptionBuilder {
    
    EventSubscriptionBuilder eventType(String eventType);
    
    EventSubscriptionBuilder eventName(String eventName);
    
    EventSubscriptionBuilder signal(Signal signal);

    EventSubscriptionBuilder executionId(String executionId);

    EventSubscriptionBuilder processInstanceId(String processInstanceId);
    
    EventSubscriptionBuilder processDefinitionId(String processDefinitionId);

    EventSubscriptionBuilder activityId(String activityId);
    
    EventSubscriptionBuilder subScopeId(String subScopeId);

    EventSubscriptionBuilder scopeId(String scopeId);
    
    EventSubscriptionBuilder scopeDefinitionId(String scopeDefinitionId);

    EventSubscriptionBuilder scopeType(String scopeType);

    EventSubscriptionBuilder tenantId(String tenantId);

    EventSubscriptionBuilder configuration(String configuration);

    EventSubscription create();
    
    String getEventType();
    
    String getEventName();
    
    Signal getSignal();
    
    String getExecutionId();
    
    String getProcessInstanceId();
    
    String getProcessDefinitionId();
    
    String getActivityId();
    
    String getSubScopeId();
    
    String getScopeId();
    
    String getScopeDefinitionId();
    
    String getScopeType();

    String getTenantId();

    String getConfiguration();
}
