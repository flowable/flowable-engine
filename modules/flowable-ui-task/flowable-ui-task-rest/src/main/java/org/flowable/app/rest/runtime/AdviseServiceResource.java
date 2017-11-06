/* Licensed under the Apache License, Version 2.0 (the \"License\");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.app.rest.runtime;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.javassist.NotFoundException;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class AdviseServiceResource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdviseServiceResource.class);

    @Autowired
    protected ObjectMapper objectMapper;
    
    
    @RequestMapping(value = "/rest/adviseservice", method = RequestMethod.GET)
    public String provideAdvise(@RequestParam(name="age") int age, @RequestParam(name="nationality") String nationality) {
        LOGGER.info("received age " + age + ", nationality " + nationality);
        try {
            JsonNode adviseCollectionNode = getAdviseNode();
            for (JsonNode adviseNode : adviseCollectionNode) {
                
                boolean conditionsMatch = true;
                JsonNode conditionsNode = adviseNode.get("conditions");
                for (JsonNode conditionObjectNode : conditionsNode) {
                    if ("age".equalsIgnoreCase(conditionObjectNode.get("variableName").asText())) {
                        if ("LTE".equalsIgnoreCase(conditionObjectNode.get("operation").asText())) {
                            if (age > conditionObjectNode.get("rightValue").asDouble()) {
                                conditionsMatch = false;
                            }
                            
                        } else if ("LT".equalsIgnoreCase(conditionObjectNode.get("operation").asText())) {
                            if (age >= conditionObjectNode.get("rightValue").asDouble()) {
                                conditionsMatch = false;
                            }
                        
                        } else if ("GTE".equalsIgnoreCase(conditionObjectNode.get("operation").asText())) {
                            if (age < conditionObjectNode.get("rightValue").asDouble()) {
                                conditionsMatch = false;
                            }
                        
                        } else if ("GT".equalsIgnoreCase(conditionObjectNode.get("operation").asText())) {
                            if (age <= conditionObjectNode.get("rightValue").asDouble()) {
                                conditionsMatch = false;
                            }
                        }
                    
                    } else if ("nationality".equalsIgnoreCase(conditionObjectNode.get("variableName").asText())) {
                        List<String> valueList = new ArrayList<>();
                        JsonNode valueArrayNode = conditionObjectNode.get("rightValue");
                        for (JsonNode valueNode : valueArrayNode) {
                            valueList.add(valueNode.asText());
                        }
                        
                        if ("IN".equalsIgnoreCase(conditionObjectNode.get("operation").asText())) {
                            if (!valueList.contains(nationality)) {
                                conditionsMatch = false;
                            }
                            
                        } else if ("NOT_IN".equalsIgnoreCase(conditionObjectNode.get("operation").asText())) {
                            if (valueList.contains(nationality)) {
                                conditionsMatch = false;
                            }
                        }
                    }
                }
                
                if (conditionsMatch) {
                    LOGGER.info("advise node " + adviseNode);
                    LOGGER.info("advise outcome " + adviseNode.get("outcome").asText());
                    return adviseNode.get("outcome").asText();
                }
            }
            
            throw new NotFoundException("No matching conditions");
            
        } catch (Exception e) {
            LOGGER.error("Error getting advise", e);
            throw new InternalServerErrorException("Error getting advise", e);
        }
    }
    
    @RequestMapping(value = "/rest/advisedata", method = RequestMethod.GET, produces = "application/json")
    public JsonNode getAdviseData(@RequestParam(name="processDefinitionId") String processDefinitionId, @RequestParam(name="definitionKey") String definitionKey) {
        try {
            return getAdviseNode();
            
        } catch (Exception e) {
            LOGGER.error("Error getting advise data", e);
            throw new InternalServerErrorException("Error getting advise data", e);
        }
    }

    protected JsonNode getAdviseNode() throws Exception {
        return objectMapper.readTree("[{\"conditions\":[{\"variableName\":\"age\",\"operation\":\"LTE\",\"rightValue\":29.0}," + 
                        "{\"variableName\":\"nationality\",\"operation\":\"IN\",\"rightValue\":[\"German\",\"Dutch\"]}]" +
                        ",\"outcome\":\"Reject\",\"probability\":1.0},{\"conditions\":[{\"variableName\":\"age\",\"operation\":\"LTE\",\"rightValue\":29.0}," + 
                        "{\"variableName\":\"nationality\",\"operation\":\"IN\",\"rightValue\":[\"German\",\"Dutch\",\"Belgian\"]}," + 
                        "{\"variableName\":\"nationality\",\"operation\":\"NOT_IN\",\"rightValue\":[\"German\",\"Dutch\"]}]," + 
                        "\"outcome\":\"Accept\",\"probability\":1.0},{\"conditions\":[{\"variableName\":\"age\",\"operation\":\"LTE\",\"rightValue\":29.0}," +
                        "{\"variableName\":\"nationality\",\"operation\":\"NOT_IN\",\"rightValue\":[\"German\",\"Dutch\",\"Belgian\"]}]," + 
                        "\"outcome\":\"Reject\",\"probability\":1.0},{\"conditions\":[{\"variableName\":\"age\",\"operation\":\"GT\",\"rightValue\":29.0}," + 
                        "{\"variableName\":\"age\",\"operation\":\"LTE\",\"rightValue\":32.0},{\"variableName\":\"home\",\"operation\":\"IN\",\"rightValue\":[\"Owned\"]}],\"outcome\":\"Accept\",\"probability\":1.0}," +
                        "{\"conditions\":[{\"variableName\":\"age\",\"operation\":\"GT\",\"rightValue\":29.0}," +
                        "{\"variableName\":\"age\",\"operation\":\"LTE\",\"rightValue\":32.0},{\"variableName\":\"home\",\"operation\":\"NOT_IN\",\"rightValue\":[\"Owned\"]}],\"outcome\":\"Reject\",\"probability\":1.0}," +
                        "{\"conditions\":[{\"variableName\":\"age\",\"operation\":\"GT\",\"rightValue\":29.0}," + 
                        "{\"variableName\":\"age\",\"operation\":\"GT\",\"rightValue\":32.0}],\"outcome\":\"Accept\",\"probability\":1.0}]");
    }
}
