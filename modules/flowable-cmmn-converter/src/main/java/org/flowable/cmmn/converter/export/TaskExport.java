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
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.SendEventServiceTask;
import org.flowable.cmmn.model.Task;
import org.flowable.cmmn.model.TaskWithFieldExtensions;

public class TaskExport extends AbstractPlanItemDefinitionExport<Task> {


    protected static <T extends TaskWithFieldExtensions> boolean writeTaskFieldExtensions(T task, boolean didWriteExtensionElement, XMLStreamWriter xtw) throws XMLStreamException {
        return FieldExport.writeFieldExtensions(task.getFieldExtensions(), didWriteExtensionElement, xtw);
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
        
        if (StringUtils.isNotEmpty(task.getBlockingExpression())){
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_IS_BLOCKING_EXPRESSION, task.getBlockingExpression());
        }

        // Async
        if (task.isAsync()) {
        	boolean exclusive = task.isExclusive();
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_IS_ASYNCHRONOUS, String.valueOf(task.isAsync()));
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_IS_EXCLUSIVE, String.valueOf(exclusive));
        }

        if (task.isAsyncLeave()) {
        	boolean asyncLeaveExclusive = task.isAsyncLeaveExclusive();
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_IS_ASYNCHRONOUS_LEAVE, String.valueOf(task.isAsyncLeave()));
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_IS_ASYNCHRONOUS_LEAVE_EXCLUSIVE, String.valueOf(asyncLeaveExclusive));
        }

        if (task instanceof SendEventServiceTask sendEventServiceTask) {
            if (StringUtils.isNotEmpty(sendEventServiceTask.getEventType()) && sendEventServiceTask.getExtensionElements().get("eventType") == null) {
                ExtensionElement extensionElement = new ExtensionElement();
                extensionElement.setNamespace(FLOWABLE_EXTENSIONS_NAMESPACE);
                extensionElement.setNamespacePrefix(FLOWABLE_EXTENSIONS_PREFIX);
                extensionElement.setName("eventType");
                extensionElement.setElementText(sendEventServiceTask.getEventType());
                sendEventServiceTask.addExtensionElement(extensionElement);
            }
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
