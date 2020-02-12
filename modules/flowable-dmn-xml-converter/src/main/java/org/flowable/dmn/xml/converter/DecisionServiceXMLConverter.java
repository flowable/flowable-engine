package org.flowable.dmn.xml.converter;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.DmnElementReference;
import org.flowable.dmn.model.InformationItem;

public class DecisionServiceXMLConverter extends BaseDmnXMLConverter {

    @Override
    public Class<? extends DmnElement> getDmnElementType() {
        return DecisionService.class;
    }

    @Override
    protected String getXMLElementName() {
        return ELEMENT_DECISION_SERVICE;
    }

    @Override
    protected DmnElement convertXMLToElement(XMLStreamReader xtr, DmnDefinition model, Decision decision) throws Exception {
        DecisionService decisionService = new DecisionService();
        decisionService.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));
        decisionService.setName(xtr.getAttributeValue(null, ATTRIBUTE_NAME));
        decisionService.setLabel(xtr.getAttributeValue(null, ATTRIBUTE_LABEL));

        boolean readyWithDecisionService = false;
        try {
            while (!readyWithDecisionService && xtr.hasNext()) {
                xtr.next();
                if (xtr.isStartElement() && ELEMENT_OUTPUT_DECISION.equalsIgnoreCase(xtr.getLocalName())) {
                    DmnElementReference ref = new DmnElementReference();
                    ref.setHref(xtr.getAttributeValue(null, ATTRIBUTE_HREF));
                    decisionService.addOutputDecision(ref);
                } if (xtr.isStartElement() && ELEMENT_ENCAPSULATED_DECISION.equalsIgnoreCase(xtr.getLocalName())) {
                    DmnElementReference ref = new DmnElementReference();
                    ref.setHref(xtr.getAttributeValue(null, ATTRIBUTE_HREF));
                    decisionService.addEncapsulatedDecision(ref);
                } if (xtr.isStartElement() && ELEMENT_INPUT_DATA.equalsIgnoreCase(xtr.getLocalName())) {
                    DmnElementReference ref = new DmnElementReference();
                    ref.setHref(xtr.getAttributeValue(null, ATTRIBUTE_HREF));
                    decisionService.addInputData(ref);
                } else if (xtr.isEndElement() && getXMLElementName().equalsIgnoreCase(xtr.getLocalName())) {
                    readyWithDecisionService = true;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Error parsing output entry", e);
        }
        return decisionService;
    }

    @Override
    protected void writeAdditionalAttributes(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }

    @Override
    protected void writeAdditionalChildElements(DmnElement element, DmnDefinition model, XMLStreamWriter xtw) throws Exception {

    }
}
