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

import org.flowable.dmn.model.BuiltinAggregator;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.OutputClause;
import org.junit.jupiter.api.Test;

public class AdditionalConverterTest extends AbstractConverterTest {

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

    @Test
    public void exporterAndVersion() throws Exception {
        DmnDefinition dmnModel = readXMLFile();
        dmnModel.setExporter("Flowable");
        dmnModel.setExporterVersion("latest");
        DmnDefinition parsedModel = exportAndReadXMLFile(dmnModel);
        validateModel(parsedModel);
        assertThat(parsedModel.getExporter()).isEqualTo("Flowable");
        assertThat(parsedModel.getExporterVersion()).isEqualTo("latest");
    }

    @Override
    protected String getResource() {
        return "full.dmn";
    }

    private void validateModel(DmnDefinition model) {
        List<Decision> decisions = model.getDecisions();
        assertThat(decisions).hasSize(1);

        DecisionTable decisionTable = (DecisionTable) decisions.get(0).getExpression();
        assertThat(decisionTable).isNotNull();

        assertThat(decisionTable.getHitPolicy()).isEqualTo(HitPolicy.COLLECT);
        assertThat(decisionTable.getAggregation()).isEqualTo(BuiltinAggregator.SUM);

        List<InputClause> inputClauses = decisionTable.getInputs();
        assertThat(inputClauses).hasSize(2);
        assertThat(inputClauses.get(0).getId()).isNotNull();
        assertThat(inputClauses.get(0).getLabel()).isNotNull();
        assertThat(inputClauses.get(0).getInputExpression().getTypeRef()).isNotNull();
        assertThat(inputClauses.get(0).getInputExpression().getId()).isNotNull();
        assertThat(inputClauses.get(0).getInputExpression().getText()).isNotNull();

        List<OutputClause> outputClauses = decisionTable.getOutputs();
        assertThat(outputClauses).hasSize(2);
        assertThat(outputClauses.get(0).getName()).isNotNull();
        assertThat(outputClauses.get(0).getTypeRef()).isNotNull();
        assertThat(outputClauses.get(0).getId()).isNotNull();
        assertThat(outputClauses.get(0).getLabel()).isNotNull();

        assertThat(decisionTable.getOutputs())
                .extracting(outputs -> outputs.getOutputValues().getText())
                .containsExactly("\"result2\",\"result1\"", "2,1");

        List<DecisionRule> rules = decisionTable.getRules();
        assertThat(rules).hasSize(2);

        assertThat(rules.get(0).getInputEntries().get(0).getInputClause().getInputExpression().getTypeRef()).isNotNull();
        assertThat(rules.get(0).getInputEntries().get(0).getInputClause().getInputExpression().getText()).isNotNull();
        assertThat(rules.get(0).getOutputEntries().get(0).getOutputClause().getTypeRef()).isNotNull();
        assertThat(rules.get(0).getOutputEntries().get(0).getOutputClause().getName()).isNotNull();
        assertThat(rules.get(0).getOutputEntries().get(1).getOutputClause().getTypeRef()).isNotNull();
        assertThat(rules.get(0).getOutputEntries().get(1).getOutputClause().getName()).isNotNull();
    }
}
