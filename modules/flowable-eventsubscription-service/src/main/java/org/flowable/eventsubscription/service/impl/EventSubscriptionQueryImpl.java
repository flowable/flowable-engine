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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.QueryCacheValues;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;
import org.flowable.eventsubscription.service.impl.util.CommandContextUtil;

/**
 * @author Daniel Meyer
 */
public class EventSubscriptionQueryImpl extends AbstractQuery<EventSubscriptionQuery, EventSubscription> implements EventSubscriptionQuery, QueryCacheValues {

    private static final long serialVersionUID = 1L;

    protected String id;
    protected String eventType;
    protected String eventName;
    protected String executionId;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String activityId;
    protected String subScopeId;
    protected String scopeId;
    protected String scopeDefinitionId;
    protected String scopeType;
    protected Date createdBefore;
    protected Date createdAfter;
    protected String tenantId;
    protected Collection<String> tenantIds;
    protected boolean withoutTenantId;
    protected String configuration;
    protected Collection<String> configurations;
    protected boolean withoutConfiguration;

    protected List<EventSubscriptionQueryImpl> orQueryObjects = new ArrayList<>();
    protected EventSubscriptionQueryImpl currentOrQueryObject;
    protected boolean inOrStatement;

    public EventSubscriptionQueryImpl() {

    }

    public EventSubscriptionQueryImpl(CommandContext commandContext) {
        super(commandContext);
    }

