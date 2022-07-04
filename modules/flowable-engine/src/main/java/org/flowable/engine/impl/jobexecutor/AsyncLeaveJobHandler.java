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
package org.flowable.engine.impl.jobexecutor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 
 * @author Joram Barrez
 */
public class AsyncLeaveJobHandler implements JobHandler {

    public static final Logger LOGGER = LoggerFactory.getLogger(AsyncLeaveJobHandler.class);

    public static final String TYPE = "async-after";

    private static final String FIELD_EVALUATE_CONDITIONS = "evaluateConditions";
    private static final String FIELD_SEQUENCE_FLOW_ID = "sequenceFlowId";
    private static final String FIELD_SEQUENCE_FLOW_SOURCE = "source";
    private static final String FIELD_SEQUENCE_FLOW_TARGET = "target";
    private static final String FIELD_SEQUENCE_FLOW_LINE_NR = "lineNr";
    private static final String FIELD_SEQUENCE_FLOW_LINE_COLUMN_NR = "colNr";


    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ExecutionEntity executionEntity = (ExecutionEntity) variableScope;

        ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper();

        if (configuration != null) {
            try {
                JsonNode jobConfigurationJson = objectMapper.readTree(configuration);

                boolean evaluateConditions = false;
                JsonNode evaluateConditionsNode = jobConfigurationJson.path(FIELD_EVALUATE_CONDITIONS);
                if (evaluateConditionsNode != null && evaluateConditionsNode.isBoolean()) {
                    evaluateConditions = evaluateConditionsNode.booleanValue();
                }

                if (isAsyncLeaveWithSpecificSequenceFlow(jobConfigurationJson)) { // see createJobConfiguration method below why sequence flow are treated differently
                    executionEntity.setCurrentFlowElement(determineSequenceFlow(job, executionEntity, jobConfigurationJson));
                }

                CommandContextUtil.getAgenda(commandContext).planTakeOutgoingSequenceFlowsSynchronousOperation(executionEntity, evaluateConditions);

            } catch (JsonProcessingException e) {
                LOGGER.warn("Programmatic error: could not parse job configuration JSON", e);
            }
        }

    }

    protected boolean isAsyncLeaveWithSpecificSequenceFlow( JsonNode jobConfigurationJson ) {
        return jobConfigurationJson.size() > 1; // a leave through a flow node has only one entry in the JSON. The leave through a sequence flow has more.
    }

    protected SequenceFlow determineSequenceFlow(JobEntity job, ExecutionEntity executionEntity, JsonNode jobConfigurationJson) {

        // See below how the configuration JSON is structured: either it's
        // - a flow node leave -> no more information in json
        // - a leave through a specific sequence flow (e.g. exclusive gw), in that case the id of the sequence flow can be given
        //   or sequence flow info to look it up, if the id is missing.

        String sequenceFlowId = null;
        JsonNode sequenceFlowIdJsonNode = jobConfigurationJson.path(FIELD_SEQUENCE_FLOW_ID);
        if (sequenceFlowIdJsonNode != null && !sequenceFlowIdJsonNode.isNull() && !sequenceFlowIdJsonNode.isMissingNode()) {
            sequenceFlowId = sequenceFlowIdJsonNode.asText();
        }

        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(executionEntity.getProcessDefinitionId());

        SequenceFlow sequenceFlow = null;
        if (StringUtils.isNotEmpty(sequenceFlowId)) {
            FlowElement flowElement = bpmnModel.getMainProcess().getFlowElement(sequenceFlowId);
            if (flowElement instanceof SequenceFlow) {
                sequenceFlow = (SequenceFlow) flowElement;
            }
        }

        if (sequenceFlow == null) {
            String source = jobConfigurationJson.path(FIELD_SEQUENCE_FLOW_SOURCE).asText();
            String target = jobConfigurationJson.path(FIELD_SEQUENCE_FLOW_TARGET).asText();
            int lineNr = jobConfigurationJson.path(FIELD_SEQUENCE_FLOW_LINE_NR).asInt();
            int columnNr = jobConfigurationJson.path(FIELD_SEQUENCE_FLOW_LINE_COLUMN_NR).asInt();

            List<SequenceFlow> sequenceFlows = bpmnModel.getMainProcess().findFlowElementsOfType(SequenceFlow.class, true);
            Optional<SequenceFlow> sequenceFlowOptional = sequenceFlows.stream()
                    .filter(s ->
                            Objects.equals(source, s.getSourceRef())
                                    && Objects.equals(target, s.getTargetRef())
                                    && lineNr == s.getXmlRowNumber()
                                    && columnNr == s.getXmlColumnNumber())
                    .findFirst();

            if (sequenceFlowOptional.isPresent()) {
                sequenceFlow = sequenceFlowOptional.get();
            }

        }

        if (sequenceFlow == null) {
            throw new FlowableException("Programmatic error: no sequence flow could be found for async leave in job " + job.getId());
        }

        return sequenceFlow;
    }

    public static String createJobConfiguration(ProcessEngineConfiguration processEngineConfiguration, boolean evaluateConditions) {
        ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put(FIELD_EVALUATE_CONDITIONS, evaluateConditions);
        try {
            return objectMapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Programmatic error: could not create job configuration JSON", e);
        }
        return null;
    }

    public static String createJobConfiguration(ProcessEngineConfiguration processEngineConfiguration, SequenceFlow sequenceFlow) {
        ObjectMapper objectMapper = processEngineConfiguration.getObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put(FIELD_EVALUATE_CONDITIONS, false); // If the sequenceflow is passed, no need to evaluate conditions

        // The execution entity does not persistently store when it's currently at a sequence flow (it only has an activityId).
        // As such, the information of the current sequence flow needs to be persisted in the job configuration, contrary to the
        // the other createJobConfiguration above where the execution entity's activityId will be enough and no extra information is needed.

        String sequenceFlowId = sequenceFlow.getId();
        if (StringUtils.isNotEmpty(sequenceFlowId)) {
            objectNode.put(FIELD_SEQUENCE_FLOW_ID, sequenceFlowId);

        } else {
            // Sequence flow don't have a requied id.
            // To be able to find it in the job, the source/target/xml location is stored.
            objectNode.put(FIELD_SEQUENCE_FLOW_SOURCE, sequenceFlow.getSourceRef());
            objectNode.put(FIELD_SEQUENCE_FLOW_TARGET, sequenceFlow.getTargetRef());
            objectNode.put(FIELD_SEQUENCE_FLOW_LINE_NR, sequenceFlow.getXmlRowNumber());
            objectNode.put(FIELD_SEQUENCE_FLOW_LINE_COLUMN_NR, sequenceFlow.getXmlColumnNumber());

        }

        try {
            return objectMapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Programmatic error: could not create job configuration JSON", e);
        }
        return null;
    }

}
