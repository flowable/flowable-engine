package org.flowable.cmmn.converter.export;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.ChildTask;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.IOParameter;

import javax.xml.stream.XMLStreamWriter;
import java.util.List;

/**
 * @author Valentin Zickner
 */
public abstract class AbstractChildTaskExport<T extends ChildTask> extends AbstractPlanItemDefinitionExport<T> {

    @Override
    protected boolean writePlanItemDefinitionExtensionElements(CmmnModel model, T planItemDefinition, boolean didWriteExtensionElement, XMLStreamWriter xtw) throws Exception {
        boolean extensionElementWritten = super.writePlanItemDefinitionExtensionElements(model, planItemDefinition, didWriteExtensionElement, xtw);
        extensionElementWritten = writeIOParameters(ELEMENT_CHILD_TASK_IN_PARAMETERS, planItemDefinition.getInParameters(), extensionElementWritten, xtw);
        extensionElementWritten = writeIOParameters(ELEMENT_CHILD_TASK_OUT_PARAMETERS, planItemDefinition.getOutParameters(), extensionElementWritten, xtw);
        return extensionElementWritten;
    }

    protected boolean writeIOParameters(String elementName, List<IOParameter> parameterList, boolean didWriteParameterStartElement, XMLStreamWriter xtw) throws Exception {

        if (parameterList == null || parameterList.isEmpty()) {
            return didWriteParameterStartElement;
        }

        for (IOParameter ioParameter : parameterList) {
            if (!didWriteParameterStartElement) {
                xtw.writeStartElement(ELEMENT_EXTENSION_ELEMENTS);
                didWriteParameterStartElement = true;
            }

            xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, elementName, FLOWABLE_EXTENSIONS_NAMESPACE);
            if (StringUtils.isNotEmpty(ioParameter.getSource())) {
                xtw.writeAttribute(ATTRIBUTE_IOPARAMETER_SOURCE, ioParameter.getSource());
            }
            if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                xtw.writeAttribute(ATTRIBUTE_IOPARAMETER_SOURCE_EXPRESSION, ioParameter.getSourceExpression());
            }
            if (StringUtils.isNotEmpty(ioParameter.getTarget())) {
                xtw.writeAttribute(ATTRIBUTE_IOPARAMETER_TARGET, ioParameter.getTarget());
            }
            if (StringUtils.isNotEmpty(ioParameter.getTargetExpression())) {
                xtw.writeAttribute(ATTRIBUTE_IOPARAMETER_TARGET_EXPRESSION, ioParameter.getTargetExpression());
            }

            xtw.writeEndElement();
        }

        return didWriteParameterStartElement;
    }
}
