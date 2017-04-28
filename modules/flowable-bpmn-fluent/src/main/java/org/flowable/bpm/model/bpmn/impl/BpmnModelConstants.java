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
package org.flowable.bpm.model.bpmn.impl;

/**
 * Constants used in the BPMN 2.0 Language (DI + Semantic)
 */
public final class BpmnModelConstants {

    /** The XSI namespace */
    public static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    /** The BPMN 2.0 namespace */
    public static final String BPMN20_NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";

    /** The BPMNDI namespace */
    public static final String BPMNDI_NS = "http://www.omg.org/spec/BPMN/20100524/DI";

    /** The DC namespace */
    public static final String DC_NS = "http://www.omg.org/spec/DD/20100524/DC";

    /** The DI namespace */
    public static final String DI_NS = "http://www.omg.org/spec/DD/20100524/DI";

    /** The location of the BPMN 2.0 XML schema. */
    public static final String BPMN_20_SCHEMA_LOCATION = "BPMN20.xsd";

    /** Xml Schema is the default type language */
    public static final String XML_SCHEMA_NS = "http://www.w3.org/2001/XMLSchema";

    public static final String XPATH_NS = "http://www.w3.org/1999/XPath";

    /**
     * @deprecated use {@link #FLOWABLE_NS}
     */
    @Deprecated
    public static final String ACTIVITI_NS = "http://activiti.org/bpmn";

    /** FLOWABLE_NS namespace */
    public static final String FLOWABLE_NS = "http://flowable.org/bpmn";

    // elements ////////////////////////////////////////

