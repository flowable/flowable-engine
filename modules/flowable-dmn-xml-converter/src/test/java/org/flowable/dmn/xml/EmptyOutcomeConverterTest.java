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

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.OutputClause;
import org.junit.Test;

public class EmptyOutcomeConverterTest extends AbstractConverterTest {

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
        return "empty_outcomes.dmn";
    }

    private void validateModel(DmnDefinition model) {
        List<Decision> decisions = model.getDecisions();
        assertEquals(1, decisions.size());

        DecisionTable decisionTable = (DecisionTable) decisions.get(0).getExpression();
        assertNotNull(decisionTable);

        assertEquals(2, decisionTable.getRules().get(0).getOutputEntries().size());
        assertEquals(2, decisionTable.getRules().get(1).getOutputEntries().size());
    }
}
