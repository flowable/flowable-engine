package org.flowable.dmn.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.filter;

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.InformationItem;
import org.flowable.dmn.model.InputData;
import org.junit.jupiter.api.Test;

public class DishExampleTest extends AbstractConverterTest {

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
        return "dishExample.dmn";
    }

    private void validateModel(DmnDefinition model) {
        assertThat(model.getInputData()).as("input data")
            .extracting(InputData::getVariable)
            .extracting(InformationItem::getId)
            .containsExactly("dayType_ii", "temperature_ii");

        assertThat(model.getDecisions()).as("decisions")
            .extracting(DmnElement::getId)
            .containsExactly("dish", "season", "guestCount");

        assertThat(model.getDecisions()).as("decisions")
            .flatExtracting(Decision::getInformationRequirements)
            .filteredOn(ir -> ir.getRequiredDecision() != null)
            .extracting(ir -> ir.getRequiredDecision().getHref())
            .containsExactly("#season", "#guestCount");

        assertThat(model.getDecisions()).as("decisions")
            .flatExtracting(Decision::getInformationRequirements)
            .filteredOn(ir -> ir.getRequiredInput() != null)
            .extracting(ir -> ir.getRequiredInput().getHref())
            .containsExactly("#temperature_id", "#dayType_id");

        assertThat(model.getDecisions()).as("decisions")
            .flatExtracting(Decision::getAuthorityRequirements)
            .filteredOn(ar -> ar.getRequiredAuthority() != null)
            .extracting(ar -> ar.getRequiredAuthority().getHref())
            .containsExactly("#host_ks");
    }

}