    public static final String BPMN_ELEMENT_BASE_ELEMENT = "baseElement";
    public static final String BPMN_ELEMENT_DEFINITIONS = "definitions";
    public static final String BPMN_ELEMENT_DOCUMENTATION = "documentation";
    public static final String BPMN_ELEMENT_EXTENSION = "extension";
    public static final String BPMN_ELEMENT_EXTENSION_ELEMENTS = "extensionElements";
    public static final String BPMN_ELEMENT_IMPORT = "import";
    public static final String BPMN_ELEMENT_RELATIONSHIP = "relationship";
    public static final String BPMN_ELEMENT_SOURCE = "source";
    public static final String BPMN_ELEMENT_TARGET = "target";
    public static final String BPMN_ELEMENT_ROOT_ELEMENT = "rootElement";
    public static final String BPMN_ELEMENT_AUDITING = "auditing";
    public static final String BPMN_ELEMENT_MONITORING = "monitoring";
    public static final String BPMN_ELEMENT_CATEGORY_VALUE = "categoryValue";
    public static final String BPMN_ELEMENT_FLOW_ELEMENT = "flowElement";
    public static final String BPMN_ELEMENT_FLOW_NODE = "flowNode";
    public static final String BPMN_ELEMENT_CATEGORY_VALUE_REF = "categoryValueRef";
    public static final String BPMN_ELEMENT_EXPRESSION = "expression";
    public static final String BPMN_ELEMENT_CONDITION_EXPRESSION = "conditionExpression";
    public static final String BPMN_ELEMENT_SEQUENCE_FLOW = "sequenceFlow";
    public static final String BPMN_ELEMENT_INCOMING = "incoming";
    public static final String BPMN_ELEMENT_OUTGOING = "outgoing";
    public static final String BPMN_ELEMENT_DATA_STATE = "dataState";
    public static final String BPMN_ELEMENT_ITEM_DEFINITION = "itemDefinition";
    public static final String BPMN_ELEMENT_ERROR = "error";
    public static final String BPMN_ELEMENT_IN_MESSAGE_REF = "inMessageRef";
    public static final String BPMN_ELEMENT_OUT_MESSAGE_REF = "outMessageRef";
    public static final String BPMN_ELEMENT_ERROR_REF = "errorRef";
    public static final String BPMN_ELEMENT_OPERATION = "operation";
    public static final String BPMN_ELEMENT_IMPLEMENTATION_REF = "implementationRef";
    public static final String BPMN_ELEMENT_OPERATION_REF = "operationRef";
    public static final String BPMN_ELEMENT_DATA_OUTPUT = "dataOutput";
    public static final String BPMN_ELEMENT_FROM = "from";
    public static final String BPMN_ELEMENT_TO = "to";
    public static final String BPMN_ELEMENT_ASSIGNMENT = "assignment";
    public static final String BPMN_ELEMENT_ITEM_AWARE_ELEMENT = "itemAwareElement";
    public static final String BPMN_ELEMENT_DATA_OBJECT = "dataObject";
    public static final String BPMN_ELEMENT_DATA_OBJECT_REFERENCE = "dataObjectReference";
    public static final String BPMN_ELEMENT_DATA_STORE = "dataStore";
    public static final String BPMN_ELEMENT_DATA_INPUT = "dataInput";
    public static final String BPMN_ELEMENT_FORMAL_EXPRESSION = "formalExpression";
    public static final String BPMN_ELEMENT_DATA_ASSOCIATION = "dataAssociation";
    public static final String BPMN_ELEMENT_SOURCE_REF = "sourceRef";
    public static final String BPMN_ELEMENT_TARGET_REF = "targetRef";
    public static final String BPMN_ELEMENT_TRANSFORMATION = "transformation";
    public static final String BPMN_ELEMENT_DATA_INPUT_ASSOCIATION = "dataInputAssociation";
    public static final String BPMN_ELEMENT_DATA_OUTPUT_ASSOCIATION = "dataOutputAssociation";
    public static final String BPMN_ELEMENT_INPUT_SET = "inputSet";
    public static final String BPMN_ELEMENT_OUTPUT_SET = "outputSet";
    public static final String BPMN_ELEMENT_DATA_INPUT_REFS = "dataInputRefs";
    public static final String BPMN_ELEMENT_OPTIONAL_INPUT_REFS = "optionalInputRefs";
    public static final String BPMN_ELEMENT_WHILE_EXECUTING_INPUT_REFS = "whileExecutingInputRefs";
    public static final String BPMN_ELEMENT_OUTPUT_SET_REFS = "outputSetRefs";
    public static final String BPMN_ELEMENT_DATA_OUTPUT_REFS = "dataOutputRefs";
    public static final String BPMN_ELEMENT_OPTIONAL_OUTPUT_REFS = "optionalOutputRefs";
    public static final String BPMN_ELEMENT_WHILE_EXECUTING_OUTPUT_REFS = "whileExecutingOutputRefs";
    public static final String BPMN_ELEMENT_INPUT_SET_REFS = "inputSetRefs";
    public static final String BPMN_ELEMENT_CATCH_EVENT = "catchEvent";
    public static final String BPMN_ELEMENT_THROW_EVENT = "throwEvent";
    public static final String BPMN_ELEMENT_END_EVENT = "endEvent";
    public static final String BPMN_ELEMENT_IO_SPECIFICATION = "ioSpecification";
    public static final String BPMN_ELEMENT_LOOP_CHARACTERISTICS = "loopCharacteristics";
    public static final String BPMN_ELEMENT_RESOURCE_PARAMETER = "resourceParameter";
    public static final String BPMN_ELEMENT_RESOURCE = "resource";
    public static final String BPMN_ELEMENT_RESOURCE_PARAMETER_BINDING = "resourceParameterBinding";
    public static final String BPMN_ELEMENT_RESOURCE_ASSIGNMENT_EXPRESSION = "resourceAssignmentExpression";
    public static final String BPMN_ELEMENT_RESOURCE_ROLE = "resourceRole";
    public static final String BPMN_ELEMENT_RESOURCE_REF = "resourceRef";
    public static final String BPMN_ELEMENT_PERFORMER = "performer";
    public static final String BPMN_ELEMENT_HUMAN_PERFORMER = "humanPerformer";
    public static final String BPMN_ELEMENT_POTENTIAL_OWNER = "potentialOwner";
    public static final String BPMN_ELEMENT_ACTIVITY = "activity";
    public static final String BPMN_ELEMENT_IO_BINDING = "ioBinding";
    public static final String BPMN_ELEMENT_INTERFACE = "interface";
    public static final String BPMN_ELEMENT_EVENT = "event";
    public static final String BPMN_ELEMENT_MESSAGE = "message";
    public static final String BPMN_ELEMENT_START_EVENT = "startEvent";
    public static final String BPMN_ELEMENT_PROPERTY = "property";
    public static final String BPMN_ELEMENT_EVENT_DEFINITION = "eventDefinition";
    public static final String BPMN_ELEMENT_EVENT_DEFINITION_REF = "eventDefinitionRef";
    public static final String BPMN_ELEMENT_MESSAGE_EVENT_DEFINITION = "messageEventDefinition";
    public static final String BPMN_ELEMENT_CANCEL_EVENT_DEFINITION = "cancelEventDefinition";
    public static final String BPMN_ELEMENT_COMPENSATE_EVENT_DEFINITION = "compensateEventDefinition";
    public static final String BPMN_ELEMENT_CONDITIONAL_EVENT_DEFINITION = "conditionalEventDefinition";
    public static final String BPMN_ELEMENT_CONDITION = "condition";
    public static final String BPMN_ELEMENT_ERROR_EVENT_DEFINITION = "errorEventDefinition";
    public static final String BPMN_ELEMENT_LINK_EVENT_DEFINITION = "linkEventDefinition";
    public static final String BPMN_ELEMENT_SIGNAL_EVENT_DEFINITION = "signalEventDefinition";
    public static final String BPMN_ELEMENT_TERMINATE_EVENT_DEFINITION = "terminateEventDefinition";
    public static final String BPMN_ELEMENT_TIMER_EVENT_DEFINITION = "timerEventDefinition";
    public static final String BPMN_ELEMENT_SUPPORTED_INTERFACE_REF = "supportedInterfaceRef";
    public static final String BPMN_ELEMENT_CALLABLE_ELEMENT = "callableElement";
    public static final String BPMN_ELEMENT_PARTITION_ELEMENT = "partitionElement";
    public static final String BPMN_ELEMENT_FLOW_NODE_REF = "flowNodeRef";
    public static final String BPMN_ELEMENT_CHILD_LANE_SET = "childLaneSet";
    public static final String BPMN_ELEMENT_LANE_SET = "laneSet";
    public static final String BPMN_ELEMENT_LANE = "lane";
    public static final String BPMN_ELEMENT_ARTIFACT = "artifact";
    public static final String BPMN_ELEMENT_CORRELATION_PROPERTY_RETRIEVAL_EXPRESSION = "correlationPropertyRetrievalExpression";
    public static final String BPMN_ELEMENT_MESSAGE_PATH = "messagePath";
    public static final String BPMN_ELEMENT_DATA_PATH = "dataPath";
    public static final String BPMN_ELEMENT_CALL_ACTIVITY = "callActivity";
    public static final String BPMN_ELEMENT_CORRELATION_PROPERTY_BINDING = "correlationPropertyBinding";
    public static final String BPMN_ELEMENT_CORRELATION_PROPERTY = "correlationProperty";
    public static final String BPMN_ELEMENT_CORRELATION_PROPERTY_REF = "correlationPropertyRef";
    public static final String BPMN_ELEMENT_CORRELATION_KEY = "correlationKey";
    public static final String BPMN_ELEMENT_CORRELATION_SUBSCRIPTION = "correlationSubscription";
    public static final String BPMN_ELEMENT_SUPPORTS = "supports";
    public static final String BPMN_ELEMENT_PROCESS = "process";
    public static final String BPMN_ELEMENT_TASK = "task";
    public static final String BPMN_ELEMENT_SEND_TASK = "sendTask";
    public static final String BPMN_ELEMENT_SERVICE_TASK = "serviceTask";
    public static final String BPMN_ELEMENT_SCRIPT_TASK = "scriptTask";
    public static final String BPMN_ELEMENT_USER_TASK = "userTask";
    public static final String BPMN_ELEMENT_RECEIVE_TASK = "receiveTask";
    public static final String BPMN_ELEMENT_BUSINESS_RULE_TASK = "businessRuleTask";
    public static final String BPMN_ELEMENT_MANUAL_TASK = "manualTask";
    public static final String BPMN_ELEMENT_SCRIPT = "script";
    public static final String BPMN_ELEMENT_RENDERING = "rendering";
    public static final String BPMN_ELEMENT_BOUNDARY_EVENT = "boundaryEvent";
    public static final String BPMN_ELEMENT_SUB_PROCESS = "subProcess";
    public static final String BPMN_ELEMENT_TRANSACTION = "transaction";
    public static final String BPMN_ELEMENT_GATEWAY = "gateway";
    public static final String BPMN_ELEMENT_PARALLEL_GATEWAY = "parallelGateway";
    public static final String BPMN_ELEMENT_EXCLUSIVE_GATEWAY = "exclusiveGateway";
    public static final String BPMN_ELEMENT_INTERMEDIATE_CATCH_EVENT = "intermediateCatchEvent";
    public static final String BPMN_ELEMENT_INTERMEDIATE_THROW_EVENT = "intermediateThrowEvent";
    public static final String BPMN_ELEMENT_END_POINT = "endPoint";
    public static final String BPMN_ELEMENT_PARTICIPANT_MULTIPLICITY = "participantMultiplicity";
    public static final String BPMN_ELEMENT_PARTICIPANT = "participant";
    public static final String BPMN_ELEMENT_PARTICIPANT_REF = "participantRef";
    public static final String BPMN_ELEMENT_INTERFACE_REF = "interfaceRef";
    public static final String BPMN_ELEMENT_END_POINT_REF = "endPointRef";
    public static final String BPMN_ELEMENT_MESSAGE_FLOW = "messageFlow";
    public static final String BPMN_ELEMENT_MESSAGE_FLOW_REF = "messageFlowRef";
    public static final String BPMN_ELEMENT_CONVERSATION_NODE = "conversationNode";
    public static final String BPMN_ELEMENT_CONVERSATION = "conversation";
    public static final String BPMN_ELEMENT_SUB_CONVERSATION = "subConversation";
    public static final String BPMN_ELEMENT_GLOBAL_CONVERSATION = "globalConversation";
    public static final String BPMN_ELEMENT_CALL_CONVERSATION = "callConversation";
    public static final String BPMN_ELEMENT_PARTICIPANT_ASSOCIATION = "participantAssociation";
    public static final String BPMN_ELEMENT_INNER_PARTICIPANT_REF = "innerParticipantRef";
    public static final String BPMN_ELEMENT_OUTER_PARTICIPANT_REF = "outerParticipantRef";
    public static final String BPMN_ELEMENT_CONVERSATION_ASSOCIATION = "conversationAssociation";
    public static final String BPMN_ELEMENT_MESSAGE_FLOW_ASSOCIATION = "messageFlowAssociation";
    public static final String BPMN_ELEMENT_CONVERSATION_LINK = "conversationLink";
    public static final String BPMN_ELEMENT_COLLABORATION = "collaboration";
    public static final String BPMN_ELEMENT_ASSOCIATION = "association";
    public static final String BPMN_ELEMENT_SIGNAL = "signal";
    public static final String BPMN_ELEMENT_TIME_DATE = "timeDate";
    public static final String BPMN_ELEMENT_TIME_DURATION = "timeDuration";
    public static final String BPMN_ELEMENT_TIME_CYCLE = "timeCycle";
    public static final String BPMN_ELEMENT_ESCALATION = "escalation";
    public static final String BPMN_ELEMENT_ESCALATION_EVENT_DEFINITION = "escalationEventDefinition";
    public static final String BPMN_ELEMENT_ACTIVATION_CONDITION = "activationCondition";
    public static final String BPMN_ELEMENT_COMPLEX_GATEWAY = "complexGateway";
    public static final String BPMN_ELEMENT_EVENT_BASED_GATEWAY = "eventBasedGateway";
    public static final String BPMN_ELEMENT_INCLUSIVE_GATEWAY = "inclusiveGateway";
    public static final String BPMN_ELEMENT_TEXT_ANNOTATION = "textAnnotation";
    public static final String BPMN_ELEMENT_TEXT = "text";
    public static final String BPMN_ELEMENT_COMPLEX_BEHAVIOR_DEFINITION = "complexBehaviorDefinition";
    public static final String BPMN_ELEMENT_MULTI_INSTANCE_LOOP_CHARACTERISTICS = "multiInstanceLoopCharacteristics";
    public static final String BPMN_ELEMENT_LOOP_CARDINALITY = "loopCardinality";
    public static final String BPMN_ELEMENT_COMPLETION_CONDITION = "completionCondition";
    public static final String BPMN_ELEMENT_OUTPUT_DATA_ITEM = "outputDataItem";
    public static final String BPMN_ELEMENT_INPUT_DATA_ITEM = "inputDataItem";
    public static final String BPMN_ELEMENT_LOOP_DATA_OUTPUT_REF = "loopDataOutputRef";
    public static final String BPMN_ELEMENT_LOOP_DATA_INPUT_REF = "loopDataInputRef";
    public static final String BPMN_ELEMENT_IS_SEQUENTIAL = "isSequential";
    public static final String BPMN_ELEMENT_BEHAVIOR = "behavior";
    public static final String BPMN_ELEMENT_ONE_BEHAVIOR_EVENT_REF = "oneBehaviorEventRef";
    public static final String BPMN_ELEMENT_NONE_BEHAVIOR_EVENT_REF = "noneBehaviorEventRef";

