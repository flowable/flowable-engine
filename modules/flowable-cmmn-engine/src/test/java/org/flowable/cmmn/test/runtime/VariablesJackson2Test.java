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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.EngineConfigurer;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.impl.json.jackson2.Jackson2VariableJsonMapper;
import org.flowable.task.api.Task;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.types.JsonType;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
public class VariablesJackson2Test extends CustomCmmnConfigurationFlowableTestCase {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @EngineConfigurer
    protected static void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setVariableJsonMapper(new Jackson2VariableJsonMapper(OBJECT_MAPPER));
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/runtime/oneHumanTaskCase.cmmn")
    public void testUpdateJsonVariableWithJackson3JsonNode() {
        tools.jackson.databind.ObjectMapper jackson3ObjectMapper = tools.jackson.databind.json.JsonMapper.shared();

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneHumanTaskCase")
                .variable("customer", OBJECT_MAPPER.createObjectNode().put("name", "Kermit"))
                .start();

        cmmnRuntimeService.setVariable(caseInstance.getId(), "customer", jackson3ObjectMapper.createObjectNode().put("name", "Gonzo"));

        VariableInstance customerVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "customer");
        assertThat(customerVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
        assertThat(customerVarInstance.getValue()).isInstanceOf(ObjectNode.class);
        assertThatJson(customerVarInstance.getValue())
                .isEqualTo("""
                        {
                          name: 'Gonzo'
                        }
                        """);

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.setVariable(task.getId(), "customer", jackson3ObjectMapper.createObjectNode().put("name", "Fozzie"));

        customerVarInstance = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "customer");
        assertThat(customerVarInstance.getTypeName()).isEqualTo(JsonType.TYPE_NAME);
        assertThat(customerVarInstance.getValue()).isInstanceOf(ObjectNode.class);
        assertThatJson(customerVarInstance.getValue())
                .isEqualTo("""
                        {
                          name: 'Fozzie'
                        }
                        """);
    }

}
