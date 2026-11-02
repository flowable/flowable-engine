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

package org.flowable.standalone.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.junit.jupiter.api.Test;

public class ProcessDefinitionCacheTest extends ResourceFlowableTestCase {

    public ProcessDefinitionCacheTest() {
        super("org/flowable/standalone/cache/flowable.cfg.xml");
    }

    @Test
    public void testProcessDefinitionCache() {
        List<String> deploymentIds = new ArrayList<>();
        int nrOfModels = 9;
        for (int nrOfIterations = 0; nrOfIterations < 10; nrOfIterations++) {
            DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();
            for (int i = 1; i <= nrOfModels; i++) {
                deploymentBuilder.addClasspathResource("org/flowable/standalone/cache/process" + i + ".bpmn");
            }
            Deployment deployment = deploymentBuilder.deploy();
            deploymentIds.add(0, deployment.getId());

            assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo((nrOfIterations + 1) * nrOfModels);

            for (int i = 1; i <= nrOfModels; i++) {
                runtimeService.startProcessInstanceByKey("process" + i);
            }

            assertThat(managementService.createJobQuery().count()).isEqualTo((nrOfIterations + 1) * nrOfModels);
        }
        
        waitForJobExecutorToProcessAllJobs(30000, 200);
        
        assertThat(managementService.createJobQuery().count()).isEqualTo(0);
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            for (int i = 1; i <= nrOfModels; i++) {
                assertThat(historyService.createHistoricProcessInstanceQuery().processDefinitionKey("process" + i).count()).isEqualTo(10);
            }
        }
        
        for (String deploymentId : deploymentIds) {
            repositoryService.deleteDeployment(deploymentId, true);
        }
    }

}
