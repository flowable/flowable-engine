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
package org.flowable.eventsubscription.service.impl;

import org.flowable.bpmn.model.Signal;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionBuilder;

public class EventSubscriptionBuilderImpl implements EventSubscriptionBuilder {

    protected EventSubscriptionServiceImpl eventSubscriptionService;

    protected String eventType;
    protected String eventName;
    protected Signal signal;
    protected String executionId;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String activityId;
    protected String subScopeId;
    protected String scopeId;
    protected String scopeDefinitionId;
    protected String scopeType;
    protected String tenantId;
    protected String configuration;

    public EventSubscriptionBuilderImpl() {
        
    }
    
    public EventSubscriptionBuilderImpl(EventSubscriptionServiceImpl eventSubscriptionService) {
        this.eventSubscriptionService = eventSubscriptionService;
    }
    
    @Override
    public EventSubscriptionBuilder eventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    @Override
    public EventSubscriptionBuilder eventName(String eventName) {
        this.eventName = eventName;
        return this;
    }
    
    @Override
    public EventSubscriptionBuilder signal(Signal signal) {
        this.signal = signal;
        return this;
    }

    @Override
    public EventSubscriptionBuilder executionId(String executionId) {
        this.executionId = executionId;
        return this;
    }

    @Override
    public EventSubscriptionBuilder processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public EventSubscriptionBuilder processDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public EventSubscriptionBuilder activityId(String activityId) {
        this.activityId = activityId;
        return this;
    }

    @Override
    public EventSubscriptionBuilder subScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
        return this;
    }

    @Override
    public EventSubscriptionBuilder scopeId(String scopeId) {
        this.scopeId = scopeId;
        return this;
    }

    @Override
    public EventSubscriptionBuilder scopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
        return this;
    }

    @Override
    public EventSubscriptionBuilder scopeType(String scopeType) {
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public EventSubscriptionBuilder tenantId(String tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    @Override
    public EventSubscriptionBuilder configuration(String configuration) {
        this.configuration = configuration;
        return this;
    }

    @Override
    public EventSubscription create() {
        return eventSubscriptionService.createEventSubscription(this);
    }
    
    @Override
    public String getEventType() {
        return eventType;
    }

    @Override
    public String getEventName() {
        return eventName;
    }
    
    @Override
    public Signal getSignal() {
        return signal;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public String getActivityId() {
        return activityId;
    }

    @Override
    public String getSubScopeId() {
        return subScopeId;
    }

    @Override
    public String getScopeId() {
        return scopeId;
    }

    @Override
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    @Override
    public String getScopeType() {
        return scopeType;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String getConfiguration() {
        return configuration;
    }
}
