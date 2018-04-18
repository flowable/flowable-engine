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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.repository.EngineDeployment;
import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.engine.impl.bpmn.parser.BpmnParser;
import org.flowable.engine.impl.cmd.DeploymentSettings;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParsedDeploymentBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParsedDeploymentBuilder.class);

    protected EngineDeployment deployment;
    protected BpmnParser bpmnParser;
    protected Map<String, Object> deploymentSettings;

    public ParsedDeploymentBuilder(EngineDeployment deployment,
            BpmnParser bpmnParser, Map<String, Object> deploymentSettings) {
        this.deployment = deployment;
        this.bpmnParser = bpmnParser;
        this.deploymentSettings = deploymentSettings;
    }

    public ParsedDeployment build() {
        List<ProcessDefinitionEntity> processDefinitions = new ArrayList<>();
        Map<ProcessDefinitionEntity, BpmnParse> processDefinitionsToBpmnParseMap = new LinkedHashMap<>();
        Map<ProcessDefinitionEntity, EngineResource> processDefinitionsToResourceMap = new LinkedHashMap<>();

        DeploymentEntity deploymentEntity = (DeploymentEntity) deployment;
        for (EngineResource resource : deploymentEntity.getResources().values()) {
            if (isBpmnResource(resource.getName())) {
                LOGGER.debug("Processing BPMN resource {}", resource.getName());
                BpmnParse parse = createBpmnParseFromResource(resource);
                for (ProcessDefinitionEntity processDefinition : parse.getProcessDefinitions()) {
                    processDefinitions.add(processDefinition);
                    processDefinitionsToBpmnParseMap.put(processDefinition, parse);
                    processDefinitionsToResourceMap.put(processDefinition, resource);
                }
            }
        }

        return new ParsedDeployment(deploymentEntity, processDefinitions,
                processDefinitionsToBpmnParseMap, processDefinitionsToResourceMap);
    }

    protected BpmnParse createBpmnParseFromResource(EngineResource resource) {
        String resourceName = resource.getName();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(resource.getBytes());

        BpmnParse bpmnParse = bpmnParser.createParse()
                .sourceInputStream(inputStream)
                .setSourceSystemId(resourceName)
                .deployment(deployment)
                .name(resourceName);

        if (deploymentSettings != null) {

            // Schema validation if needed
            if (deploymentSettings.containsKey(DeploymentSettings.IS_BPMN20_XSD_VALIDATION_ENABLED)) {
                bpmnParse.setValidateSchema((Boolean) deploymentSettings.get(DeploymentSettings.IS_BPMN20_XSD_VALIDATION_ENABLED));
            }

            // Process validation if needed
            if (deploymentSettings.containsKey(DeploymentSettings.IS_PROCESS_VALIDATION_ENABLED)) {
                bpmnParse.setValidateProcess((Boolean) deploymentSettings.get(DeploymentSettings.IS_PROCESS_VALIDATION_ENABLED));
            }

        } else {
            // On redeploy, we assume it is validated at the first deploy
            bpmnParse.setValidateSchema(false);
            bpmnParse.setValidateProcess(false);
        }

        try {
            bpmnParse.execute();
        } catch (Exception e) {
            LOGGER.error("Could not parse resource {}", resource.getName(), e);
            throw e;
        }
        return bpmnParse;
    }

    protected boolean isBpmnResource(String resourceName) {
        for (String suffix : ResourceNameUtil.BPMN_RESOURCE_SUFFIXES) {
            if (resourceName.endsWith(suffix)) {
                return true;
            }
        }

        return false;
    }

}
