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
package org.flowable.crystalball.simulator.impl;

import java.util.List;

import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.SimulationEventHandler;
import org.flowable.crystalball.simulator.SimulationRunContext;
import org.flowable.engine.repository.DeploymentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class deploys resources from classpath
 */
public class DeployClasspathResourcesEventHandler implements SimulationEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeployClasspathResourcesEventHandler.class);

    /**
     * process to start key
     */
    protected String resourcesKey;

    public DeployClasspathResourcesEventHandler(String resourcesKey) {
        this.resourcesKey = resourcesKey;
    }

    @Override
    public void init() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handle(SimulationEvent event) {

        List<String> resources = (List<String>) event.getProperty(resourcesKey);

        DeploymentBuilder deploymentBuilder = SimulationRunContext.getRepositoryService().createDeployment();

        for (String resource : resources) {
            LOGGER.debug("adding resource [{}] to repository {}", resource, SimulationRunContext.getRepositoryService());
            deploymentBuilder.addClasspathResource(resource);
        }

        deploymentBuilder.deploy();
    }

}
