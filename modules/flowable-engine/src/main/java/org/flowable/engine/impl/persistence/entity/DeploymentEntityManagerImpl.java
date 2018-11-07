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

package org.flowable.engine.impl.persistence.entity;

import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.Message;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.Signal;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.DeploymentQueryImpl;
import org.flowable.engine.impl.ModelQueryImpl;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.flowable.engine.impl.persistence.entity.data.DeploymentDataManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.util.TimerUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class DeploymentEntityManagerImpl extends AbstractEntityManager<DeploymentEntity> implements DeploymentEntityManager {

    protected DeploymentDataManager deploymentDataManager;

    public DeploymentEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, DeploymentDataManager deploymentDataManager) {
        super(processEngineConfiguration);
        this.deploymentDataManager = deploymentDataManager;
    }

    @Override
    protected DataManager<DeploymentEntity> getDataManager() {
        return deploymentDataManager;
    }

    @Override
    public void insert(DeploymentEntity deployment) {
        insert(deployment, false);

        for (EngineResource resource : deployment.getResources().values()) {
            resource.setDeploymentId(deployment.getId());
            getResourceEntityManager().insert((ResourceEntity) resource);
        }
    }

    @Override
    public void deleteDeployment(String deploymentId, boolean cascade) {
        List<ProcessDefinition> processDefinitions = new ProcessDefinitionQueryImpl().deploymentId(deploymentId).list();

        updateRelatedModels(deploymentId);

        if (cascade) {
            deleteProcessInstancesForProcessDefinitions(processDefinitions);
        }

        for (ProcessDefinition processDefinition : processDefinitions) {
            deleteProcessDefinitionIdentityLinks(processDefinition);
            deleteEventSubscriptions(processDefinition);
            deleteProcessDefinitionInfo(processDefinition.getId());

            removeTimerStartJobs(processDefinition);

            // If previous process definition version has a timer/signal/message start event, it must be added
            // Only if the currently deleted process definition is the latest version,
            // we fall back to the previous timer/signal/message start event

            restorePreviousStartEventsIfNeeded(processDefinition);
        }

        deleteProcessDefinitionForDeployment(deploymentId);
        getResourceEntityManager().deleteResourcesByDeploymentId(deploymentId);
        delete(findById(deploymentId), false);
    }

    protected void updateRelatedModels(String deploymentId) {
        // Remove the deployment link from any model.
        // The model will still exists, as a model is a source for a deployment model and has a different lifecycle
        List<Model> models = new ModelQueryImpl().deploymentId(deploymentId).list();
        for (Model model : models) {
            ModelEntity modelEntity = (ModelEntity) model;
            modelEntity.setDeploymentId(null);
            getModelEntityManager().updateModel(modelEntity);
        }
    }

    protected void deleteProcessDefinitionIdentityLinks(ProcessDefinition processDefinition) {
        CommandContextUtil.getIdentityLinkService().deleteIdentityLinksByProcessDefinitionId(processDefinition.getId());
    }

    protected void deleteEventSubscriptions(ProcessDefinition processDefinition) {
        EventSubscriptionEntityManager eventSubscriptionEntityManager = getEventSubscriptionEntityManager();
        eventSubscriptionEntityManager.deleteEventSubscriptionsForProcessDefinition(processDefinition.getId());
    }

    protected void deleteProcessDefinitionInfo(String processDefinitionId) {
        getProcessDefinitionInfoEntityManager().deleteProcessDefinitionInfo(processDefinitionId);
    }

    protected void deleteProcessDefinitionForDeployment(String deploymentId) {
        getProcessDefinitionEntityManager().deleteProcessDefinitionsByDeploymentId(deploymentId);
    }

    protected void deleteProcessInstancesForProcessDefinitions(List<ProcessDefinition> processDefinitions) {
        for (ProcessDefinition processDefinition : processDefinitions) {
            getExecutionEntityManager().deleteProcessInstancesByProcessDefinition(processDefinition.getId(), "deleted deployment", true);
        }
    }

    protected void removeTimerStartJobs(ProcessDefinition processDefinition) {
        TimerJobService timerJobService = CommandContextUtil.getTimerJobService();
        List<TimerJobEntity> timerStartJobs = timerJobService.findJobsByTypeAndProcessDefinitionId(TimerStartEventJobHandler.TYPE, processDefinition.getId());
        if (timerStartJobs != null && timerStartJobs.size() > 0) {
            for (TimerJobEntity timerStartJob : timerStartJobs) {
                if (getEventDispatcher().isEnabled()) {
                    getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, timerStartJob, null, null, processDefinition.getId()));
                }

                timerJobService.deleteTimerJob(timerStartJob);
            }
        }
    }

    protected void restorePreviousStartEventsIfNeeded(ProcessDefinition processDefinition) {
        ProcessDefinitionEntity latestProcessDefinition = findLatestProcessDefinition(processDefinition);
        if (latestProcessDefinition != null && processDefinition.getId().equals(latestProcessDefinition.getId())) {

            // Try to find a previous version (it could be some versions are missing due to deletions)
            ProcessDefinition previousProcessDefinition = findNewLatestProcessDefinitionAfterRemovalOf(processDefinition);
            if (previousProcessDefinition != null) {

                BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(previousProcessDefinition.getId());
                org.flowable.bpmn.model.Process previousProcess = ProcessDefinitionUtil.getProcess(previousProcessDefinition.getId());
                if (CollectionUtil.isNotEmpty(previousProcess.getFlowElements())) {

                    List<StartEvent> startEvents = previousProcess.findFlowElementsOfType(StartEvent.class);

                    if (CollectionUtil.isNotEmpty(startEvents)) {
                        for (StartEvent startEvent : startEvents) {

                            if (CollectionUtil.isNotEmpty(startEvent.getEventDefinitions())) {
                                EventDefinition eventDefinition = startEvent.getEventDefinitions().get(0);
                                if (eventDefinition instanceof TimerEventDefinition) {
                                    restoreTimerStartEvent(previousProcessDefinition, startEvent, eventDefinition);
                                } else if (eventDefinition instanceof SignalEventDefinition) {
                                    restoreSignalStartEvent(previousProcessDefinition, bpmnModel, startEvent, eventDefinition);
                                } else if (eventDefinition instanceof MessageEventDefinition) {
                                    restoreMessageStartEvent(previousProcessDefinition, bpmnModel, startEvent, eventDefinition);
                                }

                            }

                        }
                    }

                }

            }
        }
    }

    protected void restoreTimerStartEvent(ProcessDefinition previousProcessDefinition, StartEvent startEvent, EventDefinition eventDefinition) {
        TimerEventDefinition timerEventDefinition = (TimerEventDefinition) eventDefinition;
        TimerJobEntity timer = TimerUtil.createTimerEntityForTimerEventDefinition((TimerEventDefinition) eventDefinition, false, null, TimerStartEventJobHandler.TYPE,
                TimerEventHandler.createConfiguration(startEvent.getId(), timerEventDefinition.getEndDate(), timerEventDefinition.getCalendarName()));

        if (timer != null) {
            TimerJobEntity timerJob = TimerUtil.createTimerEntityForTimerEventDefinition(timerEventDefinition, false, null, TimerStartEventJobHandler.TYPE, 
                            TimerEventHandler.createConfiguration(startEvent.getId(), timerEventDefinition.getEndDate(), timerEventDefinition.getCalendarName()));
            
            timerJob.setProcessDefinitionId(previousProcessDefinition.getId());

            if (previousProcessDefinition.getTenantId() != null) {
                timerJob.setTenantId(previousProcessDefinition.getTenantId());
            }

            CommandContextUtil.getTimerJobService().scheduleTimerJob(timerJob);
        }
    }

    protected void restoreSignalStartEvent(ProcessDefinition previousProcessDefinition, BpmnModel bpmnModel, StartEvent startEvent, EventDefinition eventDefinition) {
        SignalEventDefinition signalEventDefinition = (SignalEventDefinition) eventDefinition;
        SignalEventSubscriptionEntity subscriptionEntity = getEventSubscriptionEntityManager().createSignalEventSubscription();
        Signal signal = bpmnModel.getSignal(signalEventDefinition.getSignalRef());
        if (signal != null) {
            subscriptionEntity.setEventName(signal.getName());
        } else {
            subscriptionEntity.setEventName(signalEventDefinition.getSignalRef());
        }
        subscriptionEntity.setActivityId(startEvent.getId());
        subscriptionEntity.setProcessDefinitionId(previousProcessDefinition.getId());
        if (previousProcessDefinition.getTenantId() != null) {
            subscriptionEntity.setTenantId(previousProcessDefinition.getTenantId());
        }

        getEventSubscriptionEntityManager().insert(subscriptionEntity);
    }

    protected void restoreMessageStartEvent(ProcessDefinition previousProcessDefinition, BpmnModel bpmnModel, StartEvent startEvent, EventDefinition eventDefinition) {
        MessageEventDefinition messageEventDefinition = (MessageEventDefinition) eventDefinition;
        if (bpmnModel.containsMessageId(messageEventDefinition.getMessageRef())) {
            Message message = bpmnModel.getMessage(messageEventDefinition.getMessageRef());
            messageEventDefinition.setMessageRef(message.getName());
        }

        MessageEventSubscriptionEntity newSubscription = getEventSubscriptionEntityManager().createMessageEventSubscription();
        newSubscription.setEventName(messageEventDefinition.getMessageRef());
        newSubscription.setActivityId(startEvent.getId());
        newSubscription.setConfiguration(previousProcessDefinition.getId());
        newSubscription.setProcessDefinitionId(previousProcessDefinition.getId());

        if (previousProcessDefinition.getTenantId() != null) {
            newSubscription.setTenantId(previousProcessDefinition.getTenantId());
        }

        getEventSubscriptionEntityManager().insert(newSubscription);
    }

    protected ProcessDefinitionEntity findLatestProcessDefinition(ProcessDefinition processDefinition) {
        ProcessDefinitionEntity latestProcessDefinition = null;
        if (processDefinition.getTenantId() != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(processDefinition.getTenantId())) {
            latestProcessDefinition = getProcessDefinitionEntityManager()
                    .findLatestProcessDefinitionByKeyAndTenantId(processDefinition.getKey(), processDefinition.getTenantId());
        } else {
            latestProcessDefinition = getProcessDefinitionEntityManager()
                    .findLatestProcessDefinitionByKey(processDefinition.getKey());
        }
        return latestProcessDefinition;
    }

    protected ProcessDefinition findNewLatestProcessDefinitionAfterRemovalOf(ProcessDefinition processDefinitionToBeRemoved) {

        // The latest process definition is not necessarily the one with 'version -1' (some versions could have been deleted)
        // Hence, the following logic

        ProcessDefinitionQueryImpl query = new ProcessDefinitionQueryImpl();
        query.processDefinitionKey(processDefinitionToBeRemoved.getKey());

        if (processDefinitionToBeRemoved.getTenantId() != null
                && !ProcessEngineConfiguration.NO_TENANT_ID.equals(processDefinitionToBeRemoved.getTenantId())) {
            query.processDefinitionTenantId(processDefinitionToBeRemoved.getTenantId());
        } else {
            query.processDefinitionWithoutTenantId();
        }

        if (processDefinitionToBeRemoved.getVersion() > 0) {
            query.processDefinitionVersionLowerThan(processDefinitionToBeRemoved.getVersion());
        }
        query.orderByProcessDefinitionVersion().desc();

        query.setFirstResult(0);
        query.setMaxResults(1);
        List<ProcessDefinition> processDefinitions = getProcessDefinitionEntityManager().findProcessDefinitionsByQueryCriteria(query);
        if (processDefinitions != null && processDefinitions.size() > 0) {
            return processDefinitions.get(0);
        }
        return null;
    }

    @Override
    public long findDeploymentCountByQueryCriteria(DeploymentQueryImpl deploymentQuery) {
        return deploymentDataManager.findDeploymentCountByQueryCriteria(deploymentQuery);
    }

    @Override
    public List<Deployment> findDeploymentsByQueryCriteria(DeploymentQueryImpl deploymentQuery) {
        return deploymentDataManager.findDeploymentsByQueryCriteria(deploymentQuery);
    }

    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return deploymentDataManager.getDeploymentResourceNames(deploymentId);
    }

    @Override
    public List<Deployment> findDeploymentsByNativeQuery(Map<String, Object> parameterMap) {
        return deploymentDataManager.findDeploymentsByNativeQuery(parameterMap);
    }

    @Override
    public long findDeploymentCountByNativeQuery(Map<String, Object> parameterMap) {
        return deploymentDataManager.findDeploymentCountByNativeQuery(parameterMap);
    }

    public DeploymentDataManager getDeploymentDataManager() {
        return deploymentDataManager;
    }

    public void setDeploymentDataManager(DeploymentDataManager deploymentDataManager) {
        this.deploymentDataManager = deploymentDataManager;
    }

}
