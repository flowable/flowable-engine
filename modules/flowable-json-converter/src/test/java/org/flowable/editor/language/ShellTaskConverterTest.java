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

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ShellTaskConverterTest extends AbstractConverterTest {

    private static final Set<String> EXPECTED_FIELDS = new HashSet<>();

    static {
        EXPECTED_FIELDS.add("command");
        EXPECTED_FIELDS.add("arg1");
        EXPECTED_FIELDS.add("arg2");
        EXPECTED_FIELDS.add("arg3");
        EXPECTED_FIELDS.add("arg4");
        EXPECTED_FIELDS.add("arg5");
        EXPECTED_FIELDS.add("wait");
        EXPECTED_FIELDS.add("errorRedirect");
        EXPECTED_FIELDS.add("errorCodeVariable");
        EXPECTED_FIELDS.add("directory");
        EXPECTED_FIELDS.add("outputVariable");
        EXPECTED_FIELDS.add("cleanEnv");
    }

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
        return "test.shelltaskmodel.json";
    }

    private void validateModel(BpmnModel model) {
        FlowElement flowElement = model.getMainProcess().getFlowElement("servicetask", true);
        assertNotNull(flowElement);
        assertTrue(flowElement instanceof ServiceTask);
        assertEquals("servicetask", flowElement.getId());
        ServiceTask serviceTask = (ServiceTask) flowElement;
        assertEquals("servicetask", serviceTask.getId());
        assertEquals("Shell", serviceTask.getName());

        List<FieldExtension> fields = serviceTask.getFieldExtensions();
        Collection<String> expectedField = new HashSet<>(EXPECTED_FIELDS);
        for (FieldExtension field : fields) {
            assertTrue(expectedField.contains(field.getFieldName()));
            assertTrue(expectedField.contains(field.getStringValue()));
            assertThat(field.getStringValue(), is(field.getFieldName()));
            expectedField.remove(field.getFieldName());
        }
        assertThat(expectedField.size(), is(0));
    }
}
