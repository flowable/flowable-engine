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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.EventInstanceCmmnUtil;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.SendEventServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.runtime.EventPayloadInstance;
import org.flowable.eventregistry.impl.runtime.EventInstanceImpl;
import org.flowable.eventregistry.model.ChannelModel;
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

        EventModel eventModel = getEventModel(planItemInstanceEntity, key);
        boolean sendOnSystemChannel = isSendOnSystemChannel(planItemInstanceEntity);
        List<ChannelModel> channelModels = getChannelModels(commandContext, planItemInstanceEntity, sendOnSystemChannel);

        Collection<EventPayloadInstance> eventPayloadInstances = EventInstanceCmmnUtil
            .createEventPayloadInstances(planItemInstanceEntity, CommandContextUtil.getExpressionManager(commandContext), serviceTask, eventModel);
        EventInstanceImpl eventInstance = new EventInstanceImpl(eventModel.getKey(), eventPayloadInstances, planItemInstanceEntity.getTenantId());

        if (!channelModels.isEmpty()) {
            eventRegistry.sendEventOutbound(eventInstance, channelModels);
        }

        if (sendOnSystemChannel) {
            eventRegistry.sendSystemEventOutbound(eventInstance);
        }

        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
    }

    protected EventModel getEventModel(PlanItemInstanceEntity planItemInstanceEntity, String key) {
        EventModel eventModel = null;
        if (Objects.equals(CmmnEngineConfiguration.NO_TENANT_ID, planItemInstanceEntity.getTenantId())) {
            eventModel = CommandContextUtil.getEventRepositoryService().getEventModelByKey(key);
        } else {
            eventModel = CommandContextUtil.getEventRepositoryService().getEventModelByKey(key, planItemInstanceEntity.getTenantId());
        }

        if (eventModel == null) {
            throw new FlowableException("No event model found for event key " + key);
        }
        return eventModel;
    }

    protected boolean isSendOnSystemChannel(PlanItemInstanceEntity planItemInstanceEntity) {
        List<ExtensionElement> systemChannels = planItemInstanceEntity.getPlanItemDefinition().getExtensionElements()
                .getOrDefault("systemChannel", Collections.emptyList());
        return !systemChannels.isEmpty();
    }

    protected List<ChannelModel> getChannelModels(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, boolean sendOnSystemChannel) {
        List<String> channelKeys = new ArrayList<>();

        Map<String, List<ExtensionElement>> extensionElements = planItemInstanceEntity.getPlanItem().getPlanItemDefinition().getExtensionElements();
        if (extensionElements != null) {
            List<ExtensionElement> channelKeyElements = extensionElements.get("channelKey");
            if (channelKeyElements != null && !channelKeyElements.isEmpty()) {
                String channelKey = channelKeyElements.get(0).getElementText();
                if (StringUtils.isNotEmpty(channelKey)) {
                    ExpressionManager expressionManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getExpressionManager();
                    Expression expression = expressionManager.createExpression(channelKey);
                    Object resolvedChannelKey = expression.getValue(planItemInstanceEntity);
                    if (resolvedChannelKey instanceof Collection) {
                        for (Object next : (Collection) resolvedChannelKey) {
                            if (next instanceof String) {
                                String[] keys = ((String) next).split(",");
                                channelKeys.addAll(Arrays.asList(keys));

                            } else {
                                throw new FlowableIllegalArgumentException("Can only use a collection of String elements for referencing channel model key");

                            }
                        }

                    } else if (resolvedChannelKey instanceof String) {
                        String[] keys = ((String) resolvedChannelKey).split(",");
                        channelKeys.addAll(Arrays.asList(keys));

                    }
                }
            }
        }

        if (channelKeys.isEmpty()) {
            if (!sendOnSystemChannel) {
                // If the event is going to be send on the system channel then it is allowed to not define any other channels
                throw new FlowableException("No channel keys configured");
            } else {
                return Collections.emptyList();
            }
        }

        EventRepositoryService eventRepositoryService = CommandContextUtil.getEventRegistryEngineConfiguration(commandContext).getEventRepositoryService();
        List<ChannelModel> channelModels = new ArrayList<>(channelKeys.size());
        for (String channelKey : channelKeys) {
            if (Objects.equals(CmmnEngineConfiguration.NO_TENANT_ID, planItemInstanceEntity.getTenantId())) {
                channelModels.add(eventRepositoryService.getChannelModelByKey(channelKey));
            } else {
                channelModels.add(eventRepositoryService.getChannelModelByKey(channelKey, planItemInstanceEntity.getTenantId()));
            }
        }

        return channelModels;
    }

    protected String getEventKey() {
        if (StringUtils.isNotEmpty(serviceTask.getEventType())) {
            return serviceTask.getEventType();
        } else {
            throw new FlowableException("No event key configured for " + serviceTask.getId());
        }
    }

}
