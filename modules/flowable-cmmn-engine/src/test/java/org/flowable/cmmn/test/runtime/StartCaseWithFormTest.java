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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.Test;

/**
 * @author martin.grofcik
 */
public class StartCaseWithFormTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/ServiceTaskTest.testJavaServiceTask.cmmn")
    public void testStartCaseWithFormWithoutFormKeyInModel() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(caseDefinition.getId())
                .variable("test", "test2")
                .startWithForm();
        assertThat(caseInstance).isNotNull();

    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/StartCaseWithFormTest.testStartCaseWithForm.cmmn")
    public void testStartCaseWithForm_withoutFormRepositoryService() {
        CaseDefinition caseDefinition = cmmnRepositoryService.createCaseDefinitionQuery().caseDefinitionKey("myCase").singleResult();
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionId(caseDefinition.getId())
                .variable("test", "test2")
                .startWithForm();
        assertThat(caseInstance).as("It must be possible to start case without form repository service").isNotNull();
    }
}