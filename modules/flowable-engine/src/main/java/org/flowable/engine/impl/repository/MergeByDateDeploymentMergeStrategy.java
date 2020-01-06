package org.flowable.engine.impl.repository;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.DeploymentQueryImpl;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.DeploymentMergeStrategy;
import org.flowable.engine.repository.ProcessDefinition;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Valentin Zickner
 */
public class MergeByDateDeploymentMergeStrategy implements DeploymentMergeStrategy {

    @Override
    public void prepareMerge(CommandContext commandContext, String deploymentId, String newTenantId) {
        List<ProcessDefinition> processDefinitions = new ProcessDefinitionQueryImpl().deploymentId(deploymentId).list();
        ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
        for (ProcessDefinition processDefinition : processDefinitions) {
            processDefinitionEntityManager.updateProcessDefinitionVersionForProcessDefinitionId(processDefinition.getId(), 0);
        }
    }

    @Override
    public void finalizeMerge(CommandContext commandContext, String deploymentId, String newTenantId) {
        List<ProcessDefinition> processDefinitions = new ProcessDefinitionQueryImpl().deploymentId(deploymentId).list();
        ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
        for (ProcessDefinition processDefinition : processDefinitions) {
            List<ProcessDefinition> allProcessDefinitionsWithKey = new ProcessDefinitionQueryImpl()
                    .processDefinitionTenantId(newTenantId)
                    .processDefinitionKey(processDefinition.getKey())
                    .list();
            List<ProcessDefinition> orderedProcessDefinitions = sortProcessDefinitionsByDeploymentTime(allProcessDefinitionsWithKey);

            int versionNumber = allProcessDefinitionsWithKey.size();
            for (ProcessDefinition alreadyExistingProcessDefinition : orderedProcessDefinitions) {
                processDefinitionEntityManager.updateProcessDefinitionVersionForProcessDefinitionId(alreadyExistingProcessDefinition.getId(), versionNumber--);
            }
        }
    }

    protected List<ProcessDefinition> sortProcessDefinitionsByDeploymentTime(List<ProcessDefinition> allProcessDefinitionsWithKey) {
        List<String> deploymentIds = extractDeploymentIds(allProcessDefinitionsWithKey);
        Map<String, ProcessDefinition> processDefinitionLookupTable = allProcessDefinitionsWithKey
                .stream()
                .collect(Collectors.toMap(ProcessDefinition::getDeploymentId, Function.identity()));

        return new DeploymentQueryImpl()
                .deploymentIds(deploymentIds)
                .orderByDeploymentTime()
                .desc()
                .list()
                .stream()
                .map(deployment -> processDefinitionLookupTable.get(deployment.getId()))
                .collect(Collectors.toList());
    }

    protected List<String> extractDeploymentIds(List<ProcessDefinition> allProcessDefinitionsWithKey) {
        return allProcessDefinitionsWithKey
                .stream()
                .map(ProcessDefinition::getDeploymentId)
                .collect(Collectors.toList());
    }

}
