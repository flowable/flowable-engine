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
package org.flowable.editor.form.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.flowable.form.model.FormField;
import org.flowable.form.model.SimpleFormModel;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
public class FormJsonConverterTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormJsonConverterTest.class);

    private static final String JSON_RESOURCE_1 = "org/flowable/editor/form/converter/form_1.json";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testSimpleJsonForm() throws Exception {

        String testJsonResource = readJsonToString(JSON_RESOURCE_1);
        SimpleFormModel formModel = new FormJsonConverter().convertToFormModel(testJsonResource);

        assertThat(formModel).isNotNull();
        assertThat(formModel.getFields()).hasSize(1);

        FormField formField = formModel.getFields().get(0);
        assertThat(formField.getId()).isEqualTo("input1");
        assertThat(formField.getName()).isEqualTo("Input1");
        assertThat(formField.getType()).isEqualTo("text");
        assertThat(formField.isRequired()).isFalse();
        assertThat(formField.getPlaceholder()).isEqualTo("empty");
    }

    /* Helper methods */
    protected String readJsonToString(String resource) {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(resource)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("Could not read " + resource + " : " + e.getMessage());
            return null;
        }
    }

    protected JsonNode parseJson(String resource) {
        String jsonString = readJsonToString(resource);
        try {
            return objectMapper.readTree(jsonString);
        } catch (IOException e) {
            fail("Could not parse " + resource + " : " + e.getMessage());
        }
        return null;
    }
}
