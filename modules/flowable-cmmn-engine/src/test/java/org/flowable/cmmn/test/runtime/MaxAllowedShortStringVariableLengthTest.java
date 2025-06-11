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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.commons.lang3.RandomStringUtils;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.variable.MaxAllowedLengthVariableVerifier;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class MaxAllowedShortStringVariableLengthTest extends CustomCmmnConfigurationFlowableTestCase {

    @Override
    protected String getEngineName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setVariableLengthVerifier(new MaxAllowedLengthVariableVerifier(500));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
    public void stringVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("stringVar", "Test value")
                .start();

        assertThatThrownBy(() -> cmmnRuntimeService.setVariable(caseInstance.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(600)))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage(
                        "The length of the string value exceeds the maximum allowed length of 500 characters. Current length: 600, for variable: stringVar in scope cmmn with id "
                                + caseInstance.getId());

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThatThrownBy(() -> cmmnTaskService.setVariableLocal(task.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(600)))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage(
                        "The length of the string value exceeds the maximum allowed length of 500 characters. Current length: 600, for variable: stringVar in scope task with id "
                                + task.getId());

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar")).isEqualTo("Test value");
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
    public void longStringVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("stringVar", "Test value")
                .start();

        assertThatThrownBy(() -> cmmnRuntimeService.setVariable(caseInstance.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(6000)))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage(
                        "The length of the longString value exceeds the maximum allowed length of 500 characters. Current length: 6000, for variable: stringVar in scope cmmn with id "
                                + caseInstance.getId());

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();
        assertThatThrownBy(() -> cmmnTaskService.setVariableLocal(task.getId(), "stringVar", RandomStringUtils.insecure().nextAlphanumeric(6000)))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage(
                        "The length of the longString value exceeds the maximum allowed length of 500 characters. Current length: 6000, for variable: stringVar in scope task with id "
                                + task.getId());

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "stringVar")).isEqualTo("Test value");
    }

}
