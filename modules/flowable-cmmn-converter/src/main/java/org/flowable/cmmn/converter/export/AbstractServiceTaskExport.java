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

import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.ScriptServiceTask;
import org.flowable.cmmn.model.ServiceTask;

public abstract class AbstractServiceTaskExport<T extends ServiceTask> extends AbstractPlanItemDefinitionExport<ServiceTask> {

    @Override
    public String getPlanItemDefinitionXmlElementValue(ServiceTask serviceTask) {
        return ELEMENT_TASK;
    }

    @Override
    public void writePlanItemDefinitionSpecificAttributes(ServiceTask serviceTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(serviceTask, xtw);

        if (StringUtils.isNotEmpty(serviceTask.getType())) {
            xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_TYPE, serviceTask.getType());
        }
        switch (serviceTask.getType()) {
            case ServiceTask.JAVA_TASK:
                if (StringUtils.isNotEmpty(serviceTask.getImplementation())) {
                    if (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(serviceTask.getImplementationType())) {
                        xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CLASS, serviceTask.getImplementation());

                    } else if (ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equals(serviceTask.getImplementationType())) {
                        xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_EXPRESSION, serviceTask.getImplementation());

                    } else if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(serviceTask.getImplementationType())) {
                        xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_DELEGATE_EXPRESSION, serviceTask.getImplementation());
                    }
                }

                if (StringUtils.isNotEmpty(serviceTask.getResultVariableName())) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_RESULT_VARIABLE_NAME, serviceTask.getResultVariableName());
                }
                break;

            case HttpServiceTask.HTTP_TASK:
                if (StringUtils.isNotEmpty(serviceTask.getImplementation())) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CLASS, serviceTask.getImplementation());
                }
                break;

            case ScriptServiceTask.SCRIPT_TASK:
                if (StringUtils.isNotBlank(serviceTask.getImplementationType())) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_SCRIPT_FORMAT, serviceTask.getImplementationType());
                }
                if (StringUtils.isNotEmpty(serviceTask.getResultVariableName())) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_RESULT_VARIABLE_NAME, serviceTask.getResultVariableName());
                }
                break;
        }
    }
    
    @Override
    protected boolean writePlanItemDefinitionExtensionElements(CmmnModel model, ServiceTask serviceTask, boolean didWriteExtensionElement, XMLStreamWriter xtw) throws Exception {
        return TaskExport.writeTaskFieldExtensions(serviceTask, didWriteExtensionElement, xtw);
    }

    @Override
    protected void writePlanItemDefinitionBody(CmmnModel model, ServiceTask serviceTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionBody(model, serviceTask, xtw);
    }

    public static class ServiceTaskExport extends AbstractServiceTaskExport<ServiceTask> {
        @Override
        protected Class<? extends ServiceTask> getExportablePlanItemDefinitionClass() {
            return ServiceTask.class;
        }
    }

    public static class HttpServiceTaskExport extends AbstractServiceTaskExport<HttpServiceTask> {
        @Override
        protected Class<? extends ServiceTask> getExportablePlanItemDefinitionClass() {
            return HttpServiceTask.class;
        }
    }

    public static class ScriptServiceTaskExport extends AbstractServiceTaskExport<ScriptServiceTask> {
        @Override
        protected Class<? extends ServiceTask> getExportablePlanItemDefinitionClass() {
            return ScriptServiceTask.class;
        }
    }
}
