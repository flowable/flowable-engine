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
package org.flowable.engine.impl.bpmn.deployer;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.assignment.CandidateUtil;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.bpmn.behavior.ProcessLevelStartEventActivityBehavior;
import org.flowable.engine.impl.bpmn.behavior.ProcessLevelStartEventDeployContext;
import org.flowable.engine.impl.bpmn.behavior.ProcessLevelStartEventUndeployContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.identitylink.service.IdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.cmd.CancelJobsCmd;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.service.impl.el.NoExecutionVariableScope;

/**
 * Methods for working with deployments. Much of the actual work of {@link BpmnDeployer} is done by orchestrating the different pieces of work this class does; by having them here, we allow other
 * deployers to make use of them.
 */
public class BpmnDeploymentHelper {

    /**
     * Verifies that no two process definitions share the same key, to prevent database unique index violation.
     * 
     * @throws FlowableException
     *             if any two processes have the same key
     */
    public void verifyProcessDefinitionsDoNotShareKeys(Collection<ProcessDefinitionEntity> processDefinitions) {
        Set<String> keySet = new LinkedHashSet<>();
        for (ProcessDefinitionEntity processDefinition : processDefinitions) {
            if (keySet.contains(processDefinition.getKey())) {
                throw new FlowableException(
                        "The deployment contains process definitions with the same key '" + processDefinition.getKey() + "' (process id attribute), this is not allowed");
            }
            keySet.add(processDefinition.getKey());
        }
    }

    /**
     * Updates all the process definition entities to match the deployment's values for tenant, engine version, and deployment id.
     */
    public void copyDeploymentValuesToProcessDefinitions(DeploymentEntity deployment,
            List<ProcessDefinitionEntity> processDefinitions) {
        String engineVersion = deployment.getEngineVersion();
        String tenantId = deployment.getTenantId();
        String deploymentId = deployment.getId();

        for (ProcessDefinitionEntity processDefinition : processDefinitions) {

            // Backwards compatibility
            if (engineVersion != null) {
                processDefinition.setEngineVersion(engineVersion);
            }

            // process definition inherits the tenant id
            if (tenantId != null) {
                processDefinition.setTenantId(tenantId);
            }

            processDefinition.setDeploymentId(deploymentId);
        }
    }

    /**
     * Updates all the process definition entities to have the correct resource names.
     */
    public void setResourceNamesOnProcessDefinitions(ParsedDeployment parsedDeployment) {
        for (ProcessDefinitionEntity processDefinition : parsedDeployment.getAllProcessDefinitions()) {
            String resourceName = parsedDeployment.getResourceForProcessDefinition(processDefinition).getName();
            processDefinition.setResourceName(resourceName);
        }
    }

    /**
     * Gets the most recent persisted process definition that matches this one for tenant and key. If none is found, returns null. This method assumes that the tenant and key are properly set on the
     * process definition entity.
     */
    public ProcessDefinitionEntity getMostRecentVersionOfProcessDefinition(ProcessDefinitionEntity processDefinition) {
        String key = processDefinition.getKey();
        String tenantId = processDefinition.getTenantId();
        ProcessDefinitionEntityManager processDefinitionManager = CommandContextUtil.getProcessEngineConfiguration().getProcessDefinitionEntityManager();

        ProcessDefinitionEntity existingDefinition = null;

        if (tenantId != null && !tenantId.equals(ProcessEngineConfiguration.NO_TENANT_ID)) {
            existingDefinition = processDefinitionManager.findLatestProcessDefinitionByKeyAndTenantId(key, tenantId);
        } else {
            existingDefinition = processDefinitionManager.findLatestProcessDefinitionByKey(key);
        }

        return existingDefinition;
    }
    
    /**
     * Gets the most recent persisted derived process definition that matches this one for tenant and key. If none is found, returns null. This method assumes that the tenant and key are properly set on the
     * process definition entity.
     */
    public ProcessDefinitionEntity getMostRecentDerivedVersionOfProcessDefinition(ProcessDefinitionEntity processDefinition) {
        String key = processDefinition.getKey();
        String tenantId = processDefinition.getTenantId();
        ProcessDefinitionEntityManager processDefinitionManager = CommandContextUtil.getProcessEngineConfiguration().getProcessDefinitionEntityManager();

        ProcessDefinitionEntity existingDefinition = null;

        if (tenantId != null && !tenantId.equals(ProcessEngineConfiguration.NO_TENANT_ID)) {
            existingDefinition = processDefinitionManager.findLatestDerivedProcessDefinitionByKeyAndTenantId(key, tenantId);
        } else {
            existingDefinition = processDefinitionManager.findLatestDerivedProcessDefinitionByKey(key);
        }

        return existingDefinition;
    }

