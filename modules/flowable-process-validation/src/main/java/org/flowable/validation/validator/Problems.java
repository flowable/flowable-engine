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
package org.flowable.validation.validator;

/**
 * @author jbarrez
 */
public interface Problems {

    String ALL_PROCESS_DEFINITIONS_NOT_EXECUTABLE = "flowable-process-definition-not-executable";
    String PROCESS_DEFINITION_NOT_EXECUTABLE = "flowable-specific-process-definition-not-executable";

    String ASSOCIATION_INVALID_SOURCE_REFERENCE = "flowable-asscociation-invalid-source-reference";

    String ASSOCIATION_INVALID_TARGET_REFERENCE = "flowable-asscociation-invalid-target-reference";

    String EXECUTION_LISTENER_IMPLEMENTATION_MISSING = "flowable-execution-listener-implementation-missing";
    String EXECUTION_LISTENER_INVALID_IMPLEMENTATION_TYPE = "flowable-execution-listener-invalid-implementation-type";

    String EVENT_LISTENER_IMPLEMENTATION_MISSING = "flowable-event-listener-implementation-missing";
    String EVENT_LISTENER_INVALID_IMPLEMENTATION = "flowable-event-listener-invalid-implementation";
    String EVENT_LISTENER_INVALID_THROW_EVENT_TYPE = "flowable-event-listener-invalid-throw-event-type";

    String START_EVENT_MULTIPLE_FOUND = "flowable-start-event-multiple-found";
    String START_EVENT_INVALID_EVENT_DEFINITION = "flowable-start-event-invalid-event-definition";

    String SEQ_FLOW_INVALID_SRC = "flowable-seq-flow-invalid-src";
    String SEQ_FLOW_INVALID_TARGET = "flowable-seq-flow-invalid-target";

    String USER_TASK_LISTENER_IMPLEMENTATION_MISSING = "flowable-usertask-listener-implementation-missing";

    String SERVICE_TASK_INVALID_TYPE = "flowable-servicetask-invalid-type";
    String SERVICE_TASK_RESULT_VAR_NAME_WITH_DELEGATE = "flowable-servicetask-result-var-name-with-delegate";
    String SERVICE_TASK_MISSING_IMPLEMENTATION = "flowable-servicetask-missing-implementation";
    String SERVICE_TASK_WEBSERVICE_INVALID_OPERATION_REF = "flowable-servicetask-webservice-invalid-operation-ref";
    String SERVICE_TASK_USE_LOCAL_SCOPE_FOR_RESULT_VAR_WITHOUT_RESULT_VARIABLE_NAME = "flowable-servicetask-use-local-scope-for-result-var-without-result-variable-name";

    String SEND_TASK_INVALID_IMPLEMENTATION = "flowable-sendtask-invalid-implementation";
    String SEND_TASK_INVALID_TYPE = "flowable-sendtask-invalid-type";
    String SEND_TASK_WEBSERVICE_INVALID_OPERATION_REF = "flowable-send-webservice-invalid-operation-ref";

    String SCRIPT_TASK_MISSING_SCRIPT = "flowable-scripttask-missing-script";

    String MAIL_TASK_NO_RECIPIENT = "flowable-mailtask-no-recipient";
    String MAIL_TASK_NO_CONTENT = "flowable-mailtask-no-content";

    String SHELL_TASK_NO_COMMAND = "flowable-shelltask-no-command";
    String SHELL_TASK_INVALID_PARAM = "flowable-shelltask-invalid-param";

    String DMN_TASK_NO_KEY = "flowable-dmntask-no-decision-table-key";

    String HTTP_TASK_NO_REQUEST_URL = "flowable-httptask-no-request-url";
    String HTTP_TASK_NO_REQUEST_METHOD = "flowable-httptask-no-request-method";

    String EXCLUSIVE_GATEWAY_NO_OUTGOING_SEQ_FLOW = "flowable-exclusive-gateway-no-outgoing-seq-flow";
    String EXCLUSIVE_GATEWAY_CONDITION_NOT_ALLOWED_ON_SINGLE_SEQ_FLOW = "flowable-exclusive-gateway-condition-not-allowed-on-single-seq-flow";
    String EXCLUSIVE_GATEWAY_CONDITION_ON_DEFAULT_SEQ_FLOW = "flowable-exclusive-gateway-condition-on-seq-flow";
    String EXCLUSIVE_GATEWAY_SEQ_FLOW_WITHOUT_CONDITIONS = "flowable-exclusive-gateway-seq-flow-without-conditions";

    String EVENT_GATEWAY_ONLY_CONNECTED_TO_INTERMEDIATE_EVENTS = "flowable-event-gateway-only-connected-to-intermediate-events";

    String BPMN_MODEL_TARGET_NAMESPACE_TOO_LONG = "flowable-bpmn-model-target-namespace-too-long";

