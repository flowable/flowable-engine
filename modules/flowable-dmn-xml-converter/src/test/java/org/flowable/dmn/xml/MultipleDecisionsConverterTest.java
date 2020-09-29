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
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.OutputClause;
import org.junit.jupiter.api.Test;

public class MultipleDecisionsConverterTest extends AbstractConverterTest {

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
        return "multiple_decisions.dmn";
    }

    private void validateModel(DmnDefinition model) {
        List<Decision> decisions = model.getDecisions();
        assertThat(decisions).hasSize(2);

        DecisionTable decisionTable1 = (DecisionTable) decisions.get(0).getExpression();
        assertThat(decisionTable1).isNotNull();

        List<InputClause> inputClauses1 = decisionTable1.getInputs();
        assertThat(inputClauses1).hasSize(2);

        List<OutputClause> outputClauses1 = decisionTable1.getOutputs();
        assertThat(outputClauses1).hasSize(1);

        List<DecisionRule> rules1 = decisionTable1.getRules();
        assertThat(rules1).hasSize(2);

        DecisionTable decisionTable2 = (DecisionTable) decisions.get(1).getExpression();
        assertThat(decisionTable2).isNotNull();

        List<InputClause> inputClauses2 = decisionTable2.getInputs();
        assertThat(inputClauses2).hasSize(2);

        List<OutputClause> outputClauses2 = decisionTable2.getOutputs();
        assertThat(outputClauses2).hasSize(1);

        List<DecisionRule> rules2 = decisionTable2.getRules();
        assertThat(rules2).hasSize(2);
    }
}