    /**
     * Gets the persisted version of the already-deployed process definition. Note that this is different from {@link #getMostRecentVersionOfProcessDefinition} as it looks specifically for a process
     * definition that is already persisted and attached to a particular deployment, rather than the latest version across all deployments.
     */
    public ProcessDefinitionEntity getPersistedInstanceOfProcessDefinition(ProcessDefinitionEntity processDefinition) {
        String deploymentId = processDefinition.getDeploymentId();
        if (StringUtils.isEmpty(processDefinition.getDeploymentId())) {
            throw new IllegalStateException("Provided process definition must have a deployment id.");
        }

        ProcessDefinitionEntityManager processDefinitionManager = CommandContextUtil.getProcessEngineConfiguration().getProcessDefinitionEntityManager();
        ProcessDefinitionEntity persistedProcessDefinition = null;
        if (processDefinition.getTenantId() == null || ProcessEngineConfiguration.NO_TENANT_ID.equals(processDefinition.getTenantId())) {
            persistedProcessDefinition = processDefinitionManager.findProcessDefinitionByDeploymentAndKey(deploymentId, processDefinition.getKey());
        } else {
            persistedProcessDefinition = processDefinitionManager.findProcessDefinitionByDeploymentAndKeyAndTenantId(deploymentId, processDefinition.getKey(), processDefinition.getTenantId());
        }

        return persistedProcessDefinition;
    }

    /**
     * Updates all timers and events for the process definition. The undeploy half iterates the previous
     * process definition's top-level start events; each behavior either does its own per-start-event work
     * (e.g. the EventRegistry "manual" re-point) or registers an obsolete event subscription / timer job
     * handler type with the context. After the undeploy iteration the deployer issues one mass-delete per
     * unique registered type — fewer DB round-trips than per-start-event deletes, and tighter than the
     * historical fixed Message+Signal+EventRegistry sweep that always ran regardless of which types the
     * previous process definition used. The deploy half then iterates the new process definition's top-level start events to register the
     * new artifacts.
     */
    public void updateTimersAndEvents(ProcessDefinitionEntity processDefinition,
            ProcessDefinitionEntity previousProcessDefinition, ParsedDeployment parsedDeployment) {

        CommandContext commandContext = Context.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);

        Process process = parsedDeployment.getProcessModelForProcessDefinition(processDefinition);
        BpmnModel bpmnModel = parsedDeployment.getBpmnModelForProcessDefinition(processDefinition);

        if (previousProcessDefinition != null) {
            Process previousProcess = ProcessDefinitionUtil.getProcess(previousProcessDefinition.getId());
            Set<String> obsoleteEventSubscriptionTypes = new LinkedHashSet<>();
            Set<String> obsoleteTimerJobHandlerTypes = new LinkedHashSet<>();

            forEachTopLevelStartEventBehavior(previousProcess, (startEvent, behavior) -> behavior.undeploy(
                    new ProcessLevelStartEventUndeployContext(previousProcessDefinition, processDefinition,
                            startEvent, processEngineConfiguration, commandContext,
                            obsoleteEventSubscriptionTypes, obsoleteTimerJobHandlerTypes)));

            if (!obsoleteEventSubscriptionTypes.isEmpty()) {
                deleteObsoleteEventSubscriptions(previousProcessDefinition, obsoleteEventSubscriptionTypes, processEngineConfiguration);
            }
            for (String timerJobHandlerType : obsoleteTimerJobHandlerTypes) {
                cancelObsoleteTimerJobs(previousProcessDefinition, timerJobHandlerType, processEngineConfiguration, commandContext);
            }
        }

