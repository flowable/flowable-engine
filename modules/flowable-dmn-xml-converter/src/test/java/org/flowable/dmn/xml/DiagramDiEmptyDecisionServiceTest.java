package org.flowable.dmn.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.dmn.model.DmnDefinition;
import org.junit.jupiter.api.Test;

public class DiagramDiEmptyDecisionServiceTest extends AbstractConverterTest {

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
        return "dmndiEmptyDecisionService.dmn";
    }

    private void validateModel(DmnDefinition model) {

        assertThat(model.getFlowLocationMap()).isEmpty();
        assertThat(model.getFlowLocationMapByDiagramId("DMNDiagram_decisionServiceTest")).isNull();

        assertThat(model.getLocationMap()).containsOnlyKeys("decisionServiceTest");
        assertThat(model.getLocationMapByDiagramId("DMNDiagram_decisionServiceTest")).containsOnlyKeys("decisionServiceTest");

        assertThat(model.getDecisionServiceDividerLocationMap()).containsOnlyKeys("decisionServiceTest");
        assertThat(model.getDecisionServiceDividerLocationMapByDiagramId("DMNDiagram_decisionServiceTest")).containsOnlyKeys("decisionServiceTest");
    }

}