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
package org.flowable.cmmn.test.jupiter;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngines;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.CmmnDeploymentId;
import org.flowable.cmmn.engine.test.FlowableCmmnTest;
import org.flowable.cmmn.engine.test.FlowableCmmnTestHelper;
import org.junit.jupiter.api.Test;

/**
 * Test runners follow the this rule: - if the class extends Testcase, run as Junit 3 - otherwise use Junit 4, or JUnit 5
 * <p>
 * So this test can be included in the regular test suite without problems.
 *
 * @author Filip Hrisafov
 */
@FlowableCmmnTest
class FlowableCmmnJupiterTest {

    @Test
    @CmmnDeployment
    void extensionUsageExample(CmmnEngine cmmnEngine) {
        CmmnRuntimeService runtimeService = cmmnEngine.getCmmnRuntimeService();
        runtimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("extensionUsage")
            .start();

        CmmnTaskService taskService = cmmnEngine.getCmmnTaskService();
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).isEqualTo("The Task");

        taskService.complete(task.getId());
        assertThat(runtimeService.createCaseInstanceQuery().count()).isEqualTo(0);

        assertThat(cmmnEngine.getName()).as("cmmn engine name").isEqualTo(CmmnEngines.NAME_DEFAULT);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/jupiter/FlowableCmmnJupiterTest.extensionUsageExample.cmmn")
    void extensionUsageCmmnDeploymentIdExample(@CmmnDeploymentId String deploymentId, FlowableCmmnTestHelper testHelper,
        CmmnRepositoryService repositoryService) {
        assertThat(deploymentId).as("deploymentId parameter").isEqualTo(testHelper.getDeploymentIdFromDeploymentAnnotation());

        org.flowable.cmmn.api.repository.CmmnDeployment deployment = repositoryService.createDeploymentQuery().singleResult();

        assertThat(deployment.getId()).as("queried deployment").isEqualTo(deploymentId);
        assertThat(deployment.getName()).as("deployment name").isEqualTo("FlowableCmmnJupiterTest.extensionUsageCmmnDeploymentIdExample");
    }
}
