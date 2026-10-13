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

package org.flowable.bpmn.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BpmnModelTest {

    @Test
    void addEscalationStoresByEscalationCode() {
        BpmnModel model = new BpmnModel();

        model.addEscalation("ESCALATION_ID", "ESCALATION_CODE", "Payment failed");

        assertThat(model.containsEscalationRef("ESCALATION_CODE")).isTrue();
        assertThat(model.getEscalation("ESCALATION_CODE")).isNotNull();
        assertThat(model.containsEscalationRef("ESCALATION_ID")).isFalse();
        assertThat(model.getEscalation("ESCALATION_CODE").getEscalationCode())
                .isEqualTo("ESCALATION_CODE");
    }
}