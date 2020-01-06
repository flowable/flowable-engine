package org.flowable.engine.impl.repository;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.DeploymentMergeStrategy;
import org.flowable.engine.repository.ProcessDefinition;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Valentin Zickner
 */
public class AddAsNewDeploymentMergeStrategy implements DeploymentMergeStrategy {

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

        Map<String, Integer> latestVersionNumberLookupTable = new ProcessDefinitionQueryImpl()
                .processDefinitionTenantId(newTenantId)
                .latestVersion()
                .list()
                .stream()
                .collect(Collectors.toMap(ProcessDefinition::getKey, ProcessDefinition::getVersion));
        ProcessDefinitionEntityManager processDefinitionEntityManager = CommandContextUtil.getProcessDefinitionEntityManager(commandContext);
        for (ProcessDefinition processDefinition : processDefinitions) {
            int nextVersionNumber = latestVersionNumberLookupTable.getOrDefault(processDefinition.getKey(), 0) + 1;
            processDefinitionEntityManager.updateProcessDefinitionVersionForProcessDefinitionId(processDefinition.getId(), nextVersionNumber);
        }
    }

}
