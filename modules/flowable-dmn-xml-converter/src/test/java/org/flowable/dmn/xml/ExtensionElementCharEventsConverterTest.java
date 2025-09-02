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
package org.flowable.dmn.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnExtensionElement;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.OutputClause;
import org.junit.jupiter.api.Test;

public class ExtensionElementCharEventsConverterTest extends AbstractConverterTest {

    protected static final String YOURCO_EXTENSIONS_NAMESPACE = "http://yourco/bpmn";
    protected static final String YOURCO_EXTENSIONS_PREFIX = "yourco";

    protected static final String ELEMENT_ATTRIBUTES = "attributes";
    protected static final String ELEMENT_ATTRIBUTE = "attribute";

    protected static final String ELEMENT_I18N = "i18n";

    @Test
    public void convertXMLToModel() throws Exception {
        DmnDefinition definition = readXMLFile();
        validateModel(definition);
    }

    @Test
    public void convertModelToXML() throws Exception {
        DmnDefinition bpmnModel = readXMLFile();
        DmnDefinition parsedModel = exportAndReadXMLFile(bpmnModel);
        validateModel(parsedModel);
    }

    @Override
    protected String getResource() {
        return "extensionElementsCharEvents.dmn";
    }

    private void validateModel(DmnDefinition model) {
        assertThat(model.getDescription()).isEqualTo("DMN description");

        /*
         * Verify localization extension
         */
        List<DmnExtensionElement> i18nExtension = model.getExtensionElements().get(ELEMENT_I18N);
        assertThat(i18nExtension.get(0).getElementText()).isEqualTo("a&b&c&d&e&f&g");

        List<Decision> decisions = model.getDecisions();
        assertThat(decisions).hasSize(1);

        DecisionTable decisionTable = (DecisionTable) decisions.get(0).getExpression();
        assertThat(decisionTable).isNotNull();

        assertThat(decisionTable.getDescription()).isEqualTo("Decision table description");

        /*
         * Verify decision table localization extension
         */
        i18nExtension = decisionTable.getExtensionElements().get(ELEMENT_I18N);
        assertThat(i18nExtension.get(0).getElementText()).isEqualTo("a&b&c&d&e&f&g");

        List<InputClause> inputClauses = decisionTable.getInputs();
        assertThat(inputClauses).hasSize(4);

        List<OutputClause> outputClauses = decisionTable.getOutputs();
        assertThat(outputClauses).hasSize(1);

        /*
         * Verify input entry extension elements
         */
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(3).getInputEntry().getExtensionElements().get("operator").get(0).getElementText()).isEqualTo("NONE OF");
        assertThat(decisionTable.getRules().get(0).getInputEntries().get(3).getInputEntry().getExtensionElements().get("expression").get(0).getElementText()).isEqualTo("20, 13");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(3).getInputEntry().getExtensionElements().get("operator").get(0).getElementText()).isEqualTo("ANY OF");
        assertThat(decisionTable.getRules().get(1).getInputEntries().get(3).getInputEntry().getExtensionElements().get("expression").get(0).getElementText()).isEqualTo("\"20\", \"13\"");
        assertThat(decisionTable.getRules().get(2).getInputEntries().get(3).getInputEntry().getExtensionElements().get("operator").get(0).getElementText()).isEqualTo("ALL OF");
        assertThat(decisionTable.getRules().get(2).getInputEntries().get(3).getInputEntry().getExtensionElements().get("expression").get(0).getElementText()).isEqualTo("20");
    }
}
