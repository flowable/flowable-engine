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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.bpmn.exceptions.XMLException;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.MapExceptionEntry;
import org.flowable.bpmn.model.ServiceTask;
import org.junit.jupiter.api.Test;

public class MapExceptionConverterTest extends AbstractConverterTest {

    String resourceName;

    @Override
    protected String getResource() {
        return resourceName;
    }

    @Test
    public void testMapExceptionWithInvalidHasChildren() throws Exception {
        resourceName = "mapException/mapExceptionInvalidHasChildrenModel.bpmn";
        assertThatThrownBy(() -> readXMLFile())
                .isExactlyInstanceOf(XMLException.class)
                .hasMessageContaining("is not valid boolean");
    }

    @Test
    public void testMapExceptionWithNoErrorCode() throws Exception {
        resourceName = "mapException/mapExceptionNoErrorCode.bpmn";
        assertThatThrownBy(() -> readXMLFile())
                .isExactlyInstanceOf(XMLException.class)
                .hasMessageContaining("No errorCode defined mapException with errorCode=null");
    }

    @Test
    public void testMapExceptionWithNoExceptionClass() throws Exception {
        resourceName = "mapException/mapExceptionNoExceptionClass.bpmn";

        BpmnModel bpmnModel = readXMLFile();
        FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement("servicetaskWithAndTrueAndChildren");
        assertThat(flowElement)
                .isInstanceOfSatisfying(ServiceTask.class, serviceTask -> {
                    assertThat(serviceTask.getId()).isEqualTo("servicetaskWithAndTrueAndChildren");
                    assertThat(serviceTask.getMapExceptions())
                            .extracting(MapExceptionEntry::getClassName)
                            .containsExactly("");
                });
    }

    @Test
    public void convertXMLToModel() throws Exception {
        resourceName = "mapException/mapExceptionModel.bpmn";

        BpmnModel bpmnModel = readXMLFile();
        BpmnModel parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    protected void validateModel(BpmnModel model) {

        // check service task with andChildren Set to True
        FlowElement flowElement = model.getMainProcess().getFlowElement("servicetaskWithAndTrueAndChildren");
        assertThat(flowElement)
                .isInstanceOfSatisfying(ServiceTask.class, serviceTask -> {
                    assertThat(serviceTask.getId()).isEqualTo("servicetaskWithAndTrueAndChildren");
                    assertThat(serviceTask.getMapExceptions()).isNotNull();
                    assertThat(serviceTask.getMapExceptions())
                            .extracting(MapExceptionEntry::getErrorCode, MapExceptionEntry::getClassName, 
                                    MapExceptionEntry::getRootCause, MapExceptionEntry::isAndChildren)
                            .containsExactly(
                                    // check a normal mapException, with hasChildren == true
                                    tuple("myErrorCode1", "com.activiti.Something1", null, true),
                                    // check a normal mapException, with hasChildren == false
                                    tuple("myErrorCode2", "com.activiti.Something2", null, false),
                                    // check a normal mapException, with no hasChildren Defined, default should be false
                                    tuple("myErrorCode3", "com.activiti.Something3", "org.flowable.Exception", false)
                            );
                });

        // if no map exception is defined, getMapException should return a not null empty list
        FlowElement flowElement1 = model.getMainProcess().getFlowElement("servicetaskWithNoMapException");
        assertThat(flowElement1)
                .isInstanceOfSatisfying(ServiceTask.class, serviceTask -> {
                    assertThat(serviceTask.getId()).isEqualTo("servicetaskWithNoMapException");
                    assertThat(serviceTask.getMapExceptions()).isEmpty();
                });
    }
}
