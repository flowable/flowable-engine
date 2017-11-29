package org.flowable.cmmn.converter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.AbstractFlowableHttpHandler;

import javax.xml.stream.XMLStreamReader;

import static org.flowable.cmmn.converter.CmmnXmlConstants.ATTRIBUTE_CLASS;
import static org.flowable.cmmn.converter.CmmnXmlConstants.ATTRIBUTE_DELEGATE_EXPRESSION;
import static org.flowable.cmmn.model.ImplementationType.IMPLEMENTATION_TYPE_CLASS;
import static org.flowable.cmmn.model.ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION;

/**
 * @author martin.grofcik
 */
public abstract class AbstractFlowableHttpHandlerXmlConverter extends BaseCmmnXmlConverter {

    protected void setImplementation(XMLStreamReader xtr, AbstractFlowableHttpHandler handler) {
        if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_CLASS))) {
            handler.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_CLASS));
            handler.setImplementationType(IMPLEMENTATION_TYPE_CLASS);

        } else if (StringUtils.isNotEmpty(xtr.getAttributeValue(null, ATTRIBUTE_DELEGATE_EXPRESSION))) {
            handler.setImplementation(xtr.getAttributeValue(null, ATTRIBUTE_DELEGATE_EXPRESSION));
            handler.setImplementationType(IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
        }
    }
}