        forEachTopLevelStartEventBehavior(process, (startEvent, behavior) -> behavior.deploy(
                new ProcessLevelStartEventDeployContext(processDefinition,
                        process, bpmnModel, startEvent, processEngineConfiguration, commandContext)));
    }

    protected void deleteObsoleteEventSubscriptions(ProcessDefinitionEntity processDefinition, Collection<String> eventHandlerTypes,
            ProcessEngineConfigurationImpl processEngineConfiguration) {

        EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
        List<EventSubscriptionEntity> subscriptionsToDelete = eventSubscriptionService
                .findEventSubscriptionsByTypesAndProcessDefinitionId(eventHandlerTypes, processDefinition.getId(), processDefinition.getTenantId());

        for (EventSubscriptionEntity eventSubscription : subscriptionsToDelete) {
            eventSubscriptionService.deleteEventSubscription(eventSubscription);
            CountingEntityUtil.handleDeleteEventSubscriptionEntityCount(eventSubscription);
        }
    }

    protected void cancelObsoleteTimerJobs(ProcessDefinitionEntity processDefinition, String timerJobHandlerType,
            ProcessEngineConfigurationImpl processEngineConfiguration, CommandContext commandContext) {

        TimerJobService timerJobService = processEngineConfiguration.getJobServiceConfiguration().getTimerJobService();
        List<TimerJobEntity> jobsToDelete;
        if (processDefinition.getTenantId() != null && !ProcessEngineConfiguration.NO_TENANT_ID.equals(processDefinition.getTenantId())) {
            jobsToDelete = timerJobService.findJobsByTypeAndProcessDefinitionKeyAndTenantId(
                    timerJobHandlerType, processDefinition.getKey(), processDefinition.getTenantId());
        } else {
            jobsToDelete = timerJobService.findJobsByTypeAndProcessDefinitionKeyNoTenantId(
                    timerJobHandlerType, processDefinition.getKey());
        }

        if (jobsToDelete != null) {
            for (TimerJobEntity job : jobsToDelete) {
                new CancelJobsCmd(job.getId(), processEngineConfiguration.getJobServiceConfiguration()).execute(commandContext);
            }
        }
    }

    protected void forEachTopLevelStartEventBehavior(Process process, StartEventBehaviorVisitor visitor) {
        if (process == null || process.getFlowElements() == null) {
            return;
        }
        for (FlowElement flowElement : process.getFlowElements()) {
            if (flowElement instanceof StartEvent startEvent
                    && startEvent.getBehavior() instanceof ProcessLevelStartEventActivityBehavior behavior) {
                visitor.visit(startEvent, behavior);
            }
        }
    }

    @FunctionalInterface
    protected interface StartEventBehaviorVisitor {
        void visit(StartEvent startEvent, ProcessLevelStartEventActivityBehavior behavior);
    }

    enum ExpressionType {
        USER, GROUP
    }

    /**
     * @param processDefinition
     */
    public void addAuthorizationsForNewProcessDefinition(Process process, ProcessDefinitionEntity processDefinition) {
        CommandContext commandContext = Context.getCommandContext();

        addAuthorizationsFromIterator(commandContext, process.getCandidateStarterUsers(), processDefinition, ExpressionType.USER);
        addAuthorizationsFromIterator(commandContext, process.getCandidateStarterGroups(), processDefinition, ExpressionType.GROUP);
    }

    protected void addAuthorizationsFromIterator(CommandContext commandContext, List<String> expressions,
            ProcessDefinitionEntity processDefinition, ExpressionType expressionType) {

        if (expressions != null) {
            ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
            IdentityLinkService identityLinkService = processEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkService();
            ExpressionManager expressionManager = processEngineConfiguration.getExpressionManager();

            Iterator<String> iterator = expressions.iterator();
            while (iterator.hasNext()) {
                @SuppressWarnings("cast")
                String expressionStr = iterator.next();

                Expression expression = expressionManager.createExpression(expressionStr);
                Object value = expression.getValue(NoExecutionVariableScope.getSharedInstance());
                if (value != null) {

                    Collection<String> candidates = CandidateUtil.extractCandidates(value);
                    for (String candidate : candidates) {
                        IdentityLinkEntity identityLink = identityLinkService.createIdentityLink();
                        identityLink.setProcessDefId(processDefinition.getId());
                        if (expressionType == ExpressionType.USER) {
                            identityLink.setUserId(candidate);
                        } else if (expressionType == ExpressionType.GROUP) {
                            identityLink.setGroupId(candidate);
                        }
                        identityLink.setType(IdentityLinkType.CANDIDATE);
                        identityLinkService.insertIdentityLink(identityLink);
                    }
                }
            }
        }

    }

}
