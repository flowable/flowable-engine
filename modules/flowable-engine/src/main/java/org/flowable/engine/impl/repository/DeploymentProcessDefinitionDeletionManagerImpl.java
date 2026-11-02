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
package org.flowable.engine.impl.repository;

import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.bpmn.behavior.ProcessLevelStartEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ProcessLevelStartEventDeployContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

/**
 * @author Filip Hrisafov
 */
public class DeploymentProcessDefinitionDeletionManagerImpl implements DeploymentProcessDefinitionDeletionManager {

    protected final ProcessEngineConfigurationImpl engineConfiguration;

    public DeploymentProcessDefinitionDeletionManagerImpl(ProcessEngineConfigurationImpl engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
    }

    @Override
    public void deleteDefinitionForDeployment(ProcessDefinition processDefinition, String deploymentId) {
        deleteProcessDefinitionIdentityLinks(processDefinition);
        deleteEventSubscriptions(processDefinition);
        deleteProcessDefinitionInfo(processDefinition.getId());

        removeTimerStartJobs(processDefinition);

        // If the deleted process definition was the latest version, restore the previous version's
        // timer / message / signal / event-registry start events.
        restorePreviousStartEventsIfNeeded(processDefinition);
    }

    protected void deleteProcessDefinitionIdentityLinks(ProcessDefinition processDefinition) {
        engineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService()
                .deleteIdentityLinksByProcessDefinitionId(processDefinition.getId());
    }

    protected void deleteEventSubscriptions(ProcessDefinition processDefinition) {
        engineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService()
                .deleteEventSubscriptionsForProcessDefinition(processDefinition.getId());
    }

    protected void deleteProcessDefinitionInfo(String processDefinitionId) {
        engineConfiguration.getProcessDefinitionInfoEntityManager().deleteProcessDefinitionInfo(processDefinitionId);
    }

    protected void removeTimerStartJobs(ProcessDefinition processDefinition) {
        TimerJobService timerJobService = engineConfiguration.getJobServiceConfiguration().getTimerJobService();
        List<TimerJobEntity> timerStartJobs = timerJobService.findJobsByTypeAndProcessDefinitionId(TimerStartEventJobHandler.TYPE, processDefinition.getId());
        if (timerStartJobs != null && timerStartJobs.size() > 0) {
            for (TimerJobEntity timerStartJob : timerStartJobs) {
                if (getEventDispatcher() != null && getEventDispatcher().isEnabled()) {
                    getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED,
                            timerStartJob, null, null, processDefinition.getId()), engineConfiguration.getEngineCfgKey());
                }

                timerJobService.deleteTimerJob(timerStartJob);
            }
        }
    }

    protected void restorePreviousStartEventsIfNeeded(ProcessDefinition processDefinition) {
        ProcessDefinitionEntity latestProcessDefinition = findLatestProcessDefinition(processDefinition);
        if (latestProcessDefinition == null || !processDefinition.getId().equals(latestProcessDefinition.getId())) {
            return;
        }

        // Try to find a previous version (it could be some versions are missing due to deletions)
        ProcessDefinition previousProcessDefinition = findNewLatestProcessDefinitionAfterRemovalOf(processDefinition);
        if (previousProcessDefinition == null) {
            return;
        }

        org.flowable.bpmn.model.Process previousProcess = ProcessDefinitionUtil.getProcess(previousProcessDefinition.getId());
        if (CollectionUtil.isEmpty(previousProcess.getFlowElements())) {
            return;
        }

        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(previousProcessDefinition.getId());
        CommandContext commandContext = Context.getCommandContext();
        ProcessDefinitionEntity previousProcessDefinitionEntity = (ProcessDefinitionEntity) previousProcessDefinition;
        for (FlowElement flowElement : previousProcess.getFlowElements()) {
            if (flowElement instanceof StartEvent startEvent
                    && startEvent.getBehavior() instanceof ProcessLevelStartEventActivityBehavior behavior) {
                behavior.deploy(new ProcessLevelStartEventDeployContext(previousProcessDefinitionEntity,
                        previousProcess, bpmnModel, startEvent, engineConfiguration, commandContext, true));
            }
        }
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

    protected ProcessDefinitionEntityManager getProcessDefinitionEntityManager() {
        return engineConfiguration.getProcessDefinitionEntityManager();
    }

    protected FlowableEventDispatcher getEventDispatcher() {
        return engineConfiguration.getEventDispatcher();
    }
}
