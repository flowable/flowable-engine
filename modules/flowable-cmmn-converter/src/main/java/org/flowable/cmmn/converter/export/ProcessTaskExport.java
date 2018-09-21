/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.cmmn.converter.export;

import java.util.List;

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.IOParameter;
import org.flowable.cmmn.model.ProcessTask;

public class ProcessTaskExport extends AbstractPlanItemDefinitionExport<ProcessTask> {

    @Override
    protected Class<ProcessTask> getExportablePlanItemDefinitionClass() {
        return ProcessTask.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(ProcessTask planItemDefinition) {
        return ELEMENT_PROCESS_TASK;
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(ProcessTask processTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(processTask, xtw);
        TaskExport.writeCommonTaskAttributes(processTask, xtw);
    }
    

    @Override
    protected boolean writePlanItemDefinitionExtensionElements(CmmnModel model, ProcessTask processTask, boolean didWriteExtensionElement, XMLStreamWriter xtw) throws Exception {
        didWriteExtensionElement = writeIOParameters(ELEMENT_PROCESS_TASK_IN_PARAMETERS,
                processTask.getInParameters(), didWriteExtensionElement, xtw);
        didWriteExtensionElement = writeIOParameters(ELEMENT_PROCESS_TASK_OUT_PARAMETERS,
                processTask.getOutParameters(), didWriteExtensionElement, xtw);
        
        return didWriteExtensionElement;
    }

    @Override
    protected void writePlanItemDefinitionBody(CmmnModel model, ProcessTask processTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionBody(model, processTask, xtw);
        
        if (StringUtils.isNotEmpty(processTask.getProcessRef()) || StringUtils.isNotEmpty(processTask.getProcessRefExpression())) {
            xtw.writeStartElement(ELEMENT_PROCESS_REF_EXPRESSION);
            xtw.writeCData(
                    StringUtils.isNotEmpty(processTask.getProcessRef()) ?
                            processTask.getProcessRef() :
                            processTask.getProcessRefExpression()
            );
            xtw.writeEndElement();
        }
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

            xtw.writeEndElement();
        }

        return didWriteParameterStartElement;
    }


}
