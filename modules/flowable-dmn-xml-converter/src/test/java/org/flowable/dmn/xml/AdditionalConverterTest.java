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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.flowable.dmn.model.BuiltinAggregator;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.OutputClause;
import org.junit.Assert;
import org.junit.Test;

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

    @Override
    protected String getResource() {
        return "full.dmn";
    }

    private void validateModel(DmnDefinition model) {
        List<Decision> decisions = model.getDecisions();
        assertEquals(1, decisions.size());

        DecisionTable decisionTable = (DecisionTable) decisions.get(0).getExpression();
        assertNotNull(decisionTable);

        Assert.assertEquals(HitPolicy.COLLECT, decisionTable.getHitPolicy());
        Assert.assertEquals(BuiltinAggregator.SUM, decisionTable.getAggregation());

        List<InputClause> inputClauses = decisionTable.getInputs();
        assertEquals(2, inputClauses.size());
        assertNotNull(inputClauses.get(0).getId());
        assertNotNull(inputClauses.get(0).getLabel());
        assertNotNull(inputClauses.get(0).getInputExpression().getTypeRef());
        assertNotNull(inputClauses.get(0).getInputExpression().getId());
        assertNotNull(inputClauses.get(0).getInputExpression().getText());

        List<OutputClause> outputClauses = decisionTable.getOutputs();
        assertEquals(2, outputClauses.size());
        assertNotNull(outputClauses.get(0).getName());
        assertNotNull(outputClauses.get(0).getTypeRef());
        assertNotNull(outputClauses.get(0).getId());
        assertNotNull(outputClauses.get(0).getLabel());

        assertEquals("\"result2\",\"result1\"", decisionTable.getOutputs().get(0).getOutputValues().getText());
        assertEquals("\"2\",\"1\"", decisionTable.getOutputs().get(1).getOutputValues().getText());

        List<DecisionRule> rules = decisionTable.getRules();
        assertEquals(2, rules.size());

        assertNotNull(rules.get(0).getInputEntries().get(0).getInputClause().getInputExpression().getTypeRef());
        assertNotNull(rules.get(0).getInputEntries().get(0).getInputClause().getInputExpression().getText());
        assertNotNull(rules.get(0).getOutputEntries().get(0).getOutputClause().getTypeRef());
        assertNotNull(rules.get(0).getOutputEntries().get(0).getOutputClause().getName());
        assertNotNull(rules.get(0).getOutputEntries().get(1).getOutputClause().getTypeRef());
        assertNotNull(rules.get(0).getOutputEntries().get(1).getOutputClause().getName());
    }
}