    String PROCESS_DEFINITION_ID_TOO_LONG = "flowable-process-definition-id-too-long";
    String PROCESS_DEFINITION_NAME_TOO_LONG = "flowable-process-definition-name-too-long";
    String PROCESS_DEFINITION_DOCUMENTATION_TOO_LONG = "flowable-process-definition-documentation-too-long";

    String FLOW_ELEMENT_ID_TOO_LONG = "flowable-flow-element-id-too-long";

    String SUBPROCESS_MULTIPLE_START_EVENTS = "flowable-subprocess-multiple-start-event";

    String SUBPROCESS_START_EVENT_EVENT_DEFINITION_NOT_ALLOWED = "flowable-subprocess-start-event-event-definition-not-allowed";

    String EVENT_SUBPROCESS_INVALID_START_EVENT_DEFINITION = "flowable-event-subprocess-invalid-start-event-definition";

    String BOUNDARY_EVENT_NO_EVENT_DEFINITION = "flowable-boundary-event-no-event-definition";
    String BOUNDARY_EVENT_INVALID_EVENT_DEFINITION = "flowable-boundary-event-invalid-event-definition";
    String BOUNDARY_EVENT_CANCEL_ONLY_ON_TRANSACTION = "flowable-boundary-event-cancel-only-on-transaction";
    String BOUNDARY_EVENT_MULTIPLE_CANCEL_ON_TRANSACTION = "flowable-boundary-event-multiple-cancel-on-transaction";

    String INTERMEDIATE_CATCH_EVENT_NO_EVENTDEFINITION = "flowable-intermediate-catch-event-no-eventdefinition";
    String INTERMEDIATE_CATCH_EVENT_INVALID_EVENTDEFINITION = "flowable-intermediate-catch-event-invalid-eventdefinition";

    String ERROR_MISSING_ERROR_CODE = "flowable-error-missing-error-code";
    String EVENT_MISSING_ERROR_CODE = "flowable-event-missing-error-code";
    String EVENT_TIMER_MISSING_CONFIGURATION = "flowable-event-timer-missing-configuration";

    String THROW_EVENT_INVALID_EVENTDEFINITION = "flowable-throw-event-invalid-eventdefinition";

    String MULTI_INSTANCE_MISSING_COLLECTION = "flowable-multi-instance-missing-collection";
    String MULTI_INSTANCE_MISSING_COLLECTION_FUNCTION_PARAMETERS = "flowable-multi-instance-missing-collection-parser";
    
    String MESSAGE_MISSING_NAME = "flowable-message-missing-name";
    String MESSAGE_INVALID_ITEM_REF = "flowable-message-invalid-item-ref";
    String MESSAGE_EVENT_MISSING_MESSAGE_REF = "flowable-message-event-missing-message-ref";
    String MESSAGE_EVENT_INVALID_MESSAGE_REF = "flowable-message-event-invalid-message-ref";
    String MESSAGE_EVENT_MULTIPLE_ON_BOUNDARY_SAME_MESSAGE_ID = "flowable-message-event-multiple-on-boundary-same-message-id";

    String OPERATION_INVALID_IN_MESSAGE_REFERENCE = "flowable-operation-invalid-in-message-reference";

    String SIGNAL_EVENT_MISSING_SIGNAL_REF = "flowable-signal-event-missing-signal-ref";
    String SIGNAL_EVENT_INVALID_SIGNAL_REF = "flowable-signal-event-invalid-signal-ref";

    String COMPENSATE_EVENT_INVALID_ACTIVITY_REF = "flowable-compensate-event-invalid-activity-ref";
    String COMPENSATE_EVENT_MULTIPLE_ON_BOUNDARY = "flowable-compensate-event-multiple-on-boundary";

    String SIGNAL_MISSING_ID = "flowable-signal-missing-id";
    String SIGNAL_MISSING_NAME = "flowable-signal-missing-name";
    String SIGNAL_DUPLICATE_NAME = "flowable-signal-duplicate-name";
    String SIGNAL_INVALID_SCOPE = "flowable-signal-invalid-scope";

    String DATA_ASSOCIATION_MISSING_TARGETREF = "flowable-data-association-missing-targetref";

    String DATA_OBJECT_MISSING_NAME = "flowable-data-object-missing-name";

    String END_EVENT_CANCEL_ONLY_INSIDE_TRANSACTION = "flowable-end-event-cancel-only-inside-transaction";

    String DI_INVALID_REFERENCE = "flowable-di-invalid-reference";
    String DI_DOES_NOT_REFERENCE_FLOWNODE = "flowable-di-does-not-reference-flownode";
    String DI_DOES_NOT_REFERENCE_SEQ_FLOW = "flowable-di-does-not-reference-seq-flow";

}