    /** DC */

    public static final String DC_ELEMENT_FONT = "Font";
    public static final String DC_ELEMENT_POINT = "Point";
    public static final String DC_ELEMENT_BOUNDS = "Bounds";

    /** DI */

    public static final String DI_ELEMENT_DIAGRAM_ELEMENT = "DiagramElement";
    public static final String DI_ELEMENT_DIAGRAM = "Diagram";
    public static final String DI_ELEMENT_EDGE = "Edge";
    public static final String DI_ELEMENT_EXTENSION = "extension";
    public static final String DI_ELEMENT_LABELED_EDGE = "LabeledEdge";
    public static final String DI_ELEMENT_LABEL = "Label";
    public static final String DI_ELEMENT_LABELED_SHAPE = "LabeledShape";
    public static final String DI_ELEMENT_NODE = "Node";
    public static final String DI_ELEMENT_PLANE = "Plane";
    public static final String DI_ELEMENT_SHAPE = "Shape";
    public static final String DI_ELEMENT_STYLE = "Style";
    public static final String DI_ELEMENT_WAYPOINT = "waypoint";

    /** BPMNDI */

    public static final String BPMNDI_ELEMENT_BPMN_DIAGRAM = "BPMNDiagram";
    public static final String BPMNDI_ELEMENT_BPMN_PLANE = "BPMNPlane";
    public static final String BPMNDI_ELEMENT_BPMN_LABEL_STYLE = "BPMNLabelStyle";
    public static final String BPMNDI_ELEMENT_BPMN_SHAPE = "BPMNShape";
    public static final String BPMNDI_ELEMENT_BPMN_LABEL = "BPMNLabel";
    public static final String BPMNDI_ELEMENT_BPMN_EDGE = "BPMNEdge";

