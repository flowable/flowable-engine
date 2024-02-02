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
package org.flowable.spring.test.junit4;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class SpringJunit4Test {
    
    @Rule
    public FlowableCmmnRule cmmnRule = new FlowableCmmnRule("org/flowable/spring/test/junit4/springTypicalUsageTest-context.xml");

    @Test
    @CmmnDeployment
    public void simpleCaseTest() {
        CaseInstance caseInstance = cmmnRule.getCmmnRuntimeService().createCaseInstanceBuilder().caseDefinitionKey("junitCase").start();
        assertThat(caseInstance).isNotNull();
    }
}
