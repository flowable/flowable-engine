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
package org.flowable.cmmn.editor.json.converter;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.IOParameter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
