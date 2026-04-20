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

package org.flowable.spring.test.el;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.flowable.common.engine.impl.test.LoggingExtension;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnRepositoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Tijs Rademakers
 */
@ExtendWith(LoggingExtension.class)
class SpringRuleBeanTest {

    protected static final String CTX_PATH = "org/flowable/spring/test/el/SpringBeanTest-context.xml";

    protected ApplicationContext applicationContext;
    protected DmnRepositoryService repositoryService;
    protected DmnDecisionService ruleService;

    protected void createAppContext(String path) {
        this.applicationContext = new ClassPathXmlApplicationContext(path);
        this.repositoryService = applicationContext.getBean(DmnRepositoryService.class);
        this.ruleService = applicationContext.getBean(DmnDecisionService.class);
    }

    @AfterEach
    void tearDown() {
        removeAllDeployments();
        this.applicationContext = null;
        this.repositoryService = null;
        this.ruleService = null;
    }

    @Test
    public void testSimpleRuleBean() {
        createAppContext(CTX_PATH);
        repositoryService.createDeployment().addClasspathResource("org/flowable/spring/test/el/springbean.dmn").deploy();

        Map<String, Object> outputVariables = ruleService.createExecuteDecisionBuilder()
                .decisionKey("springDecision")
                .variable("input1", "John Doe")
                .executeWithSingleResult();

        assertThat(outputVariables)
                .containsEntry("output1", "test1");

        outputVariables = ruleService.createExecuteDecisionBuilder()
                .decisionKey("springDecision")
                .variable("input1", "test")
                .executeWithSingleResult();

        assertThat(outputVariables)
                .containsEntry("output1", "test2");
    }

    // --Helper methods
    // ----------------------------------------------------------

    private void removeAllDeployments() {
        for (DmnDeployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId());
        }
    }

}