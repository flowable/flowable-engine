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

import org.flowable.dmn.model.DmnDefinition;
import org.junit.jupiter.api.Test;

public class DiagramDiNoEdgesTest extends AbstractConverterTest {

    @Test
    public void convertXMLToModel() throws Exception {
        DmnDefinition definition = readXMLFile();
        validateModel(definition);
    }

    @Test
    public void convertModelToXML() throws Exception {
        DmnDefinition dmnModel = readXMLFile();
        DmnDefinition parsedModel = exportAndReadXMLFile(dmnModel);
        validateModel(parsedModel);
    }

    @Override
    protected String getResource() {
        return "dmndiNoEdges.dmn";
    }

    private void validateModel(DmnDefinition model) {

        assertThat(model.getFlowLocationMap()).isEmpty();
        assertThat(model.getFlowLocationMapByDiagramId("DMNDiagram_decisionServiceTest")).isNull();

        assertThat(model.getLocationMap()).containsOnlyKeys("decisionServiceTest", "decision1", "decision2");
        assertThat(model.getLocationMapByDiagramId("DMNDiagram_decisionServiceTest")).containsOnlyKeys("decisionServiceTest", "decision1", "decision2");

        assertThat(model.getDecisionServiceDividerLocationMap()).containsOnlyKeys("decisionServiceTest");
        assertThat(model.getDecisionServiceDividerLocationMapByDiagramId("DMNDiagram_decisionServiceTest")).containsOnlyKeys("decisionServiceTest");
    }

}