    /* Flowable extensions */

    public static final String FLOWABLE_ELEMENT_CONNECTOR = "connector";
    public static final String FLOWABLE_ELEMENT_CONNECTOR_ID = "connectorId";
    public static final String FLOWABLE_ELEMENT_CONSTRAINT = "constraint";
    public static final String FLOWABLE_ELEMENT_ENTRY = "entry";
    public static final String FLOWABLE_ELEMENT_EXECUTION_LISTENER = "executionListener";
    public static final String FLOWABLE_ELEMENT_EXPRESSION = "expression";
    public static final String FLOWABLE_ELEMENT_FAILED_JOB_RETRY_TIME_CYCLE = "failedJobRetryTimeCycle";
    public static final String FLOWABLE_ELEMENT_FIELD = "field";
    public static final String FLOWABLE_ELEMENT_FORM_DATA = "formData";
    public static final String FLOWABLE_ELEMENT_FORM_FIELD = "formField";
    public static final String FLOWABLE_ELEMENT_FORM_PROPERTY = "formProperty";
    public static final String FLOWABLE_ELEMENT_IN = "in";
    public static final String FLOWABLE_ELEMENT_INPUT_OUTPUT = "inputOutput";
    public static final String FLOWABLE_ELEMENT_INPUT_PARAMETER = "inputParameter";
    public static final String FLOWABLE_ELEMENT_LIST = "list";
    public static final String FLOWABLE_ELEMENT_MAP = "map";
    public static final String FLOWABLE_ELEMENT_OUTPUT_PARAMETER = "outputParameter";
    public static final String FLOWABLE_ELEMENT_OUT = "out";
    public static final String FLOWABLE_ELEMENT_POTENTIAL_STARTER = "potentialStarter";
    public static final String FLOWABLE_ELEMENT_PROPERTIES = "properties";
    public static final String FLOWABLE_ELEMENT_PROPERTY = "property";
    public static final String FLOWABLE_ELEMENT_SCRIPT = "script";
    public static final String FLOWABLE_ELEMENT_STRING = "string";
    public static final String FLOWABLE_ELEMENT_TASK_LISTENER = "taskListener";
    public static final String FLOWABLE_ELEMENT_VALIDATION = "validation";
    public static final String FLOWABLE_ELEMENT_VALUE = "value";

