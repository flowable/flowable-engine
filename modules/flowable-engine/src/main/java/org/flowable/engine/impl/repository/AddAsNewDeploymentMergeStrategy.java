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
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.DeploymentMergeStrategy;
import org.flowable.engine.repository.ProcessDefinition;

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

        ProcessDefinitionQueryImpl processDefinitionQuery = new ProcessDefinitionQueryImpl();
        if (StringUtils.isEmpty(newTenantId)) {
            processDefinitionQuery.processDefinitionWithoutTenantId();
        } else {
            processDefinitionQuery.processDefinitionTenantId(newTenantId);
        }
        
        Map<String, Integer> latestVersionNumberLookupTable = processDefinitionQuery
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
