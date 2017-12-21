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

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.OutputClause;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        assertEquals(2, decisions.size());

        DecisionTable decisionTable1 = (DecisionTable) decisions.get(0).getExpression();
        assertNotNull(decisionTable1);

        List<InputClause> inputClauses1 = decisionTable1.getInputs();
        assertEquals(2, inputClauses1.size());

        List<OutputClause> outputClauses1 = decisionTable1.getOutputs();
        assertEquals(1, outputClauses1.size());

        List<DecisionRule> rules1 = decisionTable1.getRules();
        assertEquals(2, rules1.size());

        DecisionTable decisionTable2 = (DecisionTable) decisions.get(1).getExpression();
        assertNotNull(decisionTable2);

        List<InputClause> inputClauses2 = decisionTable2.getInputs();
        assertEquals(2, inputClauses2.size());

        List<OutputClause> outputClauses2 = decisionTable2.getOutputs();
        assertEquals(1, outputClauses2.size());

        List<DecisionRule> rules2 = decisionTable2.getRules();
        assertEquals(2, rules2.size());
    }
}
