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
package org.activiti.standalone.deploy;

import java.text.MessageFormat;

import org.activiti.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.common.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionCacheEntry;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentProperties;

/**
 * @author Joram Barrez
 */
public class DeploymentCacheLimitTest extends ResourceFlowableTestCase {

    public DeploymentCacheLimitTest() {
        super("org/activiti/standalone/deploy/deployment.cache.limit.test.flowable.cfg.xml");
    }

    public void testDeploymentCacheLimit() {
        int processDefinitionCacheLimit = 3; // This is set in the configuration above

        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl) processEngineConfiguration.getFlowable5CompatibilityHandler()
                .getRawProcessConfiguration();

        DefaultDeploymentCache<ProcessDefinitionCacheEntry> processDefinitionCache = (DefaultDeploymentCache<ProcessDefinitionCacheEntry>) activiti5ProcessEngineConfig.getProcessDefinitionCache();
        assertEquals(0, processDefinitionCache.size());

        String processDefinitionTemplate = DeploymentCacheTestUtil.readTemplateFile(
                "/org/activiti/standalone/deploy/deploymentCacheTest.bpmn20.xml");
        for (int i = 1; i <= 5; i++) {
            repositoryService.createDeployment()
                    .addString("Process " + i + ".bpmn20.xml", MessageFormat.format(processDefinitionTemplate, i))
                    .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                    .deploy();

            if (i < processDefinitionCacheLimit) {
                assertEquals(i, processDefinitionCache.size());
            } else {
                assertEquals(processDefinitionCacheLimit, processDefinitionCache.size());
            }
        }

        // Cleanup
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

}
