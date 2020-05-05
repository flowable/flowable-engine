package org.flowable.dmn.xml.converter;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.InformationRequirement;

/**
 * @author Yvo Swillens
 */
public class InformationRequirementConverter extends BaseDmnXMLConverter {

    @Override
    protected String getXMLElementName() {
        return ELEMENT_INFORMATION_REQUIREMENT;
    }

    @Override
    protected DmnElement convertXMLToElement(XMLStreamReader xtr, ConversionHelper conversionHelper) throws Exception {
        InformationRequirement informationRequirement = new InformationRequirement();
        informationRequirement.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));
        informationRequirement.setName(xtr.getAttributeValue(null, ATTRIBUTE_NAME));
        parseChildElements(getXMLElementName(), informationRequirement, conversionHelper.getCurrentDecision(), xtr);

        return informationRequirement;
    }

    @Override
    protected void writeAdditionalAttributes(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }

    @Override
    protected void writeAdditionalChildElements(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }
}
