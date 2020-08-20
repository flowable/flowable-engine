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
package org.flowable.dmn.editor.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Yvo Swillens
 */
public class DmnJsonConverterUtilTest {

    private static final String JSON_RESOURCE_1 = "org/flowable/dmn/editor/converter/decisiontable_regression_model_v1.json";
    private static final String JSON_RESOURCE_2 = "org/flowable/dmn/editor/converter/decisiontable_regression_model_v2.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void migrateV1ToV3() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_1);

        boolean wasMigrated = new DmnJsonConverterUtil().migrateModelV3(testJsonResource, OBJECT_MAPPER);

        assertThat(wasMigrated).isTrue();
        assertThat(testJsonResource.get("modelVersion").asText()).isEqualTo("3");

        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("== \"TEST\"");
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(1).getInputEntry().getText()).isEqualTo("== 100");
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(2).getInputEntry().getText()).isEqualTo("== true");
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(3).getInputEntry().getText()).isEqualTo("== date:toDate('2017-06-01')");

        assertThat(decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("!= \"TEST\"");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(1).getInputEntry().getText()).isEqualTo("!= 100");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(2).getInputEntry().getText()).isEqualTo("== false");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(3).getInputEntry().getText()).isEqualTo("!= date:toDate('2017-06-01')");

        assertThat(decisionTable.getRules().get(2).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(2).getInputEntries().get(1).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(2).getInputEntries().get(2).getInputEntry().getText()).isEqualTo("-");
        assertThat(decisionTable.getRules().get(2).getInputEntries().get(3).getInputEntry().getText()).isEqualTo("-");
    }

    @Test
    public void migrateV2ToV3() {
        JsonNode testJsonResource = parseJson(JSON_RESOURCE_2);

        boolean wasMigrated = new DmnJsonConverterUtil().migrateModelV3(testJsonResource, OBJECT_MAPPER);

        assertThat(wasMigrated).isTrue();
        assertThat(testJsonResource.get("modelVersion").asText()).isEqualTo("3");

        DmnDefinition dmnDefinition = new DmnJsonConverter().convertToDmn(testJsonResource, "abc", 1, new Date());
        DecisionTable decisionTable = (DecisionTable) dmnDefinition.getDecisions().get(0).getExpression();

        assertThat(decisionTable.getRules().get(0).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:noneOf(collection1, '\"TEST1\",\"TEST2\"')}");
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(1).getInputEntry().getText()).isEqualTo("${collection:noneOf('\"TEST1\",\"TEST2\"', input2)}");

        assertThat(decisionTable.getRules().get(1).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:notAllOf(collection1, '\"TEST1\",\"TEST5\"')}");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(1).getInputEntry().getText()).isEqualTo("${collection:noneOf('\"TEST1\",\"TEST5\"', input2)}");

        assertThat(decisionTable.getRules().get(2).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:anyOf(collection1, '\"TEST1\",\"TEST6\"')}");
        assertThat(decisionTable.getRules().get(2).getInputEntries().get(1).getInputEntry().getText()).isEqualTo("${collection:allOf('\"TEST1\",\"TEST6\"', input2)}");

        assertThat(decisionTable.getRules().get(3).getInputEntries().get(0).getInputEntry().getText()).isEqualTo("${collection:allOf(collection1, \"TEST1\")}");
        assertThat(decisionTable.getRules().get(3).getInputEntries().get(1).getInputEntry().getText()).isEqualTo("${collection:allOf('\"TEST1\",\"TEST6\"', input2)}");
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
            return OBJECT_MAPPER.readTree(jsonString);
        } catch (IOException e) {
            fail("Could not parse " + resource + " : " + e.getMessage());
        }
        return null;
    }
}
