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
package org.flowable.app.service.editor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.AppDefinition;
import org.flowable.app.domain.editor.AppModelDefinition;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.domain.editor.ModelHistory;
import org.flowable.app.domain.editor.ModelRelation;
import org.flowable.app.domain.editor.ModelRelationTypes;
import org.flowable.app.model.editor.ModelKeyRepresentation;
import org.flowable.app.model.editor.ModelRepresentation;
import org.flowable.app.model.editor.ReviveModelResultRepresentation;
import org.flowable.app.model.editor.ReviveModelResultRepresentation.UnresolveModelRepresentation;
import org.flowable.app.model.editor.decisiontable.DecisionTableDefinitionRepresentation;
import org.flowable.app.repository.editor.ModelHistoryRepository;
import org.flowable.app.repository.editor.ModelRelationRepository;
import org.flowable.app.repository.editor.ModelRepository;
import org.flowable.app.repository.editor.ModelSort;
import org.flowable.app.security.SecurityUtils;
import org.flowable.app.service.api.ModelService;
import org.flowable.app.service.exception.BadRequestException;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.app.service.exception.NotFoundException;
import org.flowable.app.util.XmlUtil;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.UserTask;
import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverter;
import org.flowable.cmmn.editor.json.converter.util.CmmnModelJsonConverterUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.editor.constants.StencilConstants;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.editor.language.json.converter.util.JsonConverterUtil;
import org.flowable.form.model.FormModel;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.flowable.app.rest.HttpRequestHelper.executeHttpGet;

