package org.flowable.cmmn.editor.json.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.IOParameter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Valentin Zickner
 */
public abstract class BaseChildTaskCmmnJsonConverter extends BaseCmmnJsonConverter {

    protected List<IOParameter> readIOParameters(JsonNode parametersNode) {
        List<IOParameter> ioParameters = new ArrayList<>();
        for (JsonNode paramNode : parametersNode){
            IOParameter ioParameter = new IOParameter();

            if (paramNode.has("source")) {
                ioParameter.setSource(paramNode.get("source").asText());
            }
            if (paramNode.has("sourceExpression")) {
                ioParameter.setSourceExpression(paramNode.get("sourceExpression").asText());
            }
            if (paramNode.has("target")) {
                ioParameter.setTarget(paramNode.get("target").asText());
            }
            if (paramNode.has("targetExpression")) {
                ioParameter.setTargetExpression(paramNode.get("targetExpression").asText());
            }
            ioParameters.add(ioParameter);
        }
        return ioParameters;
    }

    protected void readIOParameters(List<IOParameter> ioParameters, ArrayNode parametersNode) {
        for (IOParameter ioParameter : ioParameters) {

            ObjectNode parameterNode = parametersNode.addObject();

            if (StringUtils.isNotEmpty(ioParameter.getSource())) {
                parameterNode.put("source", ioParameter.getSource());
            }
            if (StringUtils.isNotEmpty(ioParameter.getSourceExpression())) {
                parameterNode.put("sourceExpression", ioParameter.getSourceExpression());
            }
            if (StringUtils.isNotEmpty(ioParameter.getTarget())) {
                parameterNode.put("target", ioParameter.getTarget());
            }
            if (StringUtils.isNotEmpty(ioParameter.getTargetExpression())) {
                parameterNode.put("targetExpression", ioParameter.getTargetExpression());
            }

        }
    }

}
