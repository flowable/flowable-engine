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
package org.flowable.validation.validator.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.CaseServiceTask;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.Interface;
import org.flowable.bpmn.model.Operation;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.SendEventServiceTask;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.validation.ValidationError;
import org.flowable.validation.validator.Problems;

/**
 * @author jbarrez
 */
public class ServiceTaskValidator extends ExternalInvocationTaskValidator {

    @Override
    protected void executeValidation(BpmnModel bpmnModel, Process process, List<ValidationError> errors) {
        List<ServiceTask> serviceTasks = process.findFlowElementsOfType(ServiceTask.class);
        for (ServiceTask serviceTask : serviceTasks) {
            verifyImplementation(process, serviceTask, errors);
            verifyType(process, serviceTask, errors);
            verifyResultVariableName(process, serviceTask, errors);
            verifyWebservice(bpmnModel, process, serviceTask, errors);
        }
    }

    protected void verifyImplementation(Process process, ServiceTask serviceTask, List<ValidationError> errors) {
        if (!ImplementationType.IMPLEMENTATION_TYPE_CLASS.equalsIgnoreCase(serviceTask.getImplementationType())
                && !ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equalsIgnoreCase(serviceTask.getImplementationType())
                && !ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION.equalsIgnoreCase(serviceTask.getImplementationType())
                && !ImplementationType.IMPLEMENTATION_TYPE_WEBSERVICE.equalsIgnoreCase(serviceTask.getImplementationType()) 
                && StringUtils.isEmpty(serviceTask.getType())) {
            
            addError(errors, Problems.SERVICE_TASK_MISSING_IMPLEMENTATION, process, serviceTask,
                    "One of the attributes 'class', 'delegateExpression', 'type', 'operation', or 'expression' is mandatory on serviceTask.");
        }
    }

    protected void verifyType(Process process, ServiceTask serviceTask, List<ValidationError> errors) {
        if (StringUtils.isNotEmpty(serviceTask.getType())) {

            switch (serviceTask.getType()) {
                case ServiceTask.MAIL_TASK:
                    validateFieldDeclarationsForEmail(process, serviceTask, serviceTask.getFieldExtensions(), errors);
                    return;
                case ServiceTask.SHELL_TASK:
                    validateFieldDeclarationsForShell(process, serviceTask, serviceTask.getFieldExtensions(), errors);
                    return;
                case ServiceTask.DMN_TASK:
                    validateFieldDeclarationsForDmn(process, serviceTask, serviceTask.getFieldExtensions(), errors);
                    return;
                case ServiceTask.HTTP_TASK:
                    validateFieldDeclarationsForHttp(process, serviceTask, serviceTask.getFieldExtensions(), errors);
                    return;
                case ServiceTask.CASE_TASK:
                    validateFieldDeclarationsForCase(process, (CaseServiceTask) serviceTask, errors);
                    return;
                case ServiceTask.SEND_EVENT_TASK:
                    validateFieldDeclarationsForSendEventTask(process, (SendEventServiceTask) serviceTask, errors);
                    return;
                case ServiceTask.EXTERNAL_WORKER_TASK:
                    validateExternalWorkerTask(process, (ExternalWorkerServiceTask) serviceTask, errors);
                    return;
                case ServiceTask.MULE:
                case ServiceTask.CAMEL:
                    // Mule or camel have no special validation
                    return;
                default:
                    validateUnknownServiceTaskType(process, serviceTask, errors);
            }

        }
    }

    protected void validateUnknownServiceTaskType(Process process, ServiceTask serviceTask, List<ValidationError> errors) {
        addError(errors, Problems.SERVICE_TASK_INVALID_TYPE, process, serviceTask, "Invalid or unsupported service task type");
    }

    protected void verifyResultVariableName(Process process, ServiceTask serviceTask, List<ValidationError> errors) {
        if (StringUtils.isNotEmpty(serviceTask.getResultVariableName())
                && (ImplementationType.IMPLEMENTATION_TYPE_CLASS.equals(serviceTask.getImplementationType()) || 
                                ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(serviceTask.getImplementationType()))) {
            
            addError(errors, Problems.SERVICE_TASK_RESULT_VAR_NAME_WITH_DELEGATE, process, serviceTask, "'resultVariableName' not supported for service tasks using 'class' or 'delegateExpression");
        }

        if (serviceTask.isUseLocalScopeForResultVariable() && StringUtils.isEmpty(serviceTask.getResultVariableName())) {
            addWarning(errors, Problems.SERVICE_TASK_USE_LOCAL_SCOPE_FOR_RESULT_VAR_WITHOUT_RESULT_VARIABLE_NAME, process, serviceTask, "'useLocalScopeForResultVariable' is set, but no 'resultVariableName' is set. The property would not be used");
        }
    }

    protected void verifyWebservice(BpmnModel bpmnModel, Process process, ServiceTask serviceTask, List<ValidationError> errors) {
        if (ImplementationType.IMPLEMENTATION_TYPE_WEBSERVICE.equalsIgnoreCase(serviceTask.getImplementationType()) && StringUtils.isNotEmpty(serviceTask.getOperationRef())) {

            boolean operationFound = false;
            if (bpmnModel.getInterfaces() != null && !bpmnModel.getInterfaces().isEmpty()) {
                for (Interface bpmnInterface : bpmnModel.getInterfaces()) {
                    if (bpmnInterface.getOperations() != null && !bpmnInterface.getOperations().isEmpty()) {
                        for (Operation operation : bpmnInterface.getOperations()) {
                            if (operation.getId() != null && operation.getId().equals(serviceTask.getOperationRef())) {
                                operationFound = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (!operationFound) {
                addError(errors, Problems.SERVICE_TASK_WEBSERVICE_INVALID_OPERATION_REF, process, serviceTask, "Invalid operation reference");
            }

        }
    }

}
