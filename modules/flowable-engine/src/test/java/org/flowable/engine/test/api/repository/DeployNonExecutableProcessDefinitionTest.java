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
package org.flowable.engine.test.api.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author jbarrez
 */
public class DeployNonExecutableProcessDefinitionTest extends PluggableFlowableTestCase {

    /*
     * Test for https://jira.codehaus.org/browse/ACT-2071
     * 
     * In this test, a process definition is deployed together with one that is not executable. The none-executable should not be startable.
     */
    @Test
    @Deployment
    public void testDeployNonExecutableProcessDefinition() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("oneTaskProcessNonExecutable"))
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("No process definition found for key 'oneTaskProcessNonExecutable'");
    }

}
