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

package org.flowable.engine.impl.rules;

import java.util.Map;

import org.drools.core.marshalling.impl.ProtobufMessages.KnowledgeBase;
import org.flowable.engine.common.EngineDeployer;
import org.flowable.engine.common.api.repository.EngineDeployment;
import org.flowable.engine.common.api.repository.EngineResource;
import org.flowable.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.kie.api.KieBase;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class RulesDeployer implements EngineDeployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RulesDeployer.class);

    @Override
    public void deploy(EngineDeployment deployment, Map<String, Object> deploymentSettings) {
        LOGGER.debug("Processing rules deployment {}", deployment.getName());

        KnowledgeBuilder knowledgeBuilder = null;

        DeploymentManager deploymentManager = CommandContextUtil.getProcessEngineConfiguration().getDeploymentManager();

        Map<String, EngineResource> resources = deployment.getResources();
        for (String resourceName : resources.keySet()) {
            if (resourceName.endsWith(".drl")) { // is only parsing .drls sufficient? what about other rule dsl's? (@see ResourceType)
                LOGGER.info("Processing rules resource {}", resourceName);
                if (knowledgeBuilder == null) {
                    knowledgeBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
                }
                EngineResource resourceEntity = resources.get(resourceName);
                byte[] resourceBytes = resourceEntity.getBytes();
                Resource droolsResource = ResourceFactory.newByteArrayResource(resourceBytes);
                knowledgeBuilder.add(droolsResource, ResourceType.DRL);
            }
        }

        if (knowledgeBuilder != null) {
            KieBase kieBase = knowledgeBuilder.newKieBase();
            deploymentManager.getKnowledgeBaseCache().add(deployment.getId(), kieBase);
        }
    }
}
