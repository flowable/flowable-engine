/*
 * Copyright 2025, Flowable Licences AG.
 * This license is based on the software license agreement and terms and conditions in effect between the parties
 * at the time of purchase of the Flowable software product.
 * Your agreement to these terms and conditions is required to install or use the Flowable software product and/or this file.
 * Flowable is a trademark of Flowable AG registered in several countries.
 */
package org.flowable.editor.language.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

public class MultipleCharacterEventsConverterTest {

    @BpmnXmlConverterTest("multipleCharacterEvents.bpmn")
    void validateModel(BpmnModel model) {
        assertThat(model.getFlowElement("task").getExtensionElements().get("data").get(0).getElementText()).isEqualTo("a&b&c&d&e&f&g");
        
        assertThat(model.getFlowElement("end").getExtensionElements().get("data").get(0).getElementText()).isEqualTo("a&b&c&d&e&f&g");
        
        assertThat(model.getFlowElement("flow2").getExtensionElements().get("data").get(0).getElementText()).isEqualTo("a&b&c&d&e&f&g");
    }

}
