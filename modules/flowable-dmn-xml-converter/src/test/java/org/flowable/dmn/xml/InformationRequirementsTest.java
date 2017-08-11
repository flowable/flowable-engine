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
import org.flowable.dmn.model.DmnDefinition;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Yvo Swillens
 */
public class InformationRequirementsTest extends AbstractConverterTest {

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

    protected String getResource() {
        return "informationRequirements.dmn";
    }

    private void validateModel(DmnDefinition model) {
        List<Decision> decisions = model.getDecisions();
        assertEquals(3, decisions.size());

        Decision firstDecision = decisions.get(0);
        Decision secondDecision = decisions.get(1);
        Decision thirdDecision = decisions.get(2);
        assertNotNull(firstDecision);

        Assert.assertEquals(2, firstDecision.getInformationRequirements().size());
        Assert.assertEquals("#DecisionTable2", firstDecision.getInformationRequirements().get(0).getRequiredDecision().getHref());
        Assert.assertEquals("#DecisionTable3", firstDecision.getInformationRequirements().get(1).getRequiredDecision().getHref());

        Assert.assertEquals(0, secondDecision.getInformationRequirements().size());
        Assert.assertEquals(0, thirdDecision.getInformationRequirements().size());
    }
}
