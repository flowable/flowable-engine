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
import org.flowable.dmn.model.DmnElementReference;
import org.flowable.dmn.model.ItemDefinition;
import org.junit.jupiter.api.Test;

public class Chapter11SingleDiagramTest extends AbstractConverterTest {

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
        return "chapter11singleDiagram.dmn";
    }

    private void validateModel(DmnDefinition model) {
        // assert that all root item definitions are present
        assertThat(model.getItemDefinitions()).as("item definitions")
            .hasSize(8);

        // assert that nested item components are present
        assertThat(model.getItemDefinitions()).as("item components")
            .filteredOn(id -> id.getItemComponents().size() > 0)
            .flatExtracting(ItemDefinition::getItemComponents)
            .filteredOn(ic -> ic.getItemComponents().size() > 0)
            .flatExtracting(ItemDefinition::getItemComponents)
            .extracting(ItemDefinition::getName)
            .containsExactly("Income", "Repayments", "Expenses");

        assertThat(model.getDecisions()).as("decisions")
            .filteredOn(dec -> dec.getVariable() != null)
            .extracting(dec -> dec.getVariable().getName())
            .containsExactly("Adjudication", "Bureau call type", "Strategy", "Eligibility", "Routing", "Pre-bureau affordability", "Post-bureau affordability",
                "Post-bureau risk category", "Pre-bureau risk category", "Application risk score", "Required monthly installment");

        assertThat(model.getDecisionServices()).as("decision services")
            .extracting(decService -> decService.getName())
            .containsExactly("Bureau Strategy Decision Service", "Routing Decision Service");

        assertThat(model.getDecisionServiceById("_7befd964-eefa-4d8f-908d-8f6ad8d22c67")
            .getOutputDecisions()).as("Decision Service - Output Decisions")
                .hasSize(2)
                .extracting(DmnElementReference::getHref)
                .containsExactly("#_8b838f06-968a-4c66-875e-f5412fd692cf", "#_5b8356f3-2cf2-40e8-8f80-324937e8b276");

        assertThat(model.getDecisionServiceById("_7befd964-eefa-4d8f-908d-8f6ad8d22c67")
            .getEncapsulatedDecisions()).as("Decision Service - Encapsulated Decisions")
                .hasSize(5)
                .extracting(DmnElementReference::getHref)
                .containsExactly("#_ed60265c-25e2-400f-a99f-fafd3b489838", "#_9997fcfd-0f50-4933-939e-88a235b5e2a0", "#_e905f02c-c5d9-4f2a-ba57-7912ff523b46",
                    "#_3c8cee68-99dd-418c-847d-0b54697354f2", "#_b5e759df-f662-44cd-94f5-55c3c81f0ee3");

        assertThat(model.getDecisionServiceById("_7befd964-eefa-4d8f-908d-8f6ad8d22c67")
            .getInputData()).as("Decision Service - Input Data")
            .hasSize(2)
            .extracting(DmnElementReference::getHref)
            .containsExactly("#_d14df033-f4a2-47e3-9590-84e9ff04db4e", "#_fe938494-ee59-425e-8728-2347ea703563");

        model.getDecisions().forEach(decision -> assertThat(model.getGraphicInfo(decision.getId())).isNotNull());

        model.getDecisions().stream()
            .forEach(decision -> decision.getRequiredDecisions().stream()
                .forEach(informationRequirement ->
                    assertThat(model.getFlowLocationGraphicInfo(informationRequirement.getId())).isNotNull()
                )
            );

        model.getDecisions().stream()
            .forEach(decision -> decision.getAuthorityRequirements().stream()
                .forEach(authorityRequirement ->
                    assertThat(model.getFlowLocationGraphicInfo(authorityRequirement.getId())).isNotNull()
                )
            );

        model.getDecisions().stream()
            .forEach(decision -> decision.getRequiredInputs().stream()
                .forEach(informationRequirement ->
                    assertThat(model.getFlowLocationGraphicInfo(informationRequirement.getId())).isNotNull()
                )
            );
    }
}