package org.flowable.dmn.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.assertj.core.groups.Tuple;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElementReference;
import org.flowable.dmn.model.GraphicInfo;
import org.flowable.dmn.model.ItemDefinition;
import org.junit.jupiter.api.Test;

public class DiagramDiTest extends AbstractConverterTest {

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
        return "dmndi.dmn";
    }

    private void validateModel(DmnDefinition model) {

        // validate divider to contain height and width (need for JSON conversion)
        assertThat(model.getDecisionServiceDividerGraphicInfo("decisionServiceTest"))
            .extracting(
                GraphicInfo::getX,
                GraphicInfo::getY,
                GraphicInfo::getWidth,
                GraphicInfo::getHeight)
            .as("x, y, width, height")
            .containsExactly(
                tuple(30.0, 285.0, 729.0, 240.0),
                tuple(759.0, 285.0, 729.0, 330.0)
            );
    }
}