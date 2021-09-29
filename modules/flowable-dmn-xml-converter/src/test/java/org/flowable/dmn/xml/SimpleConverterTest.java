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
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.OutputClause;
import org.junit.jupiter.api.Test;

public class SimpleConverterTest extends AbstractConverterTest {

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
        return "simple.dmn";
    }

    private void validateModel(DmnDefinition model) {
        List<Decision> decisions = model.getDecisions();
        assertThat(decisions)
                .extracting(Decision::isForceDMN11)
                .containsExactly(false);

        DecisionTable decisionTable = (DecisionTable) decisions.get(0).getExpression();
        assertThat(decisionTable).isNotNull();

        List<InputClause> inputClauses = decisionTable.getInputs();
        assertThat(inputClauses).hasSize(3);

        assertThat(inputClauses.get(0).getInputValues().getTextValues())
                .containsOnly("val1", "val2");
        assertThat(inputClauses.get(0).getInputValues().getText()).isEqualTo("\"val1\",\"val2\"");

        assertThat(inputClauses.get(1).getInputValues().getTextValues())
                .containsOnly("10", "20");
        assertThat(inputClauses.get(1).getInputValues().getText()).isEqualTo("10,20");

        assertThat(inputClauses.get(2).getInputValues()).isNull();

        List<OutputClause> outputClauses = decisionTable.getOutputs();
        assertThat(outputClauses).hasSize(1);

        assertThat(outputClauses.get(0).getOutputValues().getTextValues())
                .containsOnly("val1", "val2");
        assertThat(outputClauses.get(0).getOutputValues().getText()).isEqualTo("\"val1\",\"val2\"");
    }
}
