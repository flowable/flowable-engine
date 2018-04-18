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
package org.flowable.engine.impl.cmd;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class RedeployV5ProcessDefinitionsCmd implements Command<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedeployV5ProcessDefinitionsCmd.class);

    @Override
    public Void execute(CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);

        RepositoryService repositoryService = processEngineConfiguration.getRepositoryService();
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .processDefinitionEngineVersion(Flowable5Util.V5_ENGINE_TAG)
                .list();

        if (!processDefinitions.isEmpty()) {

            List<String> deploymentIds = new ArrayList<>();
            Map<String, List<ProcessDefinition>> deploymentMap = new HashMap<>();
            for (ProcessDefinition processDefinition : processDefinitions) {

                if (!deploymentIds.contains(processDefinition.getDeploymentId())) {
                    deploymentIds.add(processDefinition.getDeploymentId());
                }

                List<ProcessDefinition> groupedProcessDefinitions = null;
                if (deploymentMap.containsKey(processDefinition.getDeploymentId())) {
                    groupedProcessDefinitions = deploymentMap.get(processDefinition.getDeploymentId());
                } else {
                    groupedProcessDefinitions = new ArrayList<>();
                }
                groupedProcessDefinitions.add(processDefinition);

                deploymentMap.put(processDefinition.getDeploymentId(), groupedProcessDefinitions);
            }

            List<Deployment> deployments = repositoryService.createDeploymentQuery()
                    .deploymentIds(deploymentIds)
                    .list();

            for (Deployment deployment : deployments) {
                DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
                if (deployment.getName() != null) {
                    deploymentBuilder.name(deployment.getName());
                }

                if (deployment.getCategory() != null) {
                    deploymentBuilder.category(deployment.getCategory());
                }

                if (deployment.getKey() != null) {
                    deploymentBuilder.key(deployment.getKey());
                }

                if (deployment.getTenantId() != null) {
                    deploymentBuilder.tenantId(deployment.getTenantId());
                }

                List<ProcessDefinition> groupedProcessDefinitions = deploymentMap.get(deployment.getId());
                for (ProcessDefinition processDefinition : groupedProcessDefinitions) {
                    LOGGER.info("adding v5 process definition with id: {} and key: {} for redeployment",
                            processDefinition.getId(), processDefinition.getKey());

                    InputStream definitionStream = repositoryService.getResourceAsStream(deployment.getId(), processDefinition.getResourceName());
                    deploymentBuilder.addInputStream(processDefinition.getResourceName(), definitionStream);
                }

                deploymentBuilder.deploy();
            }
        }

        return null;
    }

}
