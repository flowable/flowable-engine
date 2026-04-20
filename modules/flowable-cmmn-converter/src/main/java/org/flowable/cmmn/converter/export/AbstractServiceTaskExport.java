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
import org.flowable.cmmn.converter.CmmnXmlConverterOptions;
import org.flowable.cmmn.converter.util.CmmnXmlUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.FormAwareServiceTask;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.ScriptServiceTask;
import org.flowable.cmmn.model.ServiceTask;

public abstract class AbstractServiceTaskExport<T extends ServiceTask> extends AbstractPlanItemDefinitionExport<T> {

    @Override
    public String getPlanItemDefinitionXmlElementValue(ServiceTask serviceTask) {
        return ELEMENT_TASK;
    }

    @Override
    public void writePlanItemDefinitionSpecificAttributes(T serviceTask, XMLStreamWriter xtw) throws Exception {
        super.writePlanItemDefinitionSpecificAttributes(serviceTask, xtw);
        TaskExport.writeCommonTaskAttributes(serviceTask, xtw);

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
                if (serviceTask.isStoreResultVariableAsTransient()) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_STORE_RESULT_AS_TRANSIENT, String.valueOf(serviceTask.isStoreResultVariableAsTransient()));
                }
                break;

            case HttpServiceTask.HTTP_TASK:
                if (StringUtils.isNotEmpty(serviceTask.getImplementation())) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_CLASS, serviceTask.getImplementation());
                }

                Boolean parallelInSameTransaction = ((HttpServiceTask) serviceTask).getParallelInSameTransaction();
                if (parallelInSameTransaction != null) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_HTTP_PARALLEL_IN_SAME_TRANSACTION, parallelInSameTransaction.toString());
                }
                break;

            case ScriptServiceTask.SCRIPT_TASK:
                if (StringUtils.isNotBlank(serviceTask.getImplementationType())) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_SCRIPT_FORMAT, serviceTask.getImplementationType());
                }
                if (StringUtils.isNotEmpty(serviceTask.getResultVariableName())) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_RESULT_VARIABLE_NAME, serviceTask.getResultVariableName());
                }
                if (((ScriptServiceTask) serviceTask).isAutoStoreVariables()) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_TASK_SCRIPT_AUTO_STORE_VARIABLE, "true");
                }
                if (((ScriptServiceTask) serviceTask).isDoNotIncludeVariables()) {
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_TASK_SCRIPT_DO_NOT_INCLUDE_VARIABLES, "true");
                }
                break;
            default:
                break;
        }
    }
    
    @Override
    protected boolean writePlanItemDefinitionExtensionElements(CmmnModel model, T serviceTask, boolean didWriteExtensionElement, XMLStreamWriter xtw)
            throws Exception {
        boolean extensionElementWritten = super.writePlanItemDefinitionExtensionElements(model, serviceTask, didWriteExtensionElement, xtw);
        return TaskExport.writeTaskFieldExtensions(serviceTask, extensionElementWritten, xtw);
    }

    @Override
    protected void writePlanItemDefinitionBody(CmmnModel model, T serviceTask, XMLStreamWriter xtw, CmmnXmlConverterOptions options) throws Exception {
        super.writePlanItemDefinitionBody(model, serviceTask, xtw, options);
    }

    public static class ServiceTaskExport extends AbstractServiceTaskExport<ServiceTask> {
        @Override
        protected Class<? extends ServiceTask> getExportablePlanItemDefinitionClass() {
            return ServiceTask.class;
        }
    }

    public static class HttpServiceTaskExport extends AbstractServiceTaskExport<HttpServiceTask> {

        @Override
        protected Class<? extends HttpServiceTask> getExportablePlanItemDefinitionClass() {
            return HttpServiceTask.class;
        }
    }

    public static class ScriptServiceTaskExport extends AbstractServiceTaskExport<ScriptServiceTask> {
        @Override
        protected Class<? extends ScriptServiceTask> getExportablePlanItemDefinitionClass() {
            return ScriptServiceTask.class;
        }

        @Override
        protected boolean writePlanItemDefinitionExtensionElements(CmmnModel model, ScriptServiceTask serviceTask, boolean didWriteExtensionElement,
                XMLStreamWriter xtw) throws Exception {
            boolean extensionElementWritten = super.writePlanItemDefinitionExtensionElements(model, serviceTask, didWriteExtensionElement, xtw);
            extensionElementWritten = CmmnXmlUtil.writeIOParameters(ELEMENT_EXTERNAL_WORKER_IN_PARAMETER, serviceTask.getInParameters(),
                    extensionElementWritten, xtw);
            return extensionElementWritten;
        }
    }

    public static class FormAwareServiceTaskExport extends AbstractServiceTaskExport<FormAwareServiceTask> {

        @Override
        protected Class<? extends FormAwareServiceTask> getExportablePlanItemDefinitionClass() {
            return FormAwareServiceTask.class;
        }

        @Override
        public void writePlanItemDefinitionSpecificAttributes(FormAwareServiceTask formAwareServiceTask, XMLStreamWriter xtw) throws Exception {
            super.writePlanItemDefinitionSpecificAttributes(formAwareServiceTask, xtw);
            if (StringUtils.isNotBlank(formAwareServiceTask.getFormKey())) {
                xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_FORM_KEY, formAwareServiceTask.getFormKey());
            }
            if (StringUtils.isNotBlank(formAwareServiceTask.getValidateFormFields())) {
                xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_FORM_FIELD_VALIDATION,
                        formAwareServiceTask.getValidateFormFields());
            }
        }
    }

}