    // attributes //////////////////////////////////////

    /** XSI attributes **/

    public static final String XSI_ATTRIBUTE_TYPE = "type";

    /** BPMN attributes **/

    public static final String BPMN_ATTRIBUTE_EXPORTER = "exporter";
    public static final String BPMN_ATTRIBUTE_EXPORTER_VERSION = "exporterVersion";
    public static final String BPMN_ATTRIBUTE_EXPRESSION_LANGUAGE = "expressionLanguage";
    public static final String BPMN_ATTRIBUTE_ID = "id";
    public static final String BPMN_ATTRIBUTE_NAME = "name";
    public static final String BPMN_ATTRIBUTE_TARGET_NAMESPACE = "targetNamespace";
    public static final String BPMN_ATTRIBUTE_TYPE_LANGUAGE = "typeLanguage";
    public static final String BPMN_ATTRIBUTE_NAMESPACE = "namespace";
    public static final String BPMN_ATTRIBUTE_LOCATION = "location";
    public static final String BPMN_ATTRIBUTE_IMPORT_TYPE = "importType";
    public static final String BPMN_ATTRIBUTE_TEXT_FORMAT = "textFormat";
    public static final String BPMN_ATTRIBUTE_PROCESS_TYPE = "processType";
    public static final String BPMN_ATTRIBUTE_IS_CLOSED = "isClosed";
    public static final String BPMN_ATTRIBUTE_IS_EXECUTABLE = "isExecutable";
    public static final String BPMN_ATTRIBUTE_MESSAGE_REF = "messageRef";
    public static final String BPMN_ATTRIBUTE_DEFINITION = "definition";
    public static final String BPMN_ATTRIBUTE_MUST_UNDERSTAND = "mustUnderstand";
    public static final String BPMN_ATTRIBUTE_TYPE = "type";
    public static final String BPMN_ATTRIBUTE_DIRECTION = "direction";
    public static final String BPMN_ATTRIBUTE_SOURCE_REF = "sourceRef";
    public static final String BPMN_ATTRIBUTE_TARGET_REF = "targetRef";
    public static final String BPMN_ATTRIBUTE_IS_IMMEDIATE = "isImmediate";
    public static final String BPMN_ATTRIBUTE_VALUE = "value";
    public static final String BPMN_ATTRIBUTE_STRUCTURE_REF = "structureRef";
    public static final String BPMN_ATTRIBUTE_IS_COLLECTION = "isCollection";
    public static final String BPMN_ATTRIBUTE_ITEM_KIND = "itemKind";
    public static final String BPMN_ATTRIBUTE_ITEM_REF = "itemRef";
    public static final String BPMN_ATTRIBUTE_ITEM_SUBJECT_REF = "itemSubjectRef";
    public static final String BPMN_ATTRIBUTE_ERROR_CODE = "errorCode";
    public static final String BPMN_ATTRIBUTE_LANGUAGE = "language";
    public static final String BPMN_ATTRIBUTE_EVALUATES_TO_TYPE_REF = "evaluatesToTypeRef";
    public static final String BPMN_ATTRIBUTE_PARALLEL_MULTIPLE = "parallelMultiple";
    public static final String BPMN_ATTRIBUTE_IS_INTERRUPTING = "isInterrupting";
    public static final String BPMN_ATTRIBUTE_IS_REQUIRED = "isRequired";
    public static final String BPMN_ATTRIBUTE_PARAMETER_REF = "parameterRef";
    public static final String BPMN_ATTRIBUTE_IS_FOR_COMPENSATION = "isForCompensation";
    public static final String BPMN_ATTRIBUTE_START_QUANTITY = "startQuantity";
    public static final String BPMN_ATTRIBUTE_COMPLETION_QUANTITY = "completionQuantity";
    public static final String BPMN_ATTRIBUTE_DEFAULT = "default";
    public static final String BPMN_ATTRIBUTE_OPERATION_REF = "operationRef";
    public static final String BPMN_ATTRIBUTE_INPUT_DATA_REF = "inputDataRef";
    public static final String BPMN_ATTRIBUTE_OUTPUT_DATA_REF = "outputDataRef";
    public static final String BPMN_ATTRIBUTE_IMPLEMENTATION_REF = "implementationRef";
    public static final String BPMN_ATTRIBUTE_PARTITION_ELEMENT_REF = "partitionElementRef";
    public static final String BPMN_ATTRIBUTE_CORRELATION_PROPERTY_REF = "correlationPropertyRef";
    public static final String BPMN_ATTRIBUTE_CORRELATION_KEY_REF = "correlationKeyRef";
    public static final String BPMN_ATTRIBUTE_IMPLEMENTATION = "implementation";
    public static final String BPMN_ATTRIBUTE_SCRIPT_FORMAT = "scriptFormat";
    public static final String BPMN_ATTRIBUTE_INSTANTIATE = "instantiate";
    public static final String BPMN_ATTRIBUTE_CANCEL_ACTIVITY = "cancelActivity";
    public static final String BPMN_ATTRIBUTE_ATTACHED_TO_REF = "attachedToRef";
    public static final String BPMN_ATTRIBUTE_TRIGGERED_BY_EVENT = "triggeredByEvent";
    public static final String BPMN_ATTRIBUTE_GATEWAY_DIRECTION = "gatewayDirection";
    public static final String BPMN_ATTRIBUTE_CALLED_ELEMENT = "calledElement";
    public static final String BPMN_ATTRIBUTE_MINIMUM = "minimum";
    public static final String BPMN_ATTRIBUTE_MAXIMUM = "maximum";
    public static final String BPMN_ATTRIBUTE_PROCESS_REF = "processRef";
    public static final String BPMN_ATTRIBUTE_CALLED_COLLABORATION_REF = "calledCollaborationRef";
    public static final String BPMN_ATTRIBUTE_INNER_CONVERSATION_NODE_REF = "innerConversationNodeRef";
    public static final String BPMN_ATTRIBUTE_OUTER_CONVERSATION_NODE_REF = "outerConversationNodeRef";
    public static final String BPMN_ATTRIBUTE_INNER_MESSAGE_FLOW_REF = "innerMessageFlowRef";
    public static final String BPMN_ATTRIBUTE_OUTER_MESSAGE_FLOW_REF = "outerMessageFlowRef";
    public static final String BPMN_ATTRIBUTE_ASSOCIATION_DIRECTION = "associationDirection";
    public static final String BPMN_ATTRIBUTE_WAIT_FOR_COMPLETION = "waitForCompletion";
    public static final String BPMN_ATTRIBUTE_ACTIVITY_REF = "activityRef";
    public static final String BPMN_ATTRIBUTE_ERROR_REF = "errorRef";
    public static final String BPMN_ATTRIBUTE_SIGNAL_REF = "signalRef";
    public static final String BPMN_ATTRIBUTE_ESCALATION_CODE = "escalationCode";
    public static final String BPMN_ATTRIBUTE_ESCALATION_REF = "escalationRef";
    public static final String BPMN_ATTRIBUTE_EVENT_GATEWAY_TYPE = "eventGatewayType";
    public static final String BPMN_ATTRIBUTE_DATA_OBJECT_REF = "dataObjectRef";
    public static final String BPMN_ATTRIBUTE_METHOD = "method";
    public static final String BPMN_ATTRIBUTE_CAPACITY = "capacity";
    public static final String BPMN_ATTRIBUTE_IS_UNLIMITED = "isUnlimited";

