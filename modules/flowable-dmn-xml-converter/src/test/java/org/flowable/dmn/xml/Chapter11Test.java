package org.flowable.dmn.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElementReference;
import org.flowable.dmn.model.ItemDefinition;
import org.junit.jupiter.api.Test;

public class Chapter11Test extends AbstractConverterTest {

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
        return "chapter11.dmn";
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

        assertThat(model.getDiDiagrams().entrySet())
            .hasSize(6)
            .hasSameSizeAs(model.getLocationMap().entrySet())
            .hasSameSizeAs(model.getFlowLocationGraphicInfo().entrySet());

        assertThat(model.getFlowLocationGraphicInfo().entrySet())
            .hasSize(6);

        assertThat(model.getGraphicInfo("_ce4a4c00-c3a3-46a6-8938-055239f6b326"))
            .hasSize(30);

        assertThat(model.getFlowLocationGraphicInfo("_ce4a4c00-c3a3-46a6-8938-055239f6b326"))
            .hasSize(46);

        assertThat(model.getGraphicInfo("_0e22b6cf-0a6e-40e1-a81e-44b31ad86262"))
            .hasSize(21);

        assertThat(model.getFlowLocationGraphicInfo("_0e22b6cf-0a6e-40e1-a81e-44b31ad86262"))
            .hasSize(27);

        assertThat(model.getGraphicInfo("_3275163a-921d-48f8-967a-21c4373b1197"))
            .hasSize(18);

        assertThat(model.getFlowLocationGraphicInfo("_3275163a-921d-48f8-967a-21c4373b1197"))
            .hasSize(22);

        assertThat(model.getGraphicInfo("_a35ef6e9-0408-4288-b8f2-d28ac4baca3b"))
            .hasSize(6);

        assertThat(model.getFlowLocationGraphicInfo("_a35ef6e9-0408-4288-b8f2-d28ac4baca3b"))
            .hasSize(5);

        assertThat(model.getGraphicInfo("_5c111794-4c6b-4747-8dfc-99d2ad0b6313"))
            .hasSize(10);

        assertThat(model.getFlowLocationGraphicInfo("_5c111794-4c6b-4747-8dfc-99d2ad0b6313"))
            .hasSize(13);

        assertThat(model.getGraphicInfo("_69750f88-f46f-4b47-bb3c-fb77f574f2b3"))
            .hasSize(9);

        assertThat(model.getFlowLocationGraphicInfo("_69750f88-f46f-4b47-bb3c-fb77f574f2b3"))
            .hasSize(11);

        assertThat(model.getDecisionServiceDividerGraphicInfo("_5c111794-4c6b-4747-8dfc-99d2ad0b6313"))
            .hasSize(1);

        assertThat(model.getDecisionServiceDividerGraphicInfo("_69750f88-f46f-4b47-bb3c-fb77f574f2b3"))
            .hasSize(1);
    }

}