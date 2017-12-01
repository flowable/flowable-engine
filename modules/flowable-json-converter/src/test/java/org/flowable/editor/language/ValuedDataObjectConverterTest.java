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
package org.flowable.editor.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.flowable.bpmn.model.BooleanDataObject;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.DateDataObject;
import org.flowable.bpmn.model.DoubleDataObject;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IntegerDataObject;
import org.flowable.bpmn.model.LongDataObject;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.StringDataObject;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.ValuedDataObject;
import org.junit.Test;

public class ValuedDataObjectConverterTest extends AbstractConverterTest {

    @Test
    public void convertJsonToModel() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        validateModel(bpmnModel);
    }

    @Test
    public void doubleConversionValidation() throws Exception {
        BpmnModel bpmnModel = readJsonFile();
        bpmnModel = convertToJsonAndBack(bpmnModel);
        validateModel(bpmnModel);
    }

    @Override
    protected String getResource() {
        return "test.valueddataobjectmodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("start1", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof StartEvent);
        assertEquals("start1", flowElement.getId());

        // verify main process data objects
        List<ValuedDataObject> dataObjects = model.getMainProcess().getDataObjects();
        assertEquals(12, dataObjects.size());
        for (ValuedDataObject dObj : dataObjects) {
            if ("dObj1".equals(dObj.getId())) {
                assertEquals(StringDataObject.class, dObj.getClass());
                assertEquals("StringTest", dObj.getName());
                assertTrue(dObj.getValue() instanceof String);
                assertEquals("xsd:string", dObj.getItemSubjectRef().getStructureRef());
                assertEquals("Testing123", dObj.getValue());
            } else if ("dObj2".equals(dObj.getId())) {
                assertEquals(BooleanDataObject.class, dObj.getClass());
                assertEquals("BooleanTest", dObj.getName());
                assertEquals("xsd:boolean", dObj.getItemSubjectRef().getStructureRef());
                assertTrue(dObj.getValue() instanceof Boolean);
                assertEquals(Boolean.TRUE, dObj.getValue());
            } else if ("dObj3".equals(dObj.getId())) {
                assertEquals(DateDataObject.class, dObj.getClass());
                assertEquals("DateTest", dObj.getName());
                assertEquals("xsd:datetime", dObj.getItemSubjectRef().getStructureRef());
                assertTrue(dObj.getValue() instanceof Date);
                GregorianCalendar dateCal = new GregorianCalendar();
                dateCal.setTime((Date) dObj.getValue());
                assertEquals(2013, dateCal.get(Calendar.YEAR));
                assertEquals(8, dateCal.get(Calendar.MONTH));
                assertEquals(16, dateCal.get(Calendar.DAY_OF_MONTH));
                assertEquals(11, dateCal.get(Calendar.HOUR_OF_DAY));
                assertEquals(23, dateCal.get(Calendar.MINUTE));
            } else if ("dObj4".equals(dObj.getId())) {
                assertEquals(DoubleDataObject.class, dObj.getClass());
                assertEquals("DoubleTest", dObj.getName());
                assertEquals("xsd:double", dObj.getItemSubjectRef().getStructureRef());
                assertTrue(dObj.getValue() instanceof Double);
                assertEquals(123456789d, dObj.getValue());
            } else if ("dObj5".equals(dObj.getId())) {
                assertEquals(IntegerDataObject.class, dObj.getClass());
                assertEquals("IntegerTest", dObj.getName());
                assertEquals("xsd:int", dObj.getItemSubjectRef().getStructureRef());
                assertTrue(dObj.getValue() instanceof Integer);
                assertEquals(123, dObj.getValue());
            } else if ("dObj6".equals(dObj.getId())) {
                assertEquals(LongDataObject.class, dObj.getClass());
                assertEquals("LongTest", dObj.getName());
                assertEquals("xsd:long", dObj.getItemSubjectRef().getStructureRef());
                assertTrue(dObj.getValue() instanceof Long);
                assertEquals((long) -123456, dObj.getValue());
            }
        }

        flowElement = model.getMainProcess().getFlowElement("subprocess1", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof SubProcess);
        assertEquals("subprocess1", flowElement.getId());
        SubProcess subProcess = (SubProcess) flowElement;
        assertEquals(17, subProcess.getFlowElements().size());

        // verify subprocess data objects
        dataObjects = ((SubProcess) flowElement).getDataObjects();
        assertEquals(12, dataObjects.size());
        for (ValuedDataObject dObj : dataObjects) {
            if ("dObj1".equals(dObj.getId())) {
                assertEquals(StringDataObject.class, dObj.getClass());
                assertEquals("SubStringTest", dObj.getName());
                assertTrue(dObj.getValue() instanceof String);
                assertEquals("xsd:string", dObj.getItemSubjectRef().getStructureRef());
                assertEquals("Testing456", dObj.getValue());
            } else if ("dObj2".equals(dObj.getId())) {
                assertEquals(BooleanDataObject.class, dObj.getClass());
                assertEquals("SubBooleanTest", dObj.getName());
                assertEquals("xsd:boolean", dObj.getItemSubjectRef().getStructureRef());
                assertTrue(dObj.getValue() instanceof Boolean);
                assertEquals(Boolean.FALSE, dObj.getValue());
            } else if ("dObj3".equals(dObj.getId())) {
                assertEquals(DateDataObject.class, dObj.getClass());
                assertEquals("SubDateTest", dObj.getName());
                assertEquals("xsd:datetime", dObj.getItemSubjectRef().getStructureRef());
                assertTrue(dObj.getValue() instanceof Date);
                GregorianCalendar dateCal = new GregorianCalendar();
                dateCal.setTime((Date) dObj.getValue());
                assertEquals(2013, dateCal.get(Calendar.YEAR));
                assertEquals(10, dateCal.get(Calendar.MONTH));
                assertEquals(11, dateCal.get(Calendar.DAY_OF_MONTH));
            } else if ("dObj4".equals(dObj.getId())) {
                assertEquals(DoubleDataObject.class, dObj.getClass());
                assertEquals("SubDoubleTest", dObj.getName());
                assertEquals("xsd:double", dObj.getItemSubjectRef().getStructureRef());
                assertTrue(dObj.getValue() instanceof Double);
                assertEquals(678912345d, dObj.getValue());
            } else if ("dObj5".equals(dObj.getId())) {
                assertEquals(IntegerDataObject.class, dObj.getClass());
                assertEquals("SubIntegerTest", dObj.getName());
                assertEquals("xsd:int", dObj.getItemSubjectRef().getStructureRef());
                assertTrue(dObj.getValue() instanceof Integer);
                assertEquals(45, dObj.getValue());
            } else if ("dObj6".equals(dObj.getId())) {
                assertEquals(LongDataObject.class, dObj.getClass());
                assertEquals("SubLongTest", dObj.getName());
                assertEquals("xsd:long", dObj.getItemSubjectRef().getStructureRef());
                assertTrue(dObj.getValue() instanceof Long);
                assertEquals(456123L, dObj.getValue());
                assertEquals(1, dObj.getExtensionElements().size());
            }
        }
    }
}