    /** DC */

    public static final String DC_ATTRIBUTE_NAME = "name";
    public static final String DC_ATTRIBUTE_SIZE = "size";
    public static final String DC_ATTRIBUTE_IS_BOLD = "isBold";
    public static final String DC_ATTRIBUTE_IS_ITALIC = "isItalic";
    public static final String DC_ATTRIBUTE_IS_UNDERLINE = "isUnderline";
    public static final String DC_ATTRIBUTE_IS_STRIKE_THROUGH = "isStrikeThrough";
    public static final String DC_ATTRIBUTE_X = "x";
    public static final String DC_ATTRIBUTE_Y = "y";
    public static final String DC_ATTRIBUTE_WIDTH = "width";
    public static final String DC_ATTRIBUTE_HEIGHT = "height";

    /** DI */

    public static final String DI_ATTRIBUTE_ID = "id";
    public static final String DI_ATTRIBUTE_NAME = "name";
    public static final String DI_ATTRIBUTE_DOCUMENTATION = "documentation";
    public static final String DI_ATTRIBUTE_RESOLUTION = "resolution";

    /** BPMNDI */

    public static final String BPMNDI_ATTRIBUTE_BPMN_ELEMENT = "bpmnElement";
    public static final String BPMNDI_ATTRIBUTE_SOURCE_ELEMENT = "sourceElement";
    public static final String BPMNDI_ATTRIBUTE_TARGET_ELEMENT = "targetElement";
    public static final String BPMNDI_ATTRIBUTE_MESSAGE_VISIBLE_KIND = "messageVisibleKind";
    public static final String BPMNDI_ATTRIBUTE_IS_HORIZONTAL = "isHorizontal";
    public static final String BPMNDI_ATTRIBUTE_IS_EXPANDED = "isExpanded";
    public static final String BPMNDI_ATTRIBUTE_IS_MARKER_VISIBLE = "isMarkerVisible";
    public static final String BPMNDI_ATTRIBUTE_IS_MESSAGE_VISIBLE = "isMessageVisible";
    public static final String BPMNDI_ATTRIBUTE_PARTICIPANT_BAND_KIND = "participantBandKind";
    public static final String BPMNDI_ATTRIBUTE_CHOREOGRAPHY_ACTIVITY_SHAPE = "choreographyActivityShape";
    public static final String BPMNDI_ATTRIBUTE_LABEL_STYLE = "labelStyle";

