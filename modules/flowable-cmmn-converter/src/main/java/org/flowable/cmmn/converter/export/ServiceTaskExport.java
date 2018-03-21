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
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.ScriptServiceTask;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.TaskWithFieldExtensions;

public class ServiceTaskExport extends AbstractPlanItemDefinitionExport {

    public static void writeTask(ServiceTask task, XMLStreamWriter xtw) throws Exception {
        // start task element
        xtw.writeStartElement(ELEMENT_TASK);
        writeCommonTaskAttributes(xtw, task);

        if (StringUtils.isNotEmpty(task.getType())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_TYPE, task.getType());
        }

        switch (task.getType()) {
            case ServiceTask.JAVA_TASK:
                if (StringUtils.isNotEmpty(task.getImplementation())) {
                    if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(task.getImplementationType())) {
                        xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CLASS, task.getImplementation());

                    } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equals(task.getImplementationType())) {
                        xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_EXPRESSION, task.getImplementation());

                    } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(task.getImplementationType())) {
                        xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_DELEGATE_EXPRESSION, task.getImplementation());
                    }
                }

                if (StringUtils.isNotEmpty(task.getResultVariableName())) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_RESULT_VARIABLE_NAME, task.getResultVariableName());
                }
                break;

            case HttpServiceTask.HTTP_TASK:
                if (StringUtils.isNotEmpty(task.getImplementation())) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CLASS, task.getImplementation());
                }
                break;

            case ScriptServiceTask.SCRIPT_TASK:
                if (StringUtils.isNotBlank(task.getImplementationType())) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_SCRIPT_FORMAT, task.getImplementationType());
                }
                if (StringUtils.isNotEmpty(task.getResultVariableName())) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_RESULT_VARIABLE_NAME, task.getResultVariableName());
                }
                break;
        }

        writeExtensions(task, xtw);

        // end task element
        xtw.writeEndElement();
    }

    public static void writeExtensions(TaskWithFieldExtensions task, XMLStreamWriter xtw) throws XMLStreamException {
        if (task.getFieldExtensions().size() > 0) {
            xtw.writeStartElement(ELEMENT_EXTENSIONS);

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

            xtw.writeEndElement();
        }
    }

}
