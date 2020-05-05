package org.flowable.dmn.xml.converter;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.flowable.dmn.model.AuthorityRequirement;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;

/**
 * @author Yvo Swillens
 */
public class AuthorityRequirementConverter extends BaseDmnXMLConverter {

    @Override
    protected String getXMLElementName() {
        return ELEMENT_AUTHORITY_REQUIREMENT;
    }

    @Override
    protected DmnElement convertXMLToElement(XMLStreamReader xtr, ConversionHelper conversionHelper) throws Exception {
        AuthorityRequirement authorityRequirement = new AuthorityRequirement();
        authorityRequirement.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));
        authorityRequirement.setName(xtr.getAttributeValue(null, ATTRIBUTE_NAME));
        parseChildElements(getXMLElementName(), authorityRequirement, conversionHelper.getCurrentDecision(), xtr);

        return authorityRequirement;
    }

    @Override
    protected void writeAdditionalAttributes(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }

    @Override
    protected void writeAdditionalChildElements(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }
}
