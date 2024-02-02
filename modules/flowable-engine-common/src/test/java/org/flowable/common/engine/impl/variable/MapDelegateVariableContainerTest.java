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
package org.flowable.common.engine.impl.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MapDelegateVariableContainerTest {

    @Test
    void expectTransientVariableTakePrecedence() {
        VariableContainer delegate = Mockito.mock(VariableContainer.class);
        when(delegate.getVariable("myVar1")).thenReturn("zonk");
        when(delegate.getVariable("myVar2")).thenReturn("Bar");
        when(delegate.hasVariable("myVar2")).thenReturn(true);

        MapDelegateVariableContainer container = new MapDelegateVariableContainer(delegate).addTransientVariable("myVar1", "Foo");

        assertThat(container.hasVariable("myVar1")).isTrue();
        assertThat(container.getVariable("myVar1")).isEqualTo("Foo");

        assertThat(container.hasVariable("myVar2")).isTrue();
        assertThat(container.getVariable("myVar2")).isEqualTo("Bar");

        assertThat(container.hasVariable("myVar3")).isFalse();

        verify(delegate, never()).getVariable("myVar1");
        verify(delegate, times(1)).getVariable("myVar2");
        verify(delegate, times(1)).hasVariable("myVar2");
    }

    @Test
    void expectSetVariableSetsToDelegate() {
        VariableContainer delegate = Mockito.mock(VariableContainer.class);

        Object myObj = new Object();
        when(delegate.getVariable("myVar1")).thenReturn(myObj);
        MapDelegateVariableContainer container = new MapDelegateVariableContainer(delegate).addTransientVariable("myVar1", "Foo");

        container.setVariable("myVar1", myObj);

        verify(delegate, times(1)).setVariable("myVar1", myObj);
    }

    @Test
    void expectSetVariableWhenDelegateEmpty() {
        MapDelegateVariableContainer container = new MapDelegateVariableContainer(VariableContainer.empty());
        container.setVariable("foo", "bar");

        assertThat(container.getVariable("foo")).isEqualTo("bar");

        container.clearTransientVariables();
        assertThat(container.getVariable("foo")).isNull();
    }

    @Test
    void expectWorksWithEmptyDelegate() {
        MapDelegateVariableContainer container = new MapDelegateVariableContainer().addTransientVariable("myVar1", "Foo");

        assertThat(container.hasVariable("myVar1")).isTrue();
        assertThat(container.getVariable("myVar1")).isEqualTo("Foo");

        assertThat(container.hasVariable("myVar2")).isFalse();
        assertThat(container.getVariable("myVar2")).isNull();
    }
}
