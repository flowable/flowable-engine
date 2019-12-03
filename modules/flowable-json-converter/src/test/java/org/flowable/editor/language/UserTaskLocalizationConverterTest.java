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
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.UserTask;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UserTaskLocalizationConverterTest extends AbstractConverterTest {

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

    protected String getResource() {
        return "test.usertaskmodel.json";
    }

    private void validateModel(BpmnModel model) {
        final UserTask userTask = model.getMainProcess().findFlowElementsOfType(UserTask.class).get(0);
        final List<ExtensionElement> localization = userTask.getExtensionElements().get("localization");
        assertNotNull(localization);
        assertEquals(localization.size(), 3);
        boolean englishLocalePresent = false, spanishLocalePresent = false, persianLocalePersent = false;
        for (ExtensionElement extensionElement : localization) {
            final String locale = extensionElement.getAttributeValue(null, "locale");
            final String name = extensionElement.getAttributeValue(null, "name");
            final List<ExtensionElement> docs = extensionElement.getChildElements().get("documentation");
            assertNotNull(name);
            assertNotNull(docs);
            assertEquals(docs.size(), 1);
            if (locale.equals("en")) {
                assertEquals(name, "registration");
                assertEquals(docs.get(0).getElementText(), "registration description");
                englishLocalePresent = true;
            }
            if (locale.equals("fa")) {
                assertEquals(name, "ثبت نام");
                assertEquals(docs.get(0).getElementText(), "توضیحات ثبت نام");
                persianLocalePersent = true;
            }
            if (locale.equals("es")){
                assertEquals(name, "registro");
                assertEquals(docs.get(0).getElementText(), "descripción de registro");
                spanishLocalePresent = true;
            }
        }
        assertTrue(englishLocalePresent);
        assertTrue(spanishLocalePresent);
        assertTrue(persianLocalePersent);
    }
}
