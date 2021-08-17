/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flowable.editor.language.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.Assignment;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.DataAssociation;
import org.flowable.common.engine.impl.util.io.BytesStreamSource;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

/**
 * @author Calin Cerchez
 */
class DataAssociationConverterTest {

    @BpmnXmlConverterTest("activityWithDataAssociations.bpmn")
    void testWriteDataAssociations(BpmnModel model) {
        BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
        byte[] output = bpmnXMLConverter.convertToXML(model);
        BpmnModel parsedBpmnModel = bpmnXMLConverter.convertToBpmnModel(new BytesStreamSource(output), true, true);
        Activity activity = (Activity) parsedBpmnModel.getFlowElement("servicetask");
        List<DataAssociation> dataInputAssociations = activity.getDataInputAssociations();
        List<DataAssociation> dataOutputAssociations = activity.getDataOutputAssociations();

        assertThat(dataInputAssociations)
                .extracting(DataAssociation::getId, DataAssociation::getSourceRef, DataAssociation::getTargetRef, DataAssociation::getTransformation)
                .containsExactly(
                        tuple("dataAssociation-inputObject", "inputObject_1", "servicetask", "${dataOutputOfServiceTask.prettyPrint}")
                );

        DataAssociation association = dataInputAssociations.get(0);
        assertThat(association.getAssignments()).isEmpty();

        assertThat(dataOutputAssociations)
                .extracting(DataAssociation::getId, DataAssociation::getSourceRef, DataAssociation::getTargetRef, DataAssociation::getTransformation)
                .containsExactly(
                        tuple("dataAssociation-outputObject", "servicetask", "outputObject_1", null)
                );

        association = dataOutputAssociations.get(0);
        assertThat(association.getAssignments())
                .extracting(Assignment::getFrom, Assignment::getTo)
                .containsExactly(
                        tuple("${dataInputOfProcess.prefix}", "${dataInputOfServiceTask.prefix}")
                );
    }
}
