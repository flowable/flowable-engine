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
package org.flowable.bpm.model.bpmn.instance;

import static org.assertj.core.api.Assertions.fail;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;


public class BoundaryEventTest
        extends BpmnModelElementInstanceTest {

    public TypeAssumption getTypeAssumption() {
        return new TypeAssumption(CatchEvent.class, false);
    }

    public Collection<ChildElementAssumption> getChildElementAssumptions() {
        return null;
    }

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Arrays.asList(
                new AttributeAssumption("cancelActivity", false, false, true),
                new AttributeAssumption("attachedToRef", false, true));
    }

    @Test
    public void shouldFailSettingFlowableAsync() {
        BoundaryEvent boundaryEvent = modelInstance.newInstance(BoundaryEvent.class);
        try {
            boundaryEvent.isFlowableAsync();
            fail("Expected: UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ex) {
            // True
        }

        try {
            boundaryEvent.setFlowableAsync(false);
            fail("Expected: UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ex) {
            // True
        }
    }

    @Test
    public void shouldFailSettingFlowableExclusive() {
        BoundaryEvent boundaryEvent = modelInstance.newInstance(BoundaryEvent.class);
        try {
            boundaryEvent.isFlowableExclusive();
            fail("Expected: UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ex) {
            // True
        }

        try {
            boundaryEvent.setFlowableExclusive(false);
            fail("Expected: UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ex) {
            // True
        }
    }
}
