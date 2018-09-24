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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.Task;
import org.flowable.cmmn.model.TaskWithFieldExtensions;

public class TaskExport extends AbstractPlanItemDefinitionExport<Task> {


    protected static <T extends TaskWithFieldExtensions> boolean writeTaskFieldExtensions(T task, boolean didWriteExtensionElement, XMLStreamWriter xtw) throws XMLStreamException {
        if (task.getFieldExtensions().size() > 0) {
            if (!didWriteExtensionElement) {
                xtw.writeStartElement(ELEMENT_EXTENSION_ELEMENTS);
                didWriteExtensionElement = true;
            }

            for (FieldExtension fieldExtension : task.getFieldExtensions()) {
                xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_FIELD, FLOWABLE_EXTENSIONS_NAMESPACE);
                xtw.writeAttribute(ATTRIBUTE_NAME, fieldExtension.getFieldName());

                if (StringUtils.isNotEmpty(fieldExtension.getStringValue())) {
                    xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ELEMENT_FIELD_STRING, FLOWABLE_EXTENSIONS_NAMESPACE);
                    xtw.writeCData(fieldExtension.getStringValue());
                } else {
                    xtw.writeStartElement(FLOWABLE_EXTENSIONS_PREFIX, ATTRIBUTE_FIELD_EXPRESSION, FLOWABLE_EXTENSIONS_NAMESPACE);
                    xtw.writeCData(fieldExtension.getExpression());
                }
                xtw.writeEndElement();
                xtw.writeEndElement();
            }
        }
        
        return didWriteExtensionElement;
    }

    protected static <T extends Task> void writeCommonTaskAttributes(T task, XMLStreamWriter xtw) throws Exception {
        // Blocking
        if (StringUtils.isEmpty(task.getBlockingExpression())) {
            if (!task.isBlocking()) { // if omitted, by default assumed true
                xtw.writeAttribute(ATTRIBUTE_IS_BLOCKING, "false");
            }
        } else {
            xtw.writeAttribute(ATTRIBUTE_IS_BLOCKING, "true");
        }

        // Async
        if (task.isAsync()) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_IS_ASYNCHRONOUS, String.valueOf(task.isAsync()));
        }
        if (task.isExclusive()) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_IS_EXCLUSIVE, String.valueOf(task.isAsync()));
        }
    }

    @Override
    protected Class<Task> getExportablePlanItemDefinitionClass() {
        return Task.class;
    }

    @Override
    protected String getPlanItemDefinitionXmlElementValue(Task planItemDefinition) {
        return ELEMENT_TASK;
    }

    @Override
    protected void writePlanItemDefinitionSpecificAttributes(Task task, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(task, xtw);
        writeCommonTaskAttributes(task, xtw);
    }


}
