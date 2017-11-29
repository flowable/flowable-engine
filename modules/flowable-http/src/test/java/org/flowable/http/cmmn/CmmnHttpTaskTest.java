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
package org.flowable.http.cmmn;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnRule;
import org.flowable.http.bpmn.HttpServiceTaskTestServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author martin.grofcik
 */
public class CmmnHttpTaskTest {

    @Rule
    public FlowableCmmnRule cmmnRule = new FlowableCmmnRule("org/flowable/http/cmmn/CmmnHttpTaskTest.cfg.xml");
    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
       HttpServiceTaskTestServer.setUp();
    }


    @Test
    @CmmnDeployment(
            resources = { "org/flowable/http/cmmn/CmmnHttpTaskTest.testSimpleGet.cmmn"
            }
    )
    public void testDecisionServiceTask() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .start();

        assertNotNull(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testGetWithVariableName() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        assertThat((String) cmmnRule.getCmmnRuntimeService().getVariable(caseInstance.getId(), "test"), containsString("John"));

    }

    @Test
    @CmmnDeployment
    public void testGetWithoutVariableName() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        assertThat((String) cmmnRule.getCmmnRuntimeService().getVariable(caseInstance.getId(), "httpGet.responseBody"),
                containsString("John"));
    }

    @Test
    @CmmnDeployment
    public void testGetWithResponseHandler() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        Map<String, Object> variables = cmmnRule.getCmmnRuntimeService().getVariables(caseInstance.getId());
        assertEquals(2, variables.size());
        String firstName = null;
        String lastName = null;

        for (Map.Entry<String,Object> variable : variables.entrySet()) {
            if ("firstName".equals(variable.getKey())) {
                firstName = (String) variable.getValue();
            } else if ("lastName".equals(variable.getKey())) {
                lastName = (String) variable.getValue();
            }
        }

        assertEquals("John", firstName);
        assertEquals("Doe", lastName);
    }

}
