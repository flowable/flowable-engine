package org.flowable.dmn.xml.converter;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.InformationItem;

public class VariableXMLConverter extends BaseDmnXMLConverter {

    @Override
    public Class<? extends DmnElement> getDmnElementType() {
        return InformationItem.class;
    }

    @Override
    protected String getXMLElementName() {
        return ELEMENT_VARIABLE;
    }

    @Override
    protected DmnElement convertXMLToElement(XMLStreamReader xtr, DmnDefinition model, Decision decision) throws Exception {
        InformationItem variable = new InformationItem();
        variable.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));
        variable.setName(xtr.getAttributeValue(null, ATTRIBUTE_NAME));
        variable.setTypeRef(xtr.getAttributeValue(null, ATTRIBUTE_TYPE_REF));
        return variable;
    }

    @Override
    protected void writeAdditionalAttributes(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }

    @Override
    protected void writeAdditionalChildElements(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }
}
