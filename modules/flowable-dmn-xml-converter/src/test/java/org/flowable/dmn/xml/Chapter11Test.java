package org.flowable.dmn.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.dmn.model.DmnDefinition;
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

    }

}