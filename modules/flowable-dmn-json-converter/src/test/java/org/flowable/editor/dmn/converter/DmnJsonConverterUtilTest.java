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
package org.flowable.editor.dmn.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Yvo Swillens
 */
public class DmnJsonConverterUtilTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmnJsonConverterTest.class);

    private static final String JSON_RESOURCE_1 = "org/flowable/editor/dmn/converter/decisiontable_regression_model_v2.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void migrateV2ToV3() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_1);

        testJsonResource = new DmnJsonConverterUtil().migrateModelV3(testJsonResource, OBJECT_MAPPER);

        assertEquals("3", testJsonResource.get("modelVersion").asText());

        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertEquals("${collection:noneOf(collection1, '\"TEST1\",\"TEST2\"')}",  decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:noneOf('\"TEST1\",\"TEST2\"', input2)}",  decisionTable.getRules().get(0).getInputEntries().get(1).getInputEntry().getText());

        assertEquals("${collection:notAllOf(collection1, '\"TEST1\",\"TEST5\"')}",  decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:noneOf('\"TEST1\",\"TEST5\"', input2)}",  decisionTable.getRules().get(1).getInputEntries().get(1).getInputEntry().getText());

        assertEquals("${collection:anyOf(collection1, '\"TEST1\",\"TEST6\"')}",  decisionTable.getRules().get(2).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:allOf('\"TEST1\",\"TEST6\"', input2)}",  decisionTable.getRules().get(2).getInputEntries().get(1).getInputEntry().getText());

        assertEquals("${collection:allOf(collection1, \"TEST1\")}",  decisionTable.getRules().get(3).getInputEntries().get(0).getInputEntry().getText());
        assertEquals("${collection:allOf('\"TEST1\",\"TEST6\"', input2)}",  decisionTable.getRules().get(3).getInputEntries().get(1).getInputEntry().getText());
    }


    /* Helper methods */
    protected String readJsonToString(String resource) {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(resource)) {
            return IOUtils.toString(is);
        } catch (IOException e) {
            fail("Could not read " + resource + " : " + e.getMessage());
            return null;
        }
    }

    protected JsonNode parseJson(String resource) {
        String jsonString = readJsonToString(resource);
        try {
            return OBJECT_MAPPER.readTree(jsonString);
        } catch (IOException e) {
            fail("Could not parse " + resource + " : " + e.getMessage());
        }
        return null;
    }
}