    /* Flowable extensions */

    public static final String FLOWABLE_ATTRIBUTE_ASSIGNEE = "assignee";
    public static final String FLOWABLE_ATTRIBUTE_ASYNC = "async";
    public static final String FLOWABLE_ATTRIBUTE_CANDIDATE_GROUPS = "candidateGroups";
    public static final String FLOWABLE_ATTRIBUTE_CANDIDATE_STARTER_GROUPS = "candidateStarterGroups";
    public static final String FLOWABLE_ATTRIBUTE_CANDIDATE_STARTER_USERS = "candidateStarterUsers";
    public static final String FLOWABLE_ATTRIBUTE_CANDIDATE_USERS = "candidateUsers";
    public static final String FLOWABLE_ATTRIBUTE_CLASS = "class";
    public static final String FLOWABLE_ATTRIBUTE_COLLECTION = "collection";
    public static final String FLOWABLE_ATTRIBUTE_DELEGATE_EXPRESSION = "delegateExpression";
    public static final String FLOWABLE_ATTRIBUTE_DUE_DATE = "dueDate";
    public static final String FLOWABLE_ATTRIBUTE_ELEMENT_VARIABLE = "elementVariable";
    public static final String FLOWABLE_ATTRIBUTE_EVENT = "event";
    public static final String FLOWABLE_ATTRIBUTE_EXCLUSIVE = "exclusive";
    public static final String FLOWABLE_ATTRIBUTE_EXPRESSION = "expression";
    public static final String FLOWABLE_ATTRIBUTE_FORM_HANDLER_CLASS = "formHandlerClass";
    public static final String FLOWABLE_ATTRIBUTE_FORM_KEY = "formKey";
    public static final String FLOWABLE_ATTRIBUTE_ID = "id";
    public static final String FLOWABLE_ATTRIBUTE_INITIATOR = "initiator";
    public static final String FLOWABLE_ATTRIBUTE_NAME = "name";
    public static final String FLOWABLE_ATTRIBUTE_PRIORITY = "priority";
    public static final String FLOWABLE_ATTRIBUTE_READABLE = "readable";
    public static final String FLOWABLE_ATTRIBUTE_REQUIRED = "required";
    public static final String FLOWABLE_ATTRIBUTE_RESOURCE = "resource";
    public static final String FLOWABLE_ATTRIBUTE_RESULT_VARIABLE = "resultVariable";
    public static final String FLOWABLE_ATTRIBUTE_SOURCE = "source";
    public static final String FLOWABLE_ATTRIBUTE_SOURCE_EXPRESSION = "sourceExpression";
    public static final String FLOWABLE_ATTRIBUTE_STRING_VALUE = "stringValue";
    public static final String FLOWABLE_ATTRIBUTE_TARGET = "target";
    public static final String FLOWABLE_ATTRIBUTE_TYPE = "type";
    public static final String FLOWABLE_ATTRIBUTE_VALUE = "value";
    public static final String FLOWABLE_ATTRIBUTE_VARIABLE = "variable";
    // public static final String FLOWABLE_ATTRIBUTE_VARIABLES = "variables";
    public static final String FLOWABLE_ATTRIBUTE_WRITEABLE = "writeable";

    private BpmnModelConstants() {}
}
