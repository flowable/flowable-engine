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
package org.flowable.cmmn.converter;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.Task;

import javax.xml.stream.XMLStreamReader;

import org.flowable.cmmn.model.ScriptServiceTask;

/**
 * @author Joram Barrez
 */
public class TaskXmlConverter extends PlanItemDefinitiomXmlConverter {

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_TASK;
    }

    @Override
    public boolean hasChildElements() {
        return true;
    }

    @Override
    protected CmmnElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        Task task;
        String type = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_TYPE);
        String className = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_CLASS);

        if (type != null) {
            switch (type) {
                case ServiceTask.JAVA_TASK:
                    ServiceTask serviceTask = new ServiceTask();
                    serviceTask.setType(ServiceTask.JAVA_TASK);

                    String expression = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_EXPRESSION);
                    String delegateExpression = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_DELEGATE_EXPRESSION);

                    if (StringUtils.isNotBlank(className)) {
                        serviceTask.setImplementation(className);
                        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);

                    } else if (StringUtils.isNotBlank(expression)) {
                        serviceTask.setImplementation(expression);
                        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION);

                    } else if (StringUtils.isNotBlank(delegateExpression)) {
                        serviceTask.setImplementation(delegateExpression);
                        serviceTask.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION);
                    }

                    serviceTask.setResultVariableName(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_RESULT_VARIABLE_NAME));
                    task = serviceTask;
                    break;

                case HttpServiceTask.HTTP_TASK:
                    HttpServiceTask httpServiceTask = new HttpServiceTask();
                    if (StringUtils.isNotBlank(className)) {
                        httpServiceTask.setImplementation(className);
                    }
                    task = httpServiceTask;
                    break;

                case ScriptServiceTask.SCRIPT_TASK:
                    ScriptServiceTask scriptTask = new ScriptServiceTask();
                    String scriptFormat = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_SCRIPT_FORMAT);
                    if (StringUtils.isNotBlank(scriptFormat)) {
                        scriptTask.setScriptFormat(scriptFormat.trim());
                    }
                    String resVarName = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_RESULT_VARIABLE_NAME);
                    if (StringUtils.isNotBlank(resVarName)) {
                        scriptTask.setResultVariableName(resVarName.trim());
                    }
                    String autoStoreVariables = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_TASK_SCRIPT_AUTO_STORE_VARIABLE);
                    if (StringUtils.isNotBlank(autoStoreVariables)) {
                        scriptTask.setAutoStoreVariables(Boolean.valueOf(autoStoreVariables));
                    }

                    task = scriptTask;
                    break;
                default:
                    task = new Task();
            }
        } else {
            task = new Task();
        }

        convertCommonTaskAttributes(xtr, task);
        return task;
    }

    protected void convertCommonTaskAttributes(XMLStreamReader xtr, Task task) {
        task.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));

        String isBlockingString = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_IS_BLOCKING);
        if (StringUtils.isNotEmpty(isBlockingString)) {
            task.setBlocking(Boolean.valueOf(isBlockingString));
        }

        String isBlockingExpressionString = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE,
                CmmnXmlConstants.ATTRIBUTE_IS_BLOCKING_EXPRESSION);
        if (StringUtils.isNotEmpty(isBlockingExpressionString)) {
            task.setBlockingExpression(isBlockingExpressionString);
        }

        String isAsyncString = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE,
                CmmnXmlConstants.ATTRIBUTE_IS_ASYNCHRONOUS);
        if (StringUtils.isNotEmpty(isAsyncString)) {
            task.setAsync(Boolean.valueOf(isAsyncString.toLowerCase()));
        }

        String isExclusiveString = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE,
                CmmnXmlConstants.ATTRIBUTE_IS_EXCLUSIVE);
        if (StringUtils.isNotEmpty(isExclusiveString)) {
            task.setExclusive(Boolean.valueOf(isExclusiveString));
        }
    }
}
