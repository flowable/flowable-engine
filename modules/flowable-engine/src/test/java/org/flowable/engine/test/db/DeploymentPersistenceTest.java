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

package org.flowable.engine.test.db;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 */
public class DeploymentPersistenceTest extends PluggableFlowableTestCase {

    @Test
    public void testDeploymentPersistence() {
        Deployment deployment = repositoryService.createDeployment().name("strings").addString("org/flowable/test/HelloWorld.string", "hello world").addString("org/flowable/test/TheAnswer.string", "42")
                .deploy();

        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
        assertEquals(1, deployments.size());
        deployment = deployments.get(0);

        assertEquals("strings", deployment.getName());
        assertNotNull(deployment.getDeploymentTime());

        String deploymentId = deployment.getId();
        List<String> resourceNames = repositoryService.getDeploymentResourceNames(deploymentId);
        Set<String> expectedResourceNames = new HashSet<>();
        expectedResourceNames.add("org/flowable/test/HelloWorld.string");
        expectedResourceNames.add("org/flowable/test/TheAnswer.string");
        assertEquals(expectedResourceNames, new HashSet<>(resourceNames));

        InputStream resourceStream = repositoryService.getResourceAsStream(deploymentId, "org/flowable/test/HelloWorld.string");
        assertTrue(Arrays.equals("hello world".getBytes(), IoUtil.readInputStream(resourceStream, "test")));

        resourceStream = repositoryService.getResourceAsStream(deploymentId, "org/flowable/test/TheAnswer.string");
        assertTrue(Arrays.equals("42".getBytes(), IoUtil.readInputStream(resourceStream, "test")));

        repositoryService.deleteDeployment(deploymentId);
    }
}
