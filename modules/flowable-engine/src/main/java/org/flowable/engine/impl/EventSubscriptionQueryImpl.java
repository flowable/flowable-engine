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

package org.flowable.engine.impl;

import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.EventSubscription;
import org.flowable.engine.runtime.EventSubscriptionQuery;

/**
 * @author Daniel Meyer
 */
public class EventSubscriptionQueryImpl extends AbstractQuery<EventSubscriptionQuery, EventSubscription> implements EventSubscriptionQuery {

    private static final long serialVersionUID = 1L;

    protected String id;
    protected String eventType;
    protected String eventName;
    protected String executionId;
    protected String processInstanceId;
    protected String processDefinitionId;
    protected String activityId;
    protected Date createdBefore;
    protected Date createdAfter;
    protected String tenantId;

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
        this.id = id;
        return this;
    }

    @Override
    public EventSubscriptionQueryImpl eventType(String eventType) {
        if (eventType == null) {
            throw new FlowableIllegalArgumentException("Provided event type is null");
        }
        this.eventType = eventType;
        return this;
    }

    @Override
    public EventSubscriptionQueryImpl eventName(String eventName) {
        if (eventName == null) {
            throw new FlowableIllegalArgumentException("Provided event name is null");
        }
        this.eventName = eventName;
        return this;
    }

    @Override
    public EventSubscriptionQueryImpl executionId(String executionId) {
        if (executionId == null) {
            throw new FlowableIllegalArgumentException("Provided execution id is null");
        }
        this.executionId = executionId;
        return this;
    }

    @Override
    public EventSubscriptionQueryImpl processInstanceId(String processInstanceId) {
        if (processInstanceId == null) {
            throw new FlowableIllegalArgumentException("Provided process instance id is null");
        }
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public EventSubscriptionQueryImpl processDefinitionId(String processDefinitionId) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Provided process definition id is null");
        }
        this.processDefinitionId = processDefinitionId;
        return this;
    }

    @Override
    public EventSubscriptionQueryImpl activityId(String activityId) {
        if (activityId == null) {
            throw new FlowableIllegalArgumentException("Provided activity id is null");
        }
        this.activityId = activityId;
        return this;
    }

    @Override
    public EventSubscriptionQueryImpl createdBefore(Date beforeTime) {
        if (beforeTime == null) {
            throw new FlowableIllegalArgumentException("created before time is null");
        }
        this.createdBefore = beforeTime;

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl createdAfter(Date afterTime) {
        if (afterTime == null) {
            throw new FlowableIllegalArgumentException("created after time is null");
        }
        this.createdAfter = afterTime;

        return this;
    }

    @Override
    public EventSubscriptionQueryImpl tenantId(String tenantId) {
        if (tenantId == null) {
            throw new FlowableIllegalArgumentException("tenant id is null");
        }
        this.tenantId = tenantId;
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
        checkQueryOk();
        return CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionCountByQueryCriteria(this);
    }

    @Override
    public List<EventSubscription> executeList(CommandContext commandContext) {
        checkQueryOk();
        return CommandContextUtil.getEventSubscriptionEntityManager(commandContext).findEventSubscriptionsByQueryCriteria(this);
    }

    // getters //////////////////////////////////////////

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

    public Date getCreatedBefore() {
        return createdBefore;
    }

    public Date getCreatedAfter() {
        return createdAfter;
    }

    public String getTenantId() {
        return tenantId;
    }

}
