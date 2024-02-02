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

import java.util.Map;

import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.FlowableDmnRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Yvo Swillens
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:org/flowable/spring/test/junit4/springTypicalUsageTest-context.xml")
public class SpringJunit4Test {

    @Autowired
    private DmnEngine dmnEngine;

    @Autowired
    private DmnDecisionService ruleService;

    @Autowired
    @Rule
    public FlowableDmnRule flowableDmnSpringRule;

    @After
    public void closeDmnEngine() {
        // Required, since all the other tests seem to do a specific drop on the
        // end
        dmnEngine.close();
    }

    @Test
    @DmnDeployment
    public void simpleDecisionTest() {
        Map<String, Object> executionResult = ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("input1", "testString")
                .executeWithSingleResult();

        assertThat(executionResult)
                .containsEntry("output1", "test1");
        assertThat(flowableDmnSpringRule.getRepositoryService()).isNotNull();
    }
}
