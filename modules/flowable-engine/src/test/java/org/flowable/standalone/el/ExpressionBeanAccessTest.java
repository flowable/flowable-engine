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

package org.flowable.standalone.el;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 */
public class ExpressionBeanAccessTest extends ResourceFlowableTestCase {

    public ExpressionBeanAccessTest() {
        super("org/flowable/standalone/el/flowable.cfg.xml");
    }

    @Test
    @Deployment
    public void testConfigurationBeanAccess() {
        // Exposed bean returns 'I'm exposed' when to-string is called in first
        // service-task
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("expressionBeanAccess");
        assertThat(runtimeService.getVariable(pi.getId(), "exposedBeanResult")).isEqualTo("I'm exposed");

        // After signaling, an expression tries to use a bean that is present in
        // the configuration but is not added to the beans-list
        assertThatThrownBy(
                () -> runtimeService.trigger(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).onlyChildExecutions().singleResult().getId()))
                .isExactlyInstanceOf(FlowableException.class)
                .hasRootCauseMessage("Cannot resolve identifier 'notExposedBean'");
    }
}
