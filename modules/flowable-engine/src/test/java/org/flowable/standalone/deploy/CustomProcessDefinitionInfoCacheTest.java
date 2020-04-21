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

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.junit.jupiter.api.Test;

public class CustomProcessDefinitionInfoCacheTest extends ResourceFlowableTestCase {

    public CustomProcessDefinitionInfoCacheTest() {
        super("org/flowable/standalone/deploy/custom.procedefinitioninfo.cache.test.flowable.cfg.xml");
    }

    @Test
    public void testCustomProcessDefinitionInfoCache() throws IOException {
        final CustomProcessDefinitionInfoCache processDefinitionInfoCache =
                (CustomProcessDefinitionInfoCache) processEngineConfiguration.getProcessDefinitionInfoCache();
        assertThat(processDefinitionInfoCache.size()).isZero();
        String processDefinitionTemplate = DeploymentCacheTestUtil
                .readTemplateFile("/org/flowable/standalone/deploy/processDefinitionInfoCacheTest.bpmn20.xml");
        repositoryService.createDeployment().addString("Process 1.bpmn20.xml", processDefinitionTemplate).deploy();
        assertThat(processDefinitionInfoCache.size()).isPositive();
        // Cleanup
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }
}