    public EventSubscriptionQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public EventSubscriptionQueryImpl id(String id) {
        if (id == null) {
            throw new FlowableIllegalArgumentException("Provided event subscription id is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.id = id;
        } else {
            this.id = id;
        }

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl eventType(String eventType) {
        if (eventType == null) {
            throw new FlowableIllegalArgumentException("Provided event type is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.eventType = eventType;
        } else {
            this.eventType = eventType;
        }

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl eventName(String eventName) {
        if (eventName == null) {
            throw new FlowableIllegalArgumentException("Provided event name is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.eventName = eventName;
        } else {
            this.eventName = eventName;
        }

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl executionId(String executionId) {
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("Provided execution id is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.executionId = executionId;
        } else {
            this.executionId = executionId;
        }

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided process instance id is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processInstanceId = processInstanceId;
        } else {
            this.processInstanceId = processInstanceId;
        }

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl processDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided process definition id is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.processDefinitionId = processDefinitionId;
        } else {
            this.processDefinitionId = processDefinitionId;
        }

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl activityId(String activityId) {
        if (activityId == null) {
            throw new FlowableIllegalArgumentException("Provided activity id is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.activityId = activityId;
        } else {
            this.activityId = activityId;
        }

        return this;
    }
    
    @Override
    public EventSubscriptionQueryImpl subScopeId(String subScopeId) {
        if (scopeId == null) {
            throw new FlowableIllegalArgumentException("Provided sub scope id is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.subScopeId = subScopeId;
        } else {
            this.subScopeId = subScopeId;
        }

        return this;
    }
    
    @Override
    public EventSubscriptionQueryImpl scopeId(String scopeId) {
        if (scopeId == null) {
            throw new FlowableIllegalArgumentException("Provided scope id is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.scopeId = scopeId;
        } else {
            this.scopeId = scopeId;
        }

        return this;
    }
    
    @Override
    public EventSubscriptionQueryImpl scopeDefinitionId(String scopeDefinitionId) {
        if (scopeDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided scope definition id is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.scopeDefinitionId = scopeDefinitionId;
        } else {
            this.scopeDefinitionId = scopeDefinitionId;
        }

        return this;
    }
    
    @Override
    public EventSubscriptionQueryImpl scopeType(String scopeType) {
        if (scopeType == null) {
            throw new FlowableIllegalArgumentException("Provided scope type is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.scopeType = scopeType;
        } else {
            this.scopeType = scopeType;
        }

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl createdBefore(Date beforeTime) {
        if (beforeTime == null) {
            throw new FlowableIllegalArgumentException("created before time is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.createdBefore = createdBefore;
        } else {
            this.createdBefore = createdBefore;
        }

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl createdAfter(Date afterTime) {
        if (afterTime == null) {
            throw new FlowableIllegalArgumentException("created after time is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.createdAfter = createdAfter;
        } else {
            this.createdAfter = createdAfter;
        }

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl tenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("tenant id is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.tenantId = tenantId;
        } else {
            this.tenantId = tenantId;
        }

        return this;
    }

    @Override
    public EventSubscriptionQuery tenantIds(Collection<String> tenantIds) {
        if (tenantIds == null) {
            throw new FlowableIllegalArgumentException("tenant ids is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.tenantIds = tenantIds;
        } else {
            this.tenantIds = tenantIds;
        }

        return this;
    }

    @Override
    public EventSubscriptionQuery withoutTenantId() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutTenantId = true;
        } else {
            this.withoutTenantId = true;
        }
        return this;
    }

    @Override
    public EventSubscriptionQueryImpl configuration(String configuration) {
        if (configuration == null) {
            throw new FlowableIllegalArgumentException("configuration is null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.configuration = configuration;
        } else {
            this.configuration = configuration;
        }

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl configurations(Collection<String> configurations) {
        if (configurations == null) {
            throw new FlowableIllegalArgumentException("configurations are null");
        }

        if (inOrStatement) {
            this.currentOrQueryObject.configurations = configurations;
        } else {
            this.configurations = configurations;
        }

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl withoutConfiguration() {
        if (inOrStatement) {
            this.currentOrQueryObject.withoutConfiguration = true;
        } else {
            this.withoutConfiguration = true;
        }
        return this;
    }

    @Override
    public EventSubscriptionQuery or() {
        if (inOrStatement) {
            throw new FlowableException("the query is already in an or statement");
        }

        inOrStatement = true;
        currentOrQueryObject = new EventSubscriptionQueryImpl();
        orQueryObjects.add(currentOrQueryObject);
        return this;
    }

    @Override
    public EventSubscriptionQuery endOr() {
        if (!inOrStatement) {
            throw new FlowableException("endOr() can only be called after calling or()");
        }

        inOrStatement = false;
        currentOrQueryObject = null;
        return this;
    }

    @Override
    public EventSubscriptionQuery orderById() {
        return orderBy(EventSubscriptionQueryProperty.ID);
    }

    @Override
    public EventSubscriptionQuery orderByExecutionId() {
        return orderBy(EventSubscriptionQueryProperty.EXECUTION_ID);
    }

    @Override
    public EventSubscriptionQuery orderByProcessInstanceId() {
        return orderBy(EventSubscriptionQueryProperty.PROCESS_INSTANCE_ID);
    }

    @Override
    public EventSubscriptionQuery orderByProcessDefinitionId() {
        return orderBy(EventSubscriptionQueryProperty.PROCESS_DEFINITION_ID);
    }

    @Override
    public EventSubscriptionQuery orderByCreateDate() {
        return orderBy(EventSubscriptionQueryProperty.CREATED);
    }

    @Override
    public EventSubscriptionQuery orderByTenantId() {
        return orderBy(EventSubscriptionQueryProperty.TENANT_ID);
    }

    // results //////////////////////////////////////////

    @Override
    public long executeCount(CommandContext commandContext) {
        return CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionCountByQueryCriteria(this);
    }

    @Override
    public List<EventSubscription> executeList(CommandContext commandContext) {
        return CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionsByQueryCriteria(this);
    }

    // getters //////////////////////////////////////////

    @Override
    public String getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventName() {
        return eventName;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getActivityId() {
        return activityId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getSubScopeId() {
        return subScopeId;
    }

    public String getScopeId() {
        return scopeId;
    }

    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public Date getCreatedBefore() {
        return createdBefore;
    }

    public Date getCreatedAfter() {
        return createdAfter;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Collection<String> getTenantIds() {
        return tenantIds;
    }

    public boolean isWithoutTenantId() {
        return withoutTenantId;
    }

    public String getConfiguration() {
        return configuration;
    }

    public Collection<String> getConfigurations() {
        return configurations;
    }

    public boolean isWithoutConfiguration() {
        return withoutConfiguration;
    }

    public List<EventSubscriptionQueryImpl> getOrQueryObjects() {
        return orQueryObjects;
    }

    public EventSubscriptionQueryImpl getCurrentOrQueryObject() {
        return currentOrQueryObject;
    }

    public boolean isInOrStatement() {
        return inOrStatement;
    }

}