@Service
@Transactional
public class ModelServiceImpl implements ModelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelServiceImpl.class);
    private static final int CIRCLE_SIZE = 30;

    public static final String NAMESPACE = "http://flowable.org/modeler";

    protected static final String PROCESS_NOT_FOUND_MESSAGE_KEY = "PROCESS.ERROR.NOT-FOUND";
    public static final int UPPER_LEFT_Y = 10;
    public static final int DELTA_X = 100;
    public static final int NODE_SIZE_X = 150;
    private static final int SUBPROCESS_BORDER = 5;

    @Autowired
    protected ModelImageService modelImageService;

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    protected ModelHistoryRepository modelHistoryRepository;

    @Autowired
    protected ModelRelationRepository modelRelationRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected Environment environment;

    protected BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
    protected BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();

    protected CmmnJsonConverter cmmnJsonConverter = new CmmnJsonConverter();

    protected CmmnXmlConverter cmmnXMLConverter = new CmmnXmlConverter();

    @Override
    public Model getModel(String modelId) {
        Model model = modelRepository.get(modelId);

        if (model == null) {
            NotFoundException modelNotFound = new NotFoundException("No model found with the given id: " + modelId);
            modelNotFound.setMessageKey(PROCESS_NOT_FOUND_MESSAGE_KEY);
            throw modelNotFound;
        }

        return model;
    }

    public ModelRepresentation getModelRepresentation(String modelId) {
        Model model = getModel(modelId);
        return new ModelRepresentation(model);
    }

    @Override
    public List<AbstractModel> getModelsByModelType(Integer modelType) {
        return new ArrayList<AbstractModel>(modelRepository.findByModelType(modelType, ModelSort.NAME_ASC));
    }

    @Override
    public ModelHistory getModelHistory(String modelId, String modelHistoryId) {
        // Check if the user has read-rights on the process-model in order to fetch history
        Model model = getModel(modelId);
        ModelHistory modelHistory = modelHistoryRepository.get(modelHistoryId);

        // Check if history corresponds to the current model and is not deleted
        if (modelHistory == null || modelHistory.getRemovalDate() != null || !modelHistory.getModelId().equals(model.getId())) {
            throw new NotFoundException("Process model history not found: " + modelHistoryId);
        }
        return modelHistory;
    }

    @Override
    public byte[] getBpmnXML(AbstractModel model) {
        BpmnModel bpmnModel = getBpmnModel(model);
        return getBpmnXML(bpmnModel);
    }

    @Override
    public byte[] getBpmnXML(BpmnModel bpmnModel) {
        for (Process process : bpmnModel.getProcesses()) {
            if (StringUtils.isNotEmpty(process.getId())) {
                char firstCharacter = process.getId().charAt(0);
                // no digit is allowed as first character
                if (Character.isDigit(firstCharacter)) {
                    process.setId("a" + process.getId());
                }
            }
        }
        byte[] xmlBytes = bpmnXMLConverter.convertToXML(bpmnModel);
        return xmlBytes;
    }

    @Override
    public byte[] getCmmnXML(AbstractModel model) {
        CmmnModel cmmnModel = getCmmnModel(model);
        return getCmmnXML(cmmnModel);
    }

    @Override
    public byte[] getCmmnXML(CmmnModel cmmnModel) {
        byte[] xmlBytes = cmmnXMLConverter.convertToXML(cmmnModel);
        return xmlBytes;
    }

    public ModelKeyRepresentation validateModelKey(Model model, Integer modelType, String key) {
        ModelKeyRepresentation modelKeyResponse = new ModelKeyRepresentation();
        modelKeyResponse.setKey(key);

        List<Model> models = modelRepository.findByKeyAndType(key, modelType);
        for (Model modelInfo : models) {
            if (model == null || !modelInfo.getId().equals(model.getId())) {
                modelKeyResponse.setKeyAlreadyExists(true);
                modelKeyResponse.setId(modelInfo.getId());
                modelKeyResponse.setName(modelInfo.getName());
                break;
            }
        }

        return modelKeyResponse;
    }

    @Override
    public String createModelJson(ModelRepresentation modelRepresentation, String skeletonId) {
        String json;
        if (modelRepresentation.getModelType() != null && modelRepresentation.getModelType().equals(AbstractModel.MODEL_TYPE_FORM)) {
            try {
                json = objectMapper.writeValueAsString(new FormModel());
            } catch (Exception e) {
                LOGGER.error("Error creating form model", e);
                throw new InternalServerErrorException("Error creating form");
            }

        } else if (modelRepresentation.getModelType() != null && modelRepresentation.getModelType().equals(AbstractModel.MODEL_TYPE_DECISION_TABLE)) {
            try {
                DecisionTableDefinitionRepresentation decisionTableDefinition = new DecisionTableDefinitionRepresentation();

                String decisionTableDefinitionKey = modelRepresentation.getName().replaceAll(" ", "");
                decisionTableDefinition.setKey(decisionTableDefinitionKey);

                json = objectMapper.writeValueAsString(decisionTableDefinition);
            } catch (Exception e) {
                LOGGER.error("Error creating decision table model", e);
                throw new InternalServerErrorException("Error creating decision table");
            }

        } else if (modelRepresentation.getModelType() != null && modelRepresentation.getModelType().equals(AbstractModel.MODEL_TYPE_APP)) {
            try {
                json = objectMapper.writeValueAsString(new AppDefinition());
            } catch (Exception e) {
                LOGGER.error("Error creating app definition", e);
                throw new InternalServerErrorException("Error creating app definition");
            }

        } else if (Integer.valueOf(AbstractModel.MODEL_TYPE_CMMN).equals(modelRepresentation.getModelType())) {
            ObjectNode editorNode = objectMapper.createObjectNode();
            editorNode.put("id", "canvas");
            editorNode.put("resourceId", "canvas");
            ObjectNode stencilSetNode = objectMapper.createObjectNode();
            stencilSetNode.put("namespace", "http://b3mn.org/stencilset/cmmn1.1#");
            editorNode.set("stencilset", stencilSetNode);
            ObjectNode propertiesNode = objectMapper.createObjectNode();
            propertiesNode.put("case_id", modelRepresentation.getKey());
            propertiesNode.put("name", modelRepresentation.getName());
            if (StringUtils.isNotEmpty(modelRepresentation.getDescription())) {
                propertiesNode.put("documentation", modelRepresentation.getDescription());
            }
            editorNode.set("properties", propertiesNode);

            ArrayNode childShapeArray = objectMapper.createArrayNode();
            editorNode.set("childShapes", childShapeArray);
            ObjectNode childNode = objectMapper.createObjectNode();
            childShapeArray.add(childNode);
            ObjectNode boundsNode = objectMapper.createObjectNode();
            childNode.set("bounds", boundsNode);
            ObjectNode lowerRightNode = objectMapper.createObjectNode();
            boundsNode.set("lowerRight", lowerRightNode);
            lowerRightNode.put("x", 758);
            lowerRightNode.put("y", 754);
            ObjectNode upperLeftNode = objectMapper.createObjectNode();
            boundsNode.set("upperLeft", upperLeftNode);
            upperLeftNode.put("x", 40);
            upperLeftNode.put("y", 40);
            childNode.set("childShapes", objectMapper.createArrayNode());
            childNode.set("dockers", objectMapper.createArrayNode());
            childNode.set("outgoing", objectMapper.createArrayNode());
            childNode.put("resourceId", "casePlanModel");
            ObjectNode stencilNode = objectMapper.createObjectNode();
            childNode.set("stencil", stencilNode);
            stencilNode.put("id", "CasePlanModel");
            json = editorNode.toString();

        } else {
            json = getInitialProcessModelContent(modelRepresentation, skeletonId);
        }
        return json;
    }


    protected String getInitialProcessModelContent(ModelRepresentation modelRepresentation, String skeletonId) {
        ObjectNode editorNode = createProcessModel(modelRepresentation, isTest(modelRepresentation));
        ArrayNode childShapeArray = objectMapper.createArrayNode();
        editorNode.set("childShapes", childShapeArray);
        JsonNode eventLogEntriesForProcessInstanceId = getEventLogEntriesForProcessInstanceId(skeletonId);

        childShapeArray.addAll(
                eventLogEntriesForProcessInstanceId.size() == 0 ?
                        addSequenceFlows(addUserTask()) :
                        addSequenceFlows(
                                encapsulateInSimulationSubProcess(
                                        addSequenceFlows(
                                                addAssertion(
                                                        createNodesFromEventLogEntries(
                                                                eventLogEntriesForProcessInstanceId
                                                        )
                                                )
                                        )
                                )
                        )
        );

        return editorNode.toString();
    }

    private boolean isTest(ModelRepresentation modelRepresentation) {
        return Integer.valueOf(AbstractModel.MODEL_TYPE_SIMULATION).equals(modelRepresentation.getModelType());
    }

    private List<ObjectNode> addUserTask() {
        List<ObjectNode> nodes = new ArrayList<>();
        nodes.add(createStartEvent(0, "startEvent1"));
        nodes.add(createUserTask(1, "userTask", "User task"));
        nodes.add(createEndEvent(2, "end"));
        return nodes;
    }

    private List<ObjectNode> encapsulateInSimulationSubProcess(List<ObjectNode> testEvents) {
        ObjectNode simulationSubProcess = createSimulationSubProcess(1, "_testSubProcess", testEvents);
        return ImmutableList.<ObjectNode>builder().
                add(createStartEvent(0, "_startTest")).
                add(simulationSubProcess).
                add(
                        createNode(getRightX(simulationSubProcess)+ DELTA_X , UPPER_LEFT_Y + CIRCLE_SIZE / 2, "EndNoneEvent", "_endTest", CIRCLE_SIZE, CIRCLE_SIZE, Collections.<String, String>emptyMap())
                ).
                build();
    }

    protected List<ObjectNode> addAssertion(List<ObjectNode> nodes) {
        nodes.add(nodes.size(), createScriptTask(nodes.size(), "assertion", "assertThat",
                "import static org.flowable.crystalball.simulator.SimUtils.* ;\n" +
                        "import static org.hamcrest.core.Is.is;\n" +
                        "import static org.flowable.engine.test.MatcherAssert.assertThat;\n" +
                        "\n" +
                        "assertThat(getVirtualProcessEngine(execution).getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).count(), is(0L));")
        );
        nodes.add(nodes.size(), createEndEvent(nodes.size(), "end"));
        return nodes;
    }

    protected List<ObjectNode> addSequenceFlows(List<ObjectNode> nodes) {
        if (nodes.size() > 1) {
            ImmutableList.Builder<ObjectNode> nodesBuilder = ImmutableList.builder();
            int taskLength = nodes.size();
            for (int i =1; i< taskLength; i++) {
                ObjectNode sourceNode = nodes.get(i - 1);
                String sequenceFlowId = "sequenceFlow" + i + "-" + UUID.randomUUID();
                ObjectNode targetNode = nodes.get(i);
                String targetId = targetNode.get("resourceId").textValue();

                ArrayNode sourceOutgoing = objectMapper.createArrayNode();
                sourceNode.set("outgoing", sourceOutgoing);
                ObjectNode sequenceFlowResourceId = objectMapper.createObjectNode();
                sequenceFlowResourceId.put("resourceId", sequenceFlowId);
                sourceOutgoing.add(sequenceFlowResourceId);

                ObjectNode sequenceFlow = createNode(getRightX(sourceNode)-CIRCLE_SIZE, UPPER_LEFT_Y+CIRCLE_SIZE, "SequenceFlow", sequenceFlowId, getLeftX(targetNode) - getRightX(sourceNode) + CIRCLE_SIZE, 0, Collections.EMPTY_MAP);
                ArrayNode dockers = objectMapper.createArrayNode();
                sequenceFlow.set("dockers", dockers);
                ObjectNode positionDockerSource = getDockersPosition(sourceNode);
                dockers.add(positionDockerSource);
                ObjectNode positionDockerDestination = getDockersPosition(targetNode);
                dockers.add(positionDockerDestination);

                ArrayNode sequenceFlowOutgoing = objectMapper.createArrayNode();
                sequenceFlow.set("outgoing", sequenceFlowOutgoing);
                ObjectNode targetResourceId = objectMapper.createObjectNode();
                targetResourceId.put("resourceId", targetId);
                sequenceFlowOutgoing.add(targetResourceId);
                sequenceFlow.set("target", targetResourceId);

                nodesBuilder.add(sourceNode);
                nodesBuilder.add(sequenceFlow);
            }
            nodesBuilder.add(nodes.get(taskLength-1));
            return nodesBuilder.build();
        }
        return  nodes;
    }

    protected ObjectNode getDockersPosition(ObjectNode sourceNode) {
        double leftX = sourceNode.get("bounds").get("upperLeft").get("x").asDouble();
        double upperY = sourceNode.get("bounds").get("upperLeft").get("y").asDouble();
        double rightX = sourceNode.get("bounds").get("lowerRight").get("x").asDouble();
        double lowerY = sourceNode.get("bounds").get("lowerRight").get("y").asDouble();
        ObjectNode position = objectMapper.createObjectNode();
        position.put("x", (rightX - leftX)/2);
        position.put("y", (lowerY - upperY) /2);
        return position;
    }

    protected JsonNode getEventLogEntriesForProcessInstanceId(String processInstanceId) {
        if (StringUtils.isNotEmpty(processInstanceId)) {
            String eventLogApiUrl = environment.getRequiredProperty("deployment.api.url");
            String basicAuthUser = environment.getRequiredProperty("idm.admin.user");
            String basicAuthPassword = environment.getRequiredProperty("idm.admin.password");

            if (!eventLogApiUrl.endsWith("/")) {
                eventLogApiUrl = eventLogApiUrl.concat("/");
            }
            eventLogApiUrl = eventLogApiUrl.concat("management/event-log/"+processInstanceId);
            eventLogApiUrl = eventLogApiUrl.concat("?includeSubProcesses=true");
            return executeHttpGet(eventLogApiUrl, basicAuthUser, basicAuthPassword,
                    new Function<HttpResponse, JsonNode>() {
                        @Override
                        public JsonNode apply(HttpResponse response) {
                            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                                try {
                                    return objectMapper.readTree(response.getEntity().getContent());
                                } catch (IOException ioe) {
                                    LOGGER.error("Error calling event log endpoint", ioe);
                                    throw new InternalServerErrorException("Error calling deploy endpoint: " + ioe.getMessage());
                                }
                            } else {
                                LOGGER.error("Invalid event log result code: {}", response.getStatusLine());
                                throw new InternalServerErrorException("Invalid event log result code: " + response.getStatusLine());
                            }
                        }
                    });
        } else {
            return objectMapper.createArrayNode();
        }
    }

    protected ObjectNode createProcessModel(ModelRepresentation modelRepresentation, boolean isTest) {
        ObjectNode editorNode = objectMapper.createObjectNode();
        editorNode.put("id", "canvas");
        editorNode.put("resourceId", "canvas");
        ObjectNode stencilSetNode = objectMapper.createObjectNode();
        stencilSetNode.put("namespace", isTest ? "http://b3mn.org/stencilset/simulation0.1#" : "http://b3mn.org/stencilset/bpmn2.0#");
        editorNode.set("stencilset", stencilSetNode);
        ObjectNode propertiesNode = objectMapper.createObjectNode();
        propertiesNode.put("process_id", modelRepresentation.getKey());
        propertiesNode.put("name", modelRepresentation.getName());
        if (StringUtils.isNotEmpty(modelRepresentation.getDescription())) {
            propertiesNode.put("documentation", modelRepresentation.getDescription());
        }
        editorNode.set("properties", propertiesNode);
        return editorNode;
    }

    protected List<ObjectNode> createNodesFromEventLogEntries(JsonNode eventLogEntries) {
        List<ObjectNode> nodes = new ArrayList<>();
        nodes.add(createStartEvent(0, "startEvent1"));

        int position = 1;
        for (JsonNode eventLogEntry : eventLogEntries) {
            switch (eventLogEntry.get("type").textValue()) {
                case "PROCESSINSTANCE_START":
                    String processDefinitionId = eventLogEntry.get("processDefinitionId").textValue();
                    String key = getProcessDefinitionKey(processDefinitionId);
                    try {
                        StringBuilder variablesScript = getVariablesScript(eventLogEntry);
                        nodes.add(createScriptTask(position++, "startProcess", "Start " + key + " process",
                                "import static org.flowable.crystalball.simulator.SimUtils.*;\n" +
                                        "\n" +
                                        "processInstanceBuilder = getVirtualProcessEngine(execution).getRuntimeService().createProcessInstanceBuilder().processDefinitionKey('" +
                                        key + "');\n" +
                                        variablesScript +
                                        "execution.setVariable('processInstanceId', processInstanceBuilder.start().getId());",
                                false
                        ));
                    } catch (IOException e) {
                        LOGGER.error("Unable to fetch variables from the start process instance event", e);
                        throw new RuntimeException(e);
                    }
                    break;
                case "ACTIVITY_MESSAGE_RECEIVED":
                    try {
                        Object messageName = objectMapper.readValue(eventLogEntry.get("data").binaryValue(), Map.class).get("messageName");
                        nodes.add(createScriptTask(position, "sendMessage" + position++, "Send message " + messageName,

                        "import static org.flowable.crystalball.simulator.SimUtils.*;\n" +
                                "\n" +
                                "runtimeService = getVirtualProcessEngine(execution).getRuntimeService()\n" +
                                "subscription= runtimeService.createEventSubscriptionQuery().processInstanceId(processInstanceId)" +
                                ".eventName(\""+ messageName +"\").singleResult();\n" +
                                "if (subscription == null) {\n" +
                                "   throw new RuntimeException('subscription for "+messageName+" was not found');\n" +
                                "}\n" +
                                "runtimeService.messageEventReceived(\"" + messageName + "\", subscription.getExecutionId());"
                        ));
                    } catch (IOException e) {
                        throw new RuntimeException("eventLog entry [" + eventLogEntry + "] does not have a correct format", e);
                    }
                    break;
                case "TASK_ASSIGNED":
                    try {
                        String taskDefinitionKey = (String) objectMapper.readValue(eventLogEntry.get("data").binaryValue(), Map.class).get("taskDefinitionKey");
                    nodes.add(createScriptTask(position, "claimTask" + position++, "Claim task " +
                                    taskDefinitionKey + " to user "+ eventLogEntry.get("userId"),
                            "import static org.flowable.crystalball.simulator.SimUtils.*;\n" +
                                    "\n" +
                                    "processInstanceIds = getSubProcessInstanceIds(processInstanceId, execution);\n" +
                                    "taskId = getVirtualProcessEngine(execution).getTaskService().createTaskQuery().processInstanceIdIn(processInstanceIds)." +
                                    "taskDefinitionKey(\""+taskDefinitionKey+"\").singleResult().getId();\n" +
                                    "getVirtualProcessEngine(execution).getTaskService().claim(taskId, '"+ eventLogEntry.get("userId").textValue() +"');"
                    ));
                    } catch (IOException e) {
                        throw new RuntimeException("eventLog entry [" + eventLogEntry + "] does not have a correct format", e);
                    }
                    break;
                case "TIMER_FIRED":
                    nodes.add(createScriptTask(position, "fireAllTimers" + position++, "Fire all timers",
                            "import static org.flowable.crystalball.simulator.SimUtils.*;\n" +
                                    "\n" +
                                    "processInstanceIds = getSubProcessInstanceIds(processInstanceId, execution);\n" +
                                    "managementService = getVirtualProcessEngine(execution).getManagementService();\n" +
                                    "for( String processInstanceId : processInstanceIds) {\n" +
                                    "    timers = managementService.createTimerJobQuery().processInstanceId(processInstanceId)." +
                                    "timers().list();\n" +
                                    "    for( org.flowable.engine.runtime.Job timer : timers) {\n" +
                                    "        managementService.executeJob(timer.getId());\n" +
                                    "    }" +
                                    "}\n"
                    ));
                    break;
                case "TASK_COMPLETED":
                    try {
                        String taskDefinitionKey = (String) objectMapper.readValue(eventLogEntry.get("data").binaryValue(), Map.class).get("taskDefinitionKey");
                        nodes.add(createScriptTask(position, "completeTask"+position++, "Complete task "+ taskDefinitionKey,
                            "import static org.flowable.crystalball.simulator.SimUtils.*;\n" +
                                    "\n" +
                                    "processInstanceIds = getSubProcessInstanceIds(processInstanceId, execution);\n" +
                                    "taskId = getVirtualProcessEngine(execution).getTaskService().createTaskQuery()." +
                                    "processInstanceIdIn(processInstanceIds).taskDefinitionKey(\""+taskDefinitionKey+"\").singleResult().getId();\n" +
                                    "getVirtualProcessEngine(execution).getTaskService().complete(taskId);"
                    ));
                    } catch (IOException e) {
                        throw new RuntimeException("eventLog entry [" + eventLogEntry + "] does not have a correct format", e);
                    }
                    break;
                case "DEBUG_LOG_SCRIPT":
                    try {
                        nodes.add(createScriptTask(position, "scriptLog" + position, "Script log " + position++,
                                (String) objectMapper.readValue(eventLogEntry.get("data").binaryValue(), Map.class).get("script")
                        ));
                    } catch (IOException e) {
                        throw new RuntimeException("eventLog entry [" + eventLogEntry + "] does not have a correct format", e);
                    }
                    break;
                default:
                    break;
            }


        }
        return nodes;
    }

    private StringBuilder getVariablesScript(JsonNode eventLogEntry) throws IOException {
        Map<String, Object> variables = (Map<String, Object>) objectMapper.readValue(eventLogEntry.get("data").binaryValue(), Map.class).get("variables");
        StringBuilder setVariablesScript = new StringBuilder();
        if (variables != null) {
            for (Map.Entry<String, Object> variable : variables.entrySet()) {
                setVariablesScript.append("processInstanceBuilder.variable(\"" + variable.getKey() + "\", " + getVariableValue(variable.getValue()) + ");\n");
            }
        }
        return setVariablesScript;
    }

    private String getVariableValue(Object value) {
        if (value instanceof String) {
            return "\"" + value +"\"";
        }
        return value.toString();
    }

    private String getProcessDefinitionKey(String processDefinitionId) {
        String definitionsApiUrl = environment.getRequiredProperty("deployment.api.url");
        String basicAuthUser = environment.getRequiredProperty("idm.admin.user");
        String basicAuthPassword = environment.getRequiredProperty("idm.admin.password");

        if (!definitionsApiUrl.endsWith("/")) {
            definitionsApiUrl = definitionsApiUrl.concat("/");
        }
        definitionsApiUrl = definitionsApiUrl.concat("repository/process-definitions/"+processDefinitionId);

        return executeHttpGet(definitionsApiUrl, basicAuthUser, basicAuthPassword,
                new Function<HttpResponse, String>() {
                    @Override
                    public String apply(HttpResponse response) {
                        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            try {
                                JsonNode jsonNode = objectMapper.readTree(response.getEntity().getContent());
                                return jsonNode.get("key").textValue();
                            } catch (IOException ioe) {
                                LOGGER.error("Error calling deploy endpoint", ioe);
                                throw new InternalServerErrorException("Error calling deploy endpoint: " + ioe.getMessage());
                            }
                        } else {
                            LOGGER.error("Invalid deploy result code: {}", response.getStatusLine());
                            throw new InternalServerErrorException("Invalid deploy result code: " + response.getStatusLine());
                        }
                    }
                }
        );
    }

    protected ObjectNode createSimulationSubProcess(int position, String id, List<ObjectNode> testEvents) {
        HashMap<String, String> properties = new HashMap<>();
        ObjectNode lastNode = testEvents.get(testEvents.size() - 1);
        int rightX = getRightX(lastNode);
        int leftX = DELTA_X + position * NODE_SIZE_X;
        return createNode(leftX, UPPER_LEFT_Y - SUBPROCESS_BORDER, StencilConstants.STENCIL_SIMULATION_SUB_PROCESS, id,
                rightX-leftX + 3 * DELTA_X , 2 * CIRCLE_SIZE + UPPER_LEFT_Y + 2*SUBPROCESS_BORDER,
                properties, objectMapper.createArrayNode().addAll(testEvents));
    }

    private int getRightX(ObjectNode lastNode) {
        return lastNode.get("bounds").get("lowerRight").get("x").asInt();
    }
    private int getLeftX(ObjectNode lastNode) {
        return lastNode.get("bounds").get("upperLeft").get("x").asInt();
    }

    protected ObjectNode createScriptTask(int position, String id, String name, String script, boolean isAsync) {
        HashMap<String, String> properties = new HashMap<>();
        properties.put("scriptformat", "groovy");
        properties.put("scripttext", script);
        properties.put("name", name);
        properties.put("asynchronousdefinition", Boolean.toString(isAsync));
        return createNode(DELTA_X + position * NODE_SIZE_X, UPPER_LEFT_Y, "ScriptTask", id, 3 * CIRCLE_SIZE, 2 * CIRCLE_SIZE,
                properties);
    }

    protected ObjectNode createScriptTask(int position, String id, String name, String script) {
        return createScriptTask( position, id, name, script, true);
    }

    protected ObjectNode createUserTask(int position, String id, String name) {
        HashMap<String, String> properties = new HashMap<>();
        properties.put("name", name);
        return createNode(DELTA_X + position * NODE_SIZE_X, UPPER_LEFT_Y, "UserTask", id, 3 * CIRCLE_SIZE, 2 * CIRCLE_SIZE,
                properties);
    }

    protected ObjectNode createStartEvent(int position, String id) {
        return createNoneEvent(position, id, "StartNoneEvent");
    }

    protected ObjectNode createEndEvent(int position, String id) {
        return createNoneEvent(position, id, "EndNoneEvent");
    }

    protected ObjectNode createNoneEvent(int position, String id, String stencil) {
        return createNode(DELTA_X + position * NODE_SIZE_X, UPPER_LEFT_Y + CIRCLE_SIZE/2, stencil, id, CIRCLE_SIZE, CIRCLE_SIZE, Collections.<String, String>emptyMap());
    }

    protected ObjectNode createNode(int upperLeftX, int upperLeftY, String stencil, String id, int sizeX, int sizeY,
                                    Map<String, String> properties) {
        return createNode( upperLeftX, upperLeftY, stencil, id, sizeX, sizeY,
        properties, this.objectMapper.createArrayNode());
    }

    protected ObjectNode createNode(int upperLeftX, int upperLeftY, String stencil, String id, int sizeX, int sizeY,
                                    Map<String, String> properties, ArrayNode childShapes) {
        ObjectNode childNode = objectMapper.createObjectNode();
        ObjectNode boundsNode = objectMapper.createObjectNode();
        childNode.set("bounds", boundsNode);
        ObjectNode lowerRightNode = objectMapper.createObjectNode();
        boundsNode.set("lowerRight", lowerRightNode);
        lowerRightNode.put("x", upperLeftX + sizeX);
        lowerRightNode.put("y", upperLeftY + sizeY);
        ObjectNode upperLeftNode = objectMapper.createObjectNode();
        boundsNode.set("upperLeft", upperLeftNode);
        upperLeftNode.put("x", upperLeftX);
        upperLeftNode.put("y", upperLeftY);
        childNode.set("childShapes", childShapes);
        childNode.set("dockers", objectMapper.createArrayNode());
        childNode.set("outgoing", objectMapper.createArrayNode());
        childNode.put("resourceId", id);
        ObjectNode stencilNode = objectMapper.createObjectNode();
        childNode.set("stencil", stencilNode);
        stencilNode.put("id", stencil);
        ObjectNode propertiesNode = objectMapper.createObjectNode();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            propertiesNode.put(entry.getKey(), entry.getValue());
        }
        childNode.set("properties", propertiesNode);
        return childNode;
    }

    @Override
    public Model createModel(Model newModel, User createdBy) {
        newModel.setVersion(1);
        newModel.setCreated(Calendar.getInstance().getTime());
        newModel.setCreatedBy(createdBy.getId());
        newModel.setLastUpdated(Calendar.getInstance().getTime());
        newModel.setLastUpdatedBy(createdBy.getId());

        persistModel(newModel);
        return newModel;
    }

    @Override
    public Model createModel(ModelRepresentation model, String editorJson, User createdBy) {
        Model newModel = new Model();
        newModel.setVersion(1);
        newModel.setName(model.getName());
        newModel.setKey(model.getKey());
        newModel.setModelType(model.getModelType());
        newModel.setCreated(Calendar.getInstance().getTime());
        if (createdBy != null) {
            newModel.setCreatedBy(createdBy.getId());
            newModel.setLastUpdatedBy(createdBy.getId());
        }
        newModel.setDescription(model.getDescription());
        newModel.setModelEditorJson(editorJson);
        newModel.setLastUpdated(Calendar.getInstance().getTime());

        persistModel(newModel);
        return newModel;
    }

    public ModelRepresentation importNewVersion(String modelId, String fileName, InputStream modelStream) {
        Model processModel = getModel(modelId);
        User currentUser = SecurityUtils.getCurrentUserObject();

        if (fileName != null && (fileName.endsWith(".bpmn") || fileName.endsWith(".bpmn20.xml"))) {
            try {
                XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
                InputStreamReader xmlIn = new InputStreamReader(modelStream, "UTF-8");
                XMLStreamReader xtr = xif.createXMLStreamReader(xmlIn);
                BpmnModel bpmnModel = bpmnXMLConverter.convertToBpmnModel(xtr);
                if (CollectionUtils.isEmpty(bpmnModel.getProcesses())) {
                    throw new BadRequestException("No process found in definition " + fileName);
                }

                if (bpmnModel.getLocationMap().size() == 0) {
                    throw new BadRequestException("No required BPMN DI information found in definition " + fileName);
                }

                ObjectNode modelNode = bpmnJsonConverter.convertToJson(bpmnModel);

                AbstractModel savedModel = saveModel(modelId, processModel.getName(), processModel.getKey(),
                        processModel.getDescription(), modelNode.toString(), true, "Version import via REST service", currentUser);
                return new ModelRepresentation(savedModel);

            } catch (BadRequestException e) {
                throw e;

            } catch (Exception e) {
                throw new BadRequestException("Import failed for " + fileName + ", error message " + e.getMessage());
            }
        } else {
            throw new BadRequestException("Invalid file name, only .bpmn and .bpmn20.xml files are supported not " + fileName);
        }
    }

    @Override
    public Model createNewModelVersion(Model modelObject, String comment, User updatedBy) {
        return (Model) internalCreateNewModelVersion(modelObject, comment, updatedBy, false);
    }

    @Override
    public ModelHistory createNewModelVersionAndReturnModelHistory(Model modelObject, String comment, User updatedBy) {
        return (ModelHistory) internalCreateNewModelVersion(modelObject, comment, updatedBy, true);
    }

    protected AbstractModel internalCreateNewModelVersion(Model modelObject, String comment, User updatedBy, boolean returnModelHistory) {
        modelObject.setLastUpdated(new Date());
        modelObject.setLastUpdatedBy(updatedBy.getId());
        modelObject.setComment(comment);

        ModelHistory historyModel = createNewModelhistory(modelObject);
        persistModelHistory(historyModel);

        modelObject.setVersion(modelObject.getVersion() + 1);
        persistModel(modelObject);

        return returnModelHistory ? historyModel : modelObject;
    }

    @Override
    public Model saveModel(Model modelObject) {
        return persistModel(modelObject);
    }

    @Override
    public Model saveModel(Model modelObject, String editorJson, byte[] imageBytes, boolean newVersion, String newVersionComment, User updatedBy) {

        return internalSave(modelObject.getName(), modelObject.getKey(), modelObject.getDescription(), editorJson, newVersion,
                newVersionComment, imageBytes, updatedBy, modelObject);
    }

    @Override
    public Model saveModel(String modelId, String name, String key, String description, String editorJson,
                           boolean newVersion, String newVersionComment, User updatedBy) {

        Model modelObject = modelRepository.get(modelId);
        return internalSave(name, key, description, editorJson, newVersion, newVersionComment, null, updatedBy, modelObject);
    }

    protected Model internalSave(String name, String key, String description, String editorJson, boolean newVersion,
                                 String newVersionComment, byte[] imageBytes, User updatedBy, Model modelObject) {

        if (!newVersion) {

            modelObject.setLastUpdated(new Date());
            modelObject.setLastUpdatedBy(updatedBy.getId());
            modelObject.setName(name);
            modelObject.setKey(key);
            modelObject.setDescription(description);
            modelObject.setModelEditorJson(editorJson);

            if (imageBytes != null) {
                modelObject.setThumbnail(imageBytes);
            }

        } else {

            ModelHistory historyModel = createNewModelhistory(modelObject);
            persistModelHistory(historyModel);

            modelObject.setVersion(modelObject.getVersion() + 1);
            modelObject.setLastUpdated(new Date());
            modelObject.setLastUpdatedBy(updatedBy.getId());
            modelObject.setName(name);
            modelObject.setKey(key);
            modelObject.setDescription(description);
            modelObject.setModelEditorJson(editorJson);
            modelObject.setComment(newVersionComment);

            if (imageBytes != null) {
                modelObject.setThumbnail(imageBytes);
            }
        }

        return persistModel(modelObject);
    }

    @Override
    public void deleteModel(String modelId) {

        Model model = modelRepository.get(modelId);
        if (model == null) {
            throw new IllegalArgumentException("No model found with id: " + modelId);
        }

        // Fetch current model history list
        List<ModelHistory> history = modelHistoryRepository.findByModelId(model.getId());

        // Move model to history and mark removed
        ModelHistory historyModel = createNewModelhistory(model);
        historyModel.setRemovalDate(Calendar.getInstance().getTime());
        persistModelHistory(historyModel);

        deleteModelAndChildren(model);
    }

    protected void deleteModelAndChildren(Model model) {

        // Models have relations with each other, in all kind of wicked and funny ways.
        // Hence, we remove first all relations, comments, etc. while collecting all models.
        // Then, once all foreign key problem makers are removed, we remove the models

        List<Model> allModels = new ArrayList<>();
        internalDeleteModelAndChildren(model, allModels);

        for (Model modelToDelete : allModels) {
            modelRepository.delete(modelToDelete);
        }
    }

    protected void internalDeleteModelAndChildren(Model model, List<Model> allModels) {
        // Delete all related data
        modelRelationRepository.deleteModelRelationsForParentModel(model.getId());

        allModels.add(model);
    }

    @Override
    public ReviveModelResultRepresentation reviveProcessModelHistory(ModelHistory modelHistory, User user, String newVersionComment) {
        Model latestModel = modelRepository.get(modelHistory.getModelId());
        if (latestModel == null) {
            throw new IllegalArgumentException("No process model found with id: " + modelHistory.getModelId());
        }

        // Store the current model in history
        ModelHistory latestModelHistory = createNewModelhistory(latestModel);
        persistModelHistory(latestModelHistory);

        // Populate the actual latest version with the properties in the historic model
        latestModel.setVersion(latestModel.getVersion() + 1);
        latestModel.setLastUpdated(new Date());
        latestModel.setLastUpdatedBy(user.getId());
        latestModel.setName(modelHistory.getName());
        latestModel.setKey(modelHistory.getKey());
        latestModel.setDescription(modelHistory.getDescription());
        latestModel.setModelEditorJson(modelHistory.getModelEditorJson());
        latestModel.setModelType(modelHistory.getModelType());
        latestModel.setComment(newVersionComment);
        persistModel(latestModel);

        ReviveModelResultRepresentation result = new ReviveModelResultRepresentation();

        // For apps, we need to make sure the referenced processes exist as models.
        // It could be the user has deleted the process model in the meantime. We give back that info to the user.
        if (latestModel.getModelType() == AbstractModel.MODEL_TYPE_APP) {
            if (StringUtils.isNotEmpty(latestModel.getModelEditorJson())) {
                try {
                    AppDefinition appDefinition = objectMapper.readValue(latestModel.getModelEditorJson(), AppDefinition.class);
                    for (AppModelDefinition appModelDefinition : appDefinition.getModels()) {
                        if (modelRepository.get(appModelDefinition.getId()) == null) {
                            result.getUnresolvedModels().add(new UnresolveModelRepresentation(appModelDefinition.getId(),
                                    appModelDefinition.getName(), appModelDefinition.getLastUpdatedBy()));
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Could not deserialize app model json (id = {})", latestModel.getId(), e);
                }
            }
        }

        return result;
    }

    @Override
    public BpmnModel getBpmnModel(AbstractModel model) {
        BpmnModel bpmnModel = null;
        try {
            Map<String, Model> formMap = new HashMap<>();
            Map<String, Model> decisionTableMap = new HashMap<>();

            List<Model> referencedModels = modelRepository.findByParentModelId(model.getId());
            for (Model childModel : referencedModels) {
                if (Model.MODEL_TYPE_FORM == childModel.getModelType()) {
                    formMap.put(childModel.getId(), childModel);

                } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
                    decisionTableMap.put(childModel.getId(), childModel);
                }
            }

            bpmnModel = getBpmnModel(model, formMap, decisionTableMap);

        } catch (Exception e) {
            LOGGER.error("Could not generate BPMN 2.0 model for {}", model.getId(), e);
            throw new InternalServerErrorException("Could not generate BPMN 2.0 model");
        }

        return bpmnModel;
    }

    @Override
    public BpmnModel getBpmnModel(AbstractModel model, Map<String, Model> formMap, Map<String, Model> decisionTableMap) {
        try {
            ObjectNode editorJsonNode = (ObjectNode) objectMapper.readTree(model.getModelEditorJson());
            Map<String, String> formKeyMap = new HashMap<>();
            for (Model formModel : formMap.values()) {
                formKeyMap.put(formModel.getId(), formModel.getKey());
            }

            Map<String, String> decisionTableKeyMap = new HashMap<>();
            for (Model decisionTableModel : decisionTableMap.values()) {
                decisionTableKeyMap.put(decisionTableModel.getId(), decisionTableModel.getKey());
            }

            return bpmnJsonConverter.convertToBpmnModel(editorJsonNode, formKeyMap, decisionTableKeyMap);

        } catch (Exception e) {
            LOGGER.error("Could not generate BPMN 2.0 model for {}", model.getId(), e);
            throw new InternalServerErrorException("Could not generate BPMN 2.0 model");
        }
    }

    @Override
    public CmmnModel getCmmnModel(AbstractModel model) {
        CmmnModel cmmnModel = null;
        try {
            Map<String, Model> formMap = new HashMap<>();
            Map<String, Model> decisionTableMap = new HashMap<>();
            Map<String, Model> caseModelMap = new HashMap<>();
            Map<String, Model> processModelMap = new HashMap<>();

            List<Model> referencedModels = modelRepository.findByParentModelId(model.getId());
            for (Model childModel : referencedModels) {
                if (Model.MODEL_TYPE_FORM == childModel.getModelType()) {
                    formMap.put(childModel.getId(), childModel);

                } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
                    decisionTableMap.put(childModel.getId(), childModel);

                } else if (Model.MODEL_TYPE_CMMN == childModel.getModelType()) {
                    caseModelMap.put(childModel.getId(), childModel);

                } else if (Model.MODEL_TYPE_BPMN == childModel.getModelType()) {
                    processModelMap.put(childModel.getId(), childModel);
                }
            }

            cmmnModel = getCmmnModel(model, formMap, decisionTableMap, caseModelMap, processModelMap);

        } catch (Exception e) {
            LOGGER.error("Could not generate CMMN model for {}", model.getId(), e);
            throw new InternalServerErrorException("Could not generate CMMN model");
        }

        return cmmnModel;
    }

    @Override
    public CmmnModel getCmmnModel(AbstractModel model, Map<String, Model> formMap, Map<String, Model> decisionTableMap,
                    Map<String, Model> caseModelMap, Map<String, Model> processModelMap) {

        try {
            ObjectNode editorJsonNode = (ObjectNode) objectMapper.readTree(model.getModelEditorJson());
            Map<String, String> formKeyMap = convertToModelKeyMap(formMap);
            Map<String, String> decisionTableKeyMap = convertToModelKeyMap(decisionTableMap);
            Map<String, String> caseModelKeyMap = convertToModelKeyMap(caseModelMap);
            Map<String, String> processModelKeyMap = convertToModelKeyMap(processModelMap);

            return cmmnJsonConverter.convertToCmmnModel(editorJsonNode, formKeyMap, decisionTableKeyMap, caseModelKeyMap, processModelKeyMap);

        } catch (Exception e) {
            LOGGER.error("Could not generate CMMN model for {}", model.getId(), e);
            throw new InternalServerErrorException("Could not generate CMMN model");
        }
    }

    protected void addOrUpdateExtensionElement(String name, String value, UserTask userTask) {
        List<ExtensionElement> extensionElements = userTask.getExtensionElements().get(name);

        ExtensionElement extensionElement;

        if (CollectionUtils.isNotEmpty(extensionElements)) {
            extensionElement = extensionElements.get(0);
        } else {
            extensionElement = new ExtensionElement();
        }
        extensionElement.setNamespace(NAMESPACE);
        extensionElement.setNamespacePrefix("modeler");
        extensionElement.setName(name);
        extensionElement.setElementText(value);

        if (CollectionUtils.isEmpty(extensionElements)) {
            userTask.addExtensionElement(extensionElement);
        }
    }

    public Long getModelCountForUser(User user, int modelType) {
        return modelRepository.countByModelTypeAndCreatedBy(modelType, user.getId());
    }

    protected Model persistModel(Model model) {

        if (StringUtils.isNotEmpty(model.getModelEditorJson())) {

            // Parse json to java
            ObjectNode jsonNode = null;
            try {
                jsonNode = (ObjectNode) objectMapper.readTree(model.getModelEditorJson());
            } catch (Exception e) {
                LOGGER.error("Could not deserialize json model", e);
                throw new InternalServerErrorException("Could not deserialize json model");
            }

            if ((model.getModelType() == null || model.getModelType().intValue() == Model.MODEL_TYPE_BPMN
            || model.getModelType().intValue() == Model.MODEL_TYPE_SIMULATION)) {

                // Thumbnail
                byte[] thumbnail = modelImageService.generateThumbnailImage(model, jsonNode);
                if (thumbnail != null) {
                    model.setThumbnail(thumbnail);
                }

                modelRepository.save(model);

                // Relations
                handleBpmnProcessFormModelRelations(model, jsonNode);
                handleBpmnProcessDecisionTaskModelRelations(model, jsonNode);

            } else if (model.getModelType().intValue() == Model.MODEL_TYPE_CMMN) {

                // Thumbnail
                byte[] thumbnail = modelImageService.generateCmmnThumbnailImage(model, jsonNode);
                if (thumbnail != null) {
                    model.setThumbnail(thumbnail);
                }

                modelRepository.save(model);

                // Relations
                handleCmmnFormModelRelations(model, jsonNode);
                handleCmmnDecisionModelRelations(model, jsonNode);
                handleCmmnCaseModelRelations(model, jsonNode);
                handleCmmnProcessModelRelations(model, jsonNode);

            } else if (model.getModelType().intValue() == Model.MODEL_TYPE_FORM ||
                    model.getModelType().intValue() == Model.MODEL_TYPE_DECISION_TABLE) {

                jsonNode.put("name", model.getName());
                jsonNode.put("key", model.getKey());
                modelRepository.save(model);

            } else if (model.getModelType().intValue() == Model.MODEL_TYPE_APP) {

                modelRepository.save(model);
                handleAppModelProcessRelations(model, jsonNode);
            }
        }

        return model;
    }

    protected void persistModelHistory(ModelHistory modelHistory) {
        modelHistoryRepository.save(modelHistory);
    }

    protected void handleBpmnProcessFormModelRelations(AbstractModel bpmnProcessModel, ObjectNode editorJsonNode) {
        List<JsonNode> formReferenceNodes = JsonConverterUtil.filterOutJsonNodes(JsonConverterUtil.getBpmnProcessModelFormReferences(editorJsonNode));
        Set<String> formIds = JsonConverterUtil.gatherStringPropertyFromJsonNodes(formReferenceNodes, "id");

        handleModelRelations(bpmnProcessModel, formIds, ModelRelationTypes.TYPE_FORM_MODEL_CHILD);
    }

    protected void handleBpmnProcessDecisionTaskModelRelations(AbstractModel bpmnProcessModel, ObjectNode editorJsonNode) {
        List<JsonNode> decisionTableNodes = JsonConverterUtil.filterOutJsonNodes(JsonConverterUtil.getBpmnProcessModelDecisionTableReferences(editorJsonNode));
        Set<String> decisionTableIds = JsonConverterUtil.gatherStringPropertyFromJsonNodes(decisionTableNodes, "id");

        handleModelRelations(bpmnProcessModel, decisionTableIds, ModelRelationTypes.TYPE_DECISION_TABLE_MODEL_CHILD);
    }

    protected void handleCmmnFormModelRelations(AbstractModel caseModel, ObjectNode editorJsonNode) {
        List<JsonNode> formReferenceNodes = CmmnModelJsonConverterUtil.filterOutJsonNodes(CmmnModelJsonConverterUtil.getCmmnModelFormReferences(editorJsonNode));
        Set<String> formIds = JsonConverterUtil.gatherStringPropertyFromJsonNodes(formReferenceNodes, "id");

        handleModelRelations(caseModel, formIds, ModelRelationTypes.TYPE_FORM_MODEL_CHILD);
    }

    protected void handleCmmnDecisionModelRelations(AbstractModel caseModel, ObjectNode editorJsonNode) {
        List<JsonNode> processReferenceNodes = CmmnModelJsonConverterUtil.filterOutJsonNodes(CmmnModelJsonConverterUtil.getCmmnModelDecisionTableReferences(editorJsonNode));
        Set<String> processModelIds = JsonConverterUtil.gatherStringPropertyFromJsonNodes(processReferenceNodes, "id");

        handleModelRelations(caseModel, processModelIds, ModelRelationTypes.TYPE_DECISION_TABLE_MODEL_CHILD);
    }

    protected void handleCmmnCaseModelRelations(AbstractModel caseModel, ObjectNode editorJsonNode) {
        List<JsonNode> caseReferenceNodes = CmmnModelJsonConverterUtil.filterOutJsonNodes(CmmnModelJsonConverterUtil.getCmmnModelCaseReferences(editorJsonNode));
        Set<String> caseModelIds = JsonConverterUtil.gatherStringPropertyFromJsonNodes(caseReferenceNodes, "id");

        handleModelRelations(caseModel, caseModelIds, ModelRelationTypes.TYPE_CASE_MODEL_CHILD);
    }

    protected void handleCmmnProcessModelRelations(AbstractModel caseModel, ObjectNode editorJsonNode) {
        List<JsonNode> processReferenceNodes = CmmnModelJsonConverterUtil.filterOutJsonNodes(CmmnModelJsonConverterUtil.getCmmnModelProcessReferences(editorJsonNode));
        Set<String> processModelIds = JsonConverterUtil.gatherStringPropertyFromJsonNodes(processReferenceNodes, "id");

        handleModelRelations(caseModel, processModelIds, ModelRelationTypes.TYPE_PROCESS_MODEL_CHILD);
    }

    protected void handleAppModelProcessRelations(AbstractModel appModel, ObjectNode appModelJsonNode) {
        Set<String> processModelIds = JsonConverterUtil.getAppModelReferencedModelIds(appModelJsonNode);
        handleModelRelations(appModel, processModelIds, ModelRelationTypes.TYPE_PROCESS_MODEL);
    }

    /**
     * Generic handling of model relations: deleting/adding where needed.
     */
    protected void handleModelRelations(AbstractModel bpmnProcessModel, Set<String> idsReferencedInJson, String relationshipType) {

        // Find existing persisted relations
        List<ModelRelation> persistedModelRelations = modelRelationRepository.findByParentModelIdAndType(bpmnProcessModel.getId(), relationshipType);

        // if no ids referenced now, just delete them all
        if (idsReferencedInJson == null || idsReferencedInJson.size() == 0) {
            for (ModelRelation modelRelation : persistedModelRelations) {
                modelRelationRepository.delete(modelRelation);
            }
            return;
        }

        Set<String> alreadyPersistedModelIds = new HashSet<>(persistedModelRelations.size());
        for (ModelRelation persistedModelRelation : persistedModelRelations) {
            if (!idsReferencedInJson.contains(persistedModelRelation.getModelId())) {
                // model used to be referenced, but not anymore. Delete it.
                modelRelationRepository.delete(persistedModelRelation);
            } else {
                alreadyPersistedModelIds.add(persistedModelRelation.getModelId());
            }
        }

        // Loop over all referenced ids and see which one are new
        for (String idReferencedInJson : idsReferencedInJson) {

            // if model is referenced, but it is not yet persisted = create it
            if (!alreadyPersistedModelIds.contains(idReferencedInJson)) {

                // Check if model actually still exists. Don't create the relationship if it doesn't exist. The client UI will have cope with this too.
                if (modelRepository.get(idReferencedInJson) != null) {
                    modelRelationRepository.save(new ModelRelation(bpmnProcessModel.getId(), idReferencedInJson, relationshipType));
                }
            }
        }
    }

    protected ModelHistory createNewModelhistory(Model model) {
        ModelHistory historyModel = new ModelHistory();
        historyModel.setName(model.getName());
        historyModel.setKey(model.getKey());
        historyModel.setDescription(model.getDescription());
        historyModel.setCreated(model.getCreated());
        historyModel.setLastUpdated(model.getLastUpdated());
        historyModel.setCreatedBy(model.getCreatedBy());
        historyModel.setLastUpdatedBy(model.getLastUpdatedBy());
        historyModel.setModelEditorJson(model.getModelEditorJson());
        historyModel.setModelType(model.getModelType());
        historyModel.setVersion(model.getVersion());
        historyModel.setModelId(model.getId());
        historyModel.setComment(model.getComment());

        return historyModel;
    }

    protected void populateModelBasedOnHistory(Model model, ModelHistory basedOn) {
        model.setName(basedOn.getName());
        model.setKey(basedOn.getKey());
        model.setDescription(basedOn.getDescription());
        model.setCreated(basedOn.getCreated());
        model.setLastUpdated(basedOn.getLastUpdated());
        model.setCreatedBy(basedOn.getCreatedBy());
        model.setLastUpdatedBy(basedOn.getLastUpdatedBy());
        model.setModelEditorJson(basedOn.getModelEditorJson());
        model.setModelType(basedOn.getModelType());
        model.setVersion(basedOn.getVersion());
        model.setComment(basedOn.getComment());
    }

    protected Map<String, String> convertToModelKeyMap(Map<String, Model> modelMap) {
        Map<String, String> modelKeyMap = new HashMap<>();
        if (modelMap != null) {
            for (Model formModel : modelMap.values()) {
                modelKeyMap.put(formModel.getId(), formModel.getKey());
            }
        }

        return modelKeyMap;
    }
}
