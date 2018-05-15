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
package org.flowable.app.spring.test.junit4;

import static org.junit.Assert.assertNotNull;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.engine.test.AppDeployment;
import org.flowable.app.engine.test.FlowableAppRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class SpringAppEngineJunit4Test {
    
    @Rule
    public FlowableAppRule appRule = new FlowableAppRule("org/flowable/app/spring/test/junit4/springTypicalUsageTest-context.xml");

    @Test
    @AppDeployment
    public void simpleAppTest() {
        AppDefinition appDefinition = appRule.getAppRepositoryService().createAppDefinitionQuery().appDefinitionKey("testApp").singleResult();
        assertNotNull(appDefinition);
    }
}
