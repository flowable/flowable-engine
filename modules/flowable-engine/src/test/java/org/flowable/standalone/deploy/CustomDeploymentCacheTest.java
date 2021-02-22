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
package org.flowable.standalone.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.text.MessageFormat;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class CustomDeploymentCacheTest extends ResourceFlowableTestCase {

    public CustomDeploymentCacheTest() {
        super("org/flowable/standalone/deploy/custom.deployment.cache.test.flowable.cfg.xml");
    }

    @Test
    public void testCustomDeploymentCacheUsed() throws IOException {
        CustomDeploymentCache customCache = (CustomDeploymentCache) processEngineConfiguration.getProcessDefinitionCache();
        assertThat(customCache.getCachedProcessDefinition()).isNull();

        String processDefinitionTemplate = DeploymentCacheTestUtil.readTemplateFile("/org/flowable/standalone/deploy/deploymentCacheTest.bpmn20.xml");
        for (int i = 1; i <= 5; i++) {
            repositoryService.createDeployment().addString("Process " + i + ".bpmn20.xml", MessageFormat.format(processDefinitionTemplate, i)).deploy();
            assertThat(customCache.getCachedProcessDefinition()).isNotNull();
        }

        // Cleanup
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

}
