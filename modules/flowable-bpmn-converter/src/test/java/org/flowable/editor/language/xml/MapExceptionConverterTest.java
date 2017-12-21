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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.flowable.bpmn.exceptions.XMLException;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;
import org.junit.Test;

public class MapExceptionConverterTest extends AbstractConverterTest {

    String resourceName;

    @Override
    protected String getResource() {
        return resourceName;
    }

    @Test
    public void testMapExceptionWithInvalidHasChildren() throws Exception {
        resourceName = "mapException/mapExceptionInvalidHasChildrenModel.bpmn";
        try {
            BpmnModel bpmnModel = readXMLFile();
            fail("No exception is thrown for mapExecution with invalid boolean for hasChildren");
        } catch (XMLException x) {
            assertTrue(x.getMessage().indexOf("is not valid boolean") != -1);

        } catch (Exception e) {
            fail("wrong exception thrown. XmlException expected, " + e.getClass() + " thrown");
        }
    }

    @Test
    public void testMapExceptionWithNoErrorCode() throws Exception {
        resourceName = "mapException/mapExceptionNoErrorCode.bpmn";
        try {

            BpmnModel bpmnModel = readXMLFile();
            fail("No exception is thrown for mapExecution with no Error Code");
        } catch (XMLException x) {
            assertTrue(x.getMessage().indexOf("No errorCode defined") != -1);
        } catch (Exception e) {
            fail("wrong exception thrown. XmlException expected, " + e.getClass() + " thrown");
        }
    }

    @Test
    public void testMapExceptionWithNoExceptionClass() throws Exception {
        resourceName = "mapException/mapExceptionNoExceptionClass.bpmn";

        BpmnModel bpmnModel = readXMLFile();
        FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement("servicetaskWithAndTrueAndChildren");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof ServiceTask);
        assertEquals("servicetaskWithAndTrueAndChildren", flowElement.getId());
        ServiceTask serviceTask = (ServiceTask) flowElement;
        assertNotNull(serviceTask.getMapExceptions());
        assertEquals(1, serviceTask.getMapExceptions().size());
        assertNotNull(serviceTask.getMapExceptions().get(0).getClassName());
        assertEquals(0, serviceTask.getMapExceptions().get(0).getClassName().length());

    }

    @Test
    public void convertXMLToModel() throws Exception {
        resourceName = "mapException/mapExceptionModel.bpmn";

        BpmnModel bpmnModel = readXMLFile();
        validateModel(bpmnModel);
    }

    private void validateModel(BpmnModel model) {

        // check service task with andChildren Set to True
        FlowElement flowElement = model.getMainProcess().getFlowElement("servicetaskWithAndTrueAndChildren");
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof ServiceTask);
        assertEquals("servicetaskWithAndTrueAndChildren", flowElement.getId());
        ServiceTask serviceTask = (ServiceTask) flowElement;
        assertNotNull(serviceTask.getMapExceptions());
        assertEquals(3, serviceTask.getMapExceptions().size());

        // check a normal mapException, with hasChildren == true
        assertEquals("myErrorCode1", serviceTask.getMapExceptions().get(0).getErrorCode());
        assertEquals("com.activiti.Something1", serviceTask.getMapExceptions().get(0).getClassName());
        assertTrue(serviceTask.getMapExceptions().get(0).isAndChildren());

        // check a normal mapException, with hasChildren == false
        assertEquals("myErrorCode2", serviceTask.getMapExceptions().get(1).getErrorCode());
        assertEquals("com.activiti.Something2", serviceTask.getMapExceptions().get(1).getClassName());
        assertFalse(serviceTask.getMapExceptions().get(1).isAndChildren());

        // check a normal mapException, with no hasChildren Defined, default
        // should
        // be false
        assertEquals("myErrorCode3", serviceTask.getMapExceptions().get(2).getErrorCode());
        assertEquals("com.activiti.Something3", serviceTask.getMapExceptions().get(2).getClassName());
        assertFalse(serviceTask.getMapExceptions().get(2).isAndChildren());

        // if no map exception is defined, getMapException should return a not
        // null
        // empty list
        FlowElement flowElement1 = model.getMainProcess().getFlowElement("servicetaskWithNoMapException");
        assertNotNull(flowElement1);
        assertTrue(flowElement1 instanceof ServiceTask);
        assertEquals("servicetaskWithNoMapException", flowElement1.getId());
        ServiceTask serviceTask1 = (ServiceTask) flowElement1;
        assertNotNull(serviceTask1.getMapExceptions());
        assertEquals(0, serviceTask1.getMapExceptions().size());

    }

}
