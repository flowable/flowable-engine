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

import java.util.Objects;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.CasePageTask;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.ExternalWorkerServiceTask;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.ScriptServiceTask;
import org.flowable.cmmn.model.SendEventServiceTask;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.Task;

/**
 * @author Joram Barrez
 */
public class TaskXmlConverter extends PlanItemDefinitionXmlConverter {

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
        Task task = null;
        String type = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_TYPE);
        String className = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_CLASS);

        if (type != null) {

            if (Objects.equals(type, ServiceTask.JAVA_TASK)) {
                task = convertToJavaServiceTask(xtr, className);

            } else if (Objects.equals(type, HttpServiceTask.HTTP_TASK)) {
                task = convertToHttpTask(xtr, className);

            } else if (Objects.equals(type, ServiceTask.MAIL_TASK)) {
                task = convertToMailTask();

            } else if (Objects.equals(type, ScriptServiceTask.SCRIPT_TASK)) {
                task = convertToScriptTask(xtr);
                
            } else if (Objects.equals(type, CasePageTask.TYPE)) {
                task = convertToCasePageTask(xtr);

            } else if (Objects.equals(type, ExternalWorkerServiceTask.TYPE)) {
                task = convertToExternalWorkerServiceTask(xtr);
            } else if (Objects.equals(type, SendEventServiceTask.SEND_EVENT)) {
              task = convertToSendEventTask(xtr);

            } else {
                task = new Task();
            }

        } else {
            task = new Task();
        }

        convertCommonTaskAttributes(xtr, task);
        return task;
    }

    protected Task convertToJavaServiceTask(XMLStreamReader xtr, String className) {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setType(ServiceTask.JAVA_TASK);

        String expression = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_EXPRESSION);
        String delegateExpression = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, 
                        CmmnXmlConstants.ATTRIBUTE_DELEGATE_EXPRESSION);

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

        serviceTask.setResultVariableName(
            xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_RESULT_VARIABLE_NAME));

        serviceTask.setStoreResultVariableAsTransient(
            Boolean.parseBoolean(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_STORE_RESULT_AS_TRANSIENT)));

        return serviceTask;
    }

    protected Task convertToHttpTask(XMLStreamReader xtr, String className) {
        HttpServiceTask httpServiceTask = new HttpServiceTask();
        if (StringUtils.isNotBlank(className)) {
            httpServiceTask.setImplementation(className);
        }

        String parallelInSameTransaction = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_HTTP_PARALLEL_IN_SAME_TRANSACTION);

        if (StringUtils.isNotEmpty(parallelInSameTransaction)) {
            httpServiceTask.setParallelInSameTransaction(Boolean.parseBoolean(parallelInSameTransaction));
        }

        return httpServiceTask;
    }

    protected Task convertToMailTask() {
        ServiceTask serviceTask = new ServiceTask();
        serviceTask.setType(ServiceTask.MAIL_TASK);
        return serviceTask;
    }
    
    protected Task convertToCasePageTask(XMLStreamReader xtr) {
        CasePageTask casePageTask = new CasePageTask();
        String formKey = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_FORM_KEY);
        if (formKey != null) {
            casePageTask.setFormKey(formKey);
        }

        String sameDeploymentAttribute = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_SAME_DEPLOYMENT);
        if ("false".equalsIgnoreCase(sameDeploymentAttribute)) {
            casePageTask.setSameDeployment(false);
        }

        String label = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_LABEL);
        if (label != null) {
            casePageTask.setLabel(label);
        }
        
        String icon = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_ICON);
        if (icon != null) {
            casePageTask.setIcon(icon);
        }
        
        String assignee = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_ASSIGNEE);
        if (assignee != null) {
            casePageTask.setAssignee(assignee);
        }
        
        String owner = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_OWNER);
        if (owner != null) {
            casePageTask.setOwner(owner);
        }
        
        String candidateUsersString = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_CANDIDATE_USERS);
        if (StringUtils.isNotEmpty(candidateUsersString)) {
            String[] candidateUsers = candidateUsersString.split(",");
            for (String candidateUser : candidateUsers) {
                casePageTask.getCandidateUsers().add(candidateUser);
            }
        }
        
        String candidateGroupsString = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_CANDIDATE_GROUPS);
        if (StringUtils.isNotEmpty(candidateGroupsString)) {
            String[] candidateGroups = candidateGroupsString.split(",");
            for (String candidateGroup : candidateGroups) {
                casePageTask.getCandidateGroups().add(candidateGroup);
            }
        }
        
        return casePageTask;
    }

    protected Task convertToScriptTask(XMLStreamReader xtr) {
        ScriptServiceTask scriptTask = new ScriptServiceTask();
        String scriptFormat = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_SCRIPT_FORMAT);
        if (StringUtils.isNotBlank(scriptFormat)) {
            scriptTask.setScriptFormat(scriptFormat.trim());
        }
        String resVarName = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_RESULT_VARIABLE_NAME);
        if (StringUtils.isNotBlank(resVarName)) {
            scriptTask.setResultVariableName(resVarName.trim());
        }
        String autoStoreVariables = xtr
            .getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_TASK_SCRIPT_AUTO_STORE_VARIABLE);
        if (StringUtils.isNotBlank(autoStoreVariables)) {
            scriptTask.setAutoStoreVariables(Boolean.valueOf(autoStoreVariables));
        }

        return scriptTask;
    }

    protected Task convertToExternalWorkerServiceTask(XMLStreamReader xtr) {
        ExternalWorkerServiceTask externalWorkerTask = new ExternalWorkerServiceTask();

        String topic = xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE, CmmnXmlConstants.ATTRIBUTE_EXTERNAL_WORKER_TOPIC);
        if (topic != null) {
            externalWorkerTask.setTopic(topic);
        }

        return externalWorkerTask;
    }

    protected Task convertToSendEventTask(XMLStreamReader xmlStreamReader) {
        return new SendEventServiceTask();
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
