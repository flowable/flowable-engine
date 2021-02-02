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
package org.flowable.editor.language.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.bpmn.model.ValuedDataObject;
import org.flowable.editor.language.xml.util.BpmnXmlConverterTest;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @see {https://activiti.atlassian.net/browse/ACT-1847}.
 */
class ValuedDataObjectConverterTest {

    @BpmnXmlConverterTest("valueddataobjectmodel.bpmn")
    void validateModel(BpmnModel model) throws ParseException {
        FlowElement flowElement = model.getMainProcess().getFlowElement("start1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(StartEvent.class, startEvent -> {
                    assertThat(startEvent.getId()).isEqualTo("start1");
                });

        // verify the main process data objects
        List<ValuedDataObject> dataObjects = model.getProcess(null).getDataObjects();
        assertThat(dataObjects).hasSize(8);

        Map<String, ValuedDataObject> objectMap = new HashMap<>();
        for (ValuedDataObject valueObj : dataObjects) {
            objectMap.put(valueObj.getId(), valueObj);
        }

        ValuedDataObject dataObj = objectMap.get("dObj1");
        assertThat(dataObj.getId()).isEqualTo("dObj1");
        assertThat(dataObj.getName()).isEqualTo("StringTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:string");
        assertThat(dataObj.getValue()).isEqualTo("Testing1&2&3");

        dataObj = objectMap.get("dObj2");
        assertThat(dataObj.getId()).isEqualTo("dObj2");
        assertThat(dataObj.getName()).isEqualTo("BooleanTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:boolean");
        assertThat(dataObj.getValue()).isEqualTo(Boolean.TRUE);

        dataObj = objectMap.get("dObj3");
        assertThat(dataObj.getId()).isEqualTo("dObj3");
        assertThat(dataObj.getName()).isEqualTo("DateTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:datetime");
        assertThat(dataObj.getValue()).isInstanceOf(Date.class);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        assertThat(sdf.format(dataObj.getValue())).isEqualTo("2013-09-16T11:23:00");

        dataObj = objectMap.get("dObj4");
        assertThat(dataObj.getId()).isEqualTo("dObj4");
        assertThat(dataObj.getName()).isEqualTo("DoubleTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:double");
        assertThat(dataObj.getValue())
                .isInstanceOf(Double.class)
                .isEqualTo(123456789d);

        dataObj = objectMap.get("dObj5");
        assertThat(dataObj.getId()).isEqualTo("dObj5");
        assertThat(dataObj.getName()).isEqualTo("IntegerTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:int");
        assertThat(dataObj.getValue())
                .isInstanceOf(Integer.class)
                .isEqualTo(123);

        dataObj = objectMap.get("dObj6");
        assertThat(dataObj.getId()).isEqualTo("dObj6");
        assertThat(dataObj.getName()).isEqualTo("LongTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:long");
        assertThat(dataObj.getValue())
                .isInstanceOf(Long.class)
                .isEqualTo(-123456L);
        assertThat(dataObj.getExtensionElements()).hasSize(1);
        List<ExtensionElement> testValues = dataObj.getExtensionElements().get("testvalue");
        assertThat(testValues)
                .extracting(ExtensionElement::getName, ExtensionElement::getElementText)
                .containsExactly(tuple("testvalue", "test"));

        dataObj = objectMap.get("dObjJson");
        assertThat(dataObj.getId()).isEqualTo("dObjJson");
        assertThat(dataObj.getName()).isEqualTo("JsonTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:json");
        assertThat(dataObj.getValue()).isInstanceOf(JsonNode.class);
        assertThat(((JsonNode) dataObj.getValue()).get("eltString").asText()).isEqualTo("my-string");
        final GregorianCalendar myCalendar = new GregorianCalendar(2020, 1, 21, 10, 25, 23);
        final Date myDate = myCalendar.getTime();
        final String myDateJson = ((JsonNode) dataObj.getValue()).get("eltDate").asText();
        assertThat(sdf.parse(myDateJson)).isEqualTo(myDate);

        dataObj = objectMap.get("NoData");
        assertThat(dataObj.getId()).isEqualTo("NoData");
        assertThat(dataObj.getName()).isEqualTo("NoData");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:datetime");
        assertThat(dataObj.getValue()).isNull();

        flowElement = model.getMainProcess().getFlowElement("userTask1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(UserTask.class, userTask -> {
                    assertThat(userTask.getId()).isEqualTo("userTask1");
                    assertThat(userTask.getAssignee()).isEqualTo("kermit");
                });

        flowElement = model.getMainProcess().getFlowElement("subprocess1");
        assertThat(flowElement)
                .isInstanceOfSatisfying(SubProcess.class, subProcess -> {
                    assertThat(subProcess.getId()).isEqualTo("subprocess1");
                    assertThat(subProcess.getFlowElements()).hasSize(11);

                    // verify the sub process data objects
                    assertThat(subProcess.getDataObjects()).hasSize(6);
                });

        objectMap = new HashMap<>();
        for (ValuedDataObject valueObj : ((SubProcess) flowElement).getDataObjects()) {
            objectMap.put(valueObj.getId(), valueObj);
        }

        dataObj = objectMap.get("dObj7");
        assertThat(dataObj.getId()).isEqualTo("dObj7");
        assertThat(dataObj.getName()).isEqualTo("StringSubTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:string");
        assertThat(dataObj.getValue()).isEqualTo("Testing456");

        dataObj = objectMap.get("dObj8");
        assertThat(dataObj.getId()).isEqualTo("dObj8");
        assertThat(dataObj.getName()).isEqualTo("BooleanSubTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:boolean");
        assertThat(dataObj.getValue()).isEqualTo(Boolean.FALSE);

        dataObj = objectMap.get("dObj9");
        assertThat(dataObj.getId()).isEqualTo("dObj9");
        assertThat(dataObj.getName()).isEqualTo("DateSubTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:datetime");
        assertThat(dataObj.getValue()).isInstanceOf(Date.class);
        assertThat(sdf.format(dataObj.getValue())).isEqualTo("2013-11-11T22:00:00");

        dataObj = objectMap.get("dObj10");
        assertThat(dataObj.getId()).isEqualTo("dObj10");
        assertThat(dataObj.getName()).isEqualTo("DoubleSubTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:double");
        assertThat(dataObj.getValue())
                .isInstanceOf(Double.class)
                .isEqualTo(678912345d);

        dataObj = objectMap.get("dObj11");
        assertThat(dataObj.getId()).isEqualTo("dObj11");
        assertThat(dataObj.getName()).isEqualTo("IntegerSubTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:int");
        assertThat(dataObj.getValue())
                .isInstanceOf(Integer.class)
                .isEqualTo(45);

        dataObj = objectMap.get("dObj12");
        assertThat(dataObj.getId()).isEqualTo("dObj12");
        assertThat(dataObj.getName()).isEqualTo("LongSubTest");
        assertThat(dataObj.getItemSubjectRef().getStructureRef()).isEqualTo("xsd:long");
        assertThat(dataObj.getValue())
                .isInstanceOf(Long.class)
                .isEqualTo(456123L);
    }
}
