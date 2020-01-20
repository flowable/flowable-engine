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
package org.flowable.cmmn.engine.impl.behavior.impl;

import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.EventInstanceCmmnUtil;
import org.flowable.cmmn.model.SendEventServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
import org.flowable.eventregistry.model.EventModel;

/**
 * @author Joram Barrez
 */
public class SendEventActivityBehavior extends TaskActivityBehavior{

    protected SendEventServiceTask serviceTask;

    public SendEventActivityBehavior(SendEventServiceTask serviceTask) {
        super(serviceTask.isBlocking(), serviceTask.getBlockingExpression());
        this.serviceTask = serviceTask;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {

        String key = getEventKey();

        EventRegistry eventRegistry = CommandContextUtil.getEventRegistry();

        EventModel eventModel = null;
        if (Objects.equals(CmmnEngineConfiguration.NO_TENANT_ID, planItemInstanceEntity.getTenantId())) {
            eventModel = CommandContextUtil.getEventRepositoryService().getEventModelByKey(key);
        } else {
            eventModel = CommandContextUtil.getEventRepositoryService().getEventModelByKey(key, planItemInstanceEntity.getTenantId());
        }

        if (eventModel == null) {
            throw new FlowableException("No event model found for event key " + key);
        }

        EventInstanceImpl eventInstance = new EventInstanceImpl();
        eventInstance.setEventModel(eventModel);

        Collection<EventPayloadInstance> eventPayloadInstances = EventInstanceCmmnUtil
            .createEventPayloadInstances(planItemInstanceEntity, CommandContextUtil.getExpressionManager(commandContext), serviceTask, eventModel);
        eventInstance.setPayloadInstances(eventPayloadInstances);

        eventRegistry.sendEventOutbound(eventInstance);

        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
    }

    protected String getEventKey() {
        if (StringUtils.isNotEmpty(serviceTask.getEventType())) {
            return serviceTask.getEventType();
        } else {
            throw new FlowableException("No event key configured for " + serviceTask.getId());
        }
    }

}
