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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.flowable.bpm.model.bpmn.EventBasedGatewayType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

public class EventBasedGatewayTest
        extends AbstractGatewayTest<EventBasedGateway> {

    public Collection<AttributeAssumption> getAttributesAssumptions() {
        return Arrays.asList(
                new AttributeAssumption("instantiate", false, false, false),
                new AttributeAssumption("eventGatewayType", false, false, EventBasedGatewayType.Exclusive));
    }

    @Test
    public void getInstantiate() {
        assertThat(gateway.isInstantiate()).isTrue();
    }

    @Test
    public void getEventGatewayType() {
        assertThat(gateway.getEventGatewayType()).isEqualTo(EventBasedGatewayType.Parallel);
    }

    @Test
    public void shouldFailSetAsyncToEventBasedGateway() {
        // fetching should fail
        try {
            gateway.isFlowableAsync();
            fail("Expected: UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ex) {
            // True
        }

        // set the attribute should fail to!
        try {
            gateway.setFlowableAsync(false);
            fail("Expected: UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ex) {
            // True
        }
    }
}
