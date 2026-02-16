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
package org.flowable.engine.test.el;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.api.definition.DefinitionVariableContainer;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.junit.jupiter.api.Test;

/**
 * @author Christopher Welsch
 */
class DefinitionVariableContainerTest {

    @Test
    void testDefaultConstructor() {
        DefinitionVariableContainer container = new DefinitionVariableContainer();
        assertThat(container.getDefinitionId()).isNull();
        assertThat(container.getDeploymentId()).isNull();
        assertThat(container.getScopeType()).isNull();
        assertThat(container.getTenantId()).isNull();
    }

    @Test
    void testParameterizedConstructor() {
        DefinitionVariableContainer container = new DefinitionVariableContainer("defId", "deplId", "bpmn", "tenantA");
        assertThat(container.getDefinitionId()).isEqualTo("defId");
        assertThat(container.getDeploymentId()).isEqualTo("deplId");
        assertThat(container.getScopeType()).isEqualTo("bpmn");
        assertThat(container.getTenantId()).isEqualTo("tenantA");
    }

    @Test
    void testImplementsVariableContainer() {
        DefinitionVariableContainer container = new DefinitionVariableContainer("defId", "deplId", "bpmn", "tenantA");
        assertThat(container).isInstanceOf(VariableContainer.class);
    }

    @Test
    void testVariableContainerBehavior() {
        DefinitionVariableContainer container = new DefinitionVariableContainer("defId", "deplId", "bpmn", "tenantA");

        // DefinitionVariableContainer does not hold variables
        assertThat(container.hasVariable("anyVar")).isFalse();
        assertThat(container.getVariable("anyVar")).isNull();
        assertThat(container.getVariableNames()).isEmpty();

        // setVariable and setTransientVariable should not throw
        container.setVariable("test", "value");
        container.setTransientVariable("test", "value");
        assertThat(container.hasVariable("test")).isFalse();
    }
}
