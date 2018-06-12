package org.flowable.cmmn.converter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.exception.XMLException;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.ProcessTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ParameterMappingXMLConverter extends CaseElementXmlConverter {

    protected static final Logger LOGGER = LoggerFactory.getLogger(ParameterMappingXMLConverter.class);

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_PARAMETER_MAPPING;
    }

    @Override
    public boolean hasChildElements() {
        return false;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {

        if (conversionHelper.getCurrentCmmnElement() instanceof ProcessTask) {
            ProcessTask processTask = (ProcessTask) conversionHelper.getCurrentCmmnElement();
            List<IOParameter> inParameters = new ArrayList<>();
            List<IOParameter> outParameters = new ArrayList<>();

            boolean readyWithChildElements = false;
            try {

                while (!readyWithChildElements && xtr.hasNext()) {
                    xtr.next();
                    if (xtr.isStartElement()) {
                        if (CmmnXmlConstants.ELEMENT_PROCESS_TASK_IN_PARAMETERS.equals(xtr.getLocalName())) {
                            readIOParameter(xtr, inParameters);
                        } else if (CmmnXmlConstants.ELEMENT_PROCESS_TASK_OUT_PARAMETERS.equals(xtr.getLocalName())) {
                            readIOParameter(xtr, outParameters);
                        }
                    } else if (xtr.isEndElement()) {
                        if (CmmnXmlConstants.ELEMENT_PARAMETER_MAPPING.equalsIgnoreCase(xtr.getLocalName())) {
                            readyWithChildElements = true;
                        }
                    }

                }
            } catch (Exception ex) {
                LOGGER.error("Error processing BPMN document", ex);
                throw new XMLException("Error processing CMMN document", ex);
            }

            if(!inParameters.isEmpty()){
                processTask.setInParameters(inParameters);
            }
            if(!outParameters.isEmpty()){
                processTask.setOutParameters(outParameters);
            }
        }
        return null;
    }

    private void readIOParameter(XMLStreamReader xtr, List<IOParameter> inParameters) {

        String source = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_SOURCE);
        String sourceExpression = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION);
        String target = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IOPARAMETER_TARGET);
        if ((StringUtils.isNotEmpty(source) || StringUtils.isNotEmpty(sourceExpression)) && StringUtils.isNotEmpty(target)) {
            IOParameter parameter = new IOParameter();
            if (StringUtils.isNotEmpty(sourceExpression)) {
                parameter.setSourceExpression(sourceExpression);
            } else {
                parameter.setSource(source);
            }

            parameter.setTarget(target);
            inParameters.add(parameter);
        }
    }

}
