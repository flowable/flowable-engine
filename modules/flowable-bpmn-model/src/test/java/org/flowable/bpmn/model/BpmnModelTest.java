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