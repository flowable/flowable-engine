package org.flowable.engine.impl.repository;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.DeploymentMergeStrategy;
import org.flowable.engine.repository.ProcessDefinition;

import java.util.List;

/**
 * @author Valentin Zickner
 */
public class AddAsOldDeploymentMergeStrategy implements DeploymentMergeStrategy {

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
            List<ProcessDefinition> alreadyExistingProcessDefinitions = new ProcessDefinitionQueryImpl()
                    .processDefinitionTenantId(newTenantId)
                    .processDefinitionKey(processDefinition.getKey())
                    .orderByProcessDefinitionVersion()
                    .desc()
                    .list();
            for (ProcessDefinition alreadyExistingProcessDefinition : alreadyExistingProcessDefinitions) {
                if (!alreadyExistingProcessDefinition.getId().equals(processDefinition.getId())) {
                    processDefinitionEntityManager.updateProcessDefinitionVersionForProcessDefinitionId(alreadyExistingProcessDefinition.getId(), alreadyExistingProcessDefinition.getVersion() + 1);
                }
            }
            processDefinitionEntityManager.updateProcessDefinitionVersionForProcessDefinitionId(processDefinition.getId(), 1);
        }
    }

}
