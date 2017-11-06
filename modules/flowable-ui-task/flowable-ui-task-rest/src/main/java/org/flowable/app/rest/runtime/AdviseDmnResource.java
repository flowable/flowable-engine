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
package org.flowable.app.rest.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.LiteralExpression;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.model.RuleInputClauseContainer;
import org.flowable.dmn.model.RuleOutputClauseContainer;
import org.flowable.dmn.model.UnaryTests;
import org.flowable.dmn.xml.converter.DmnXMLConverter;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class AdviseDmnResource {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AdviseDmnResource.class);
    
    @Autowired
    protected RepositoryService repositoryService;
    
    @Autowired
    protected DmnRepositoryService dmnRepositoryService;
    
    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected Environment environment;
    
    protected DmnXMLConverter dmnXMLConverter = new DmnXMLConverter();

    @RequestMapping(value = "/rest/advisedmn", method = RequestMethod.GET, produces = "application/json")
    public void adviseForTask(@RequestParam(name="processDefinitionKey") String processDefinitionKey, @RequestParam(name="definitionKey") String definitionKey) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey).latestVersion().singleResult();
        JsonNode adviseDataNode = doAdviseDataCall(processDefinition.getId(), definitionKey);
        
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            InputStreamReader in = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("devoxx/advise.dmn"), "UTF-8");
            XMLStreamReader xtr = xif.createXMLStreamReader(in);
            DmnDefinition dmnDefinition = dmnXMLConverter.convertToDmnModel(xtr);
            List<Decision> decisions = dmnDefinition.getDecisions();
            DecisionTable decisionTable = (DecisionTable) decisions.get(0).getExpression();
            decisionTable.getRules().clear();
            
            int ruleNumberCounter = 1;
            
            for (JsonNode adviseObjectNode : adviseDataNode) {
                
                String ageValue = null;
                String ageValue2 = null;
                List<String> nationalityList = new ArrayList<>();
                String nationalityOperator = null;
                String adviseValue = adviseObjectNode.get("outcome").asText();
                
                JsonNode conditionsNode = adviseObjectNode.get("conditions");
                for (JsonNode conditionObjectNode : conditionsNode) {
                    if ("age".equalsIgnoreCase(conditionObjectNode.get("variableName").asText())) {
                        int rightValue = conditionObjectNode.get("rightValue").asInt();
                        String ageOperator = null;
                        if ("LTE".equalsIgnoreCase(conditionObjectNode.get("operation").asText())) {
                            ageOperator = "<=";
                            
                        } else if ("LT".equalsIgnoreCase(conditionObjectNode.get("operation").asText())) {
                            ageOperator = "<" ;
                        
                        } else if ("GTE".equalsIgnoreCase(conditionObjectNode.get("operation").asText())) {
                            ageOperator = ">=";
                        
                        } else if ("GT".equalsIgnoreCase(conditionObjectNode.get("operation").asText())) {
                            ageOperator = ">";
                        }
                        
                        if (ageValue != null) {
                            ageValue2 = ageOperator + " " + rightValue;
                        } else {
                            ageValue = ageOperator + " " + rightValue;
                        }
                    
                    } else if ("nationality".equalsIgnoreCase(conditionObjectNode.get("variableName").asText())) {
                        if (nationalityOperator != null) {
                            if ("NOT_IN".equalsIgnoreCase(conditionObjectNode.get("operation").asText())) {
                                continue;
                            
                            } else {
                                nationalityList.clear();
                            }
                        }
                        
                        JsonNode valueArrayNode = conditionObjectNode.get("rightValue");
                        for (JsonNode valueNode : valueArrayNode) {
                            nationalityList.add(valueNode.asText());
                        }
                        
                        nationalityOperator = conditionObjectNode.get("operation").asText();
                    }
                }
                
                if ("NOT_IN".equalsIgnoreCase(nationalityOperator)) {
                    DecisionRule newRule = new DecisionRule();
                    newRule.setRuleNumber(ruleNumberCounter);
                    
                    int inputNumberCounter = 1;
                    newRule.addInputEntry(createInputEntry("age", ageValue, ruleNumberCounter, inputNumberCounter++));
                    if (ageValue2 != null) {
                        newRule.addInputEntry(createInputEntry("age", ageValue2, ruleNumberCounter, inputNumberCounter++));
                    } else {
                        newRule.addInputEntry(createInputEntry("age", "-", ruleNumberCounter, inputNumberCounter++));
                    }
                    
                    for (String nationality : nationalityList) {
                        newRule.addInputEntry(createInputEntry("nationality", "!= '" + nationality + "'", ruleNumberCounter, inputNumberCounter++));
                    }
                    
                    for (int i = nationalityList.size(); i < 4; i++) {
                        newRule.addInputEntry(createInputEntry("nationality", "-", ruleNumberCounter, inputNumberCounter++));
                    }
                    
                    newRule.addOutputEntry(createOutputEntry(adviseValue, ruleNumberCounter));
                    decisionTable.getRules().add(newRule);
                    ruleNumberCounter++;
                    
                } else {
                    
                    if (nationalityList.size() > 0) {
                        for (String nationality : nationalityList) {
                            DecisionRule newRule = new DecisionRule();
                            newRule.setRuleNumber(ruleNumberCounter);
                            
                            newRule.addInputEntry(createInputEntry("age", ageValue, ruleNumberCounter, 1));
                            if (ageValue2 != null) {
                                newRule.addInputEntry(createInputEntry("age", ageValue2, ruleNumberCounter, 2));
                            } else {
                                newRule.addInputEntry(createInputEntry("age", "-", ruleNumberCounter, 2));
                            }
                            newRule.addInputEntry(createInputEntry("nationality", "== '" + nationality + "'", ruleNumberCounter, 3));
                            
                            for (int i = 1; i < 4; i++) {
                                newRule.addInputEntry(createInputEntry("nationality", "-", ruleNumberCounter, i + 3));
                            }
                            
                            newRule.addOutputEntry(createOutputEntry(adviseValue, ruleNumberCounter));
                            decisionTable.getRules().add(newRule);
                            ruleNumberCounter++;
                        }
                        
                    } else {
                        DecisionRule newRule = new DecisionRule();
                        newRule.setRuleNumber(ruleNumberCounter);
                        
                        newRule.addInputEntry(createInputEntry("age", ageValue, ruleNumberCounter, 1));
                        if (ageValue2 != null) {
                            newRule.addInputEntry(createInputEntry("age", ageValue2, ruleNumberCounter, 2));
                        } else {
                            newRule.addInputEntry(createInputEntry("-", ageValue2, ruleNumberCounter, 2));
                        }
                        
                        for (int i = 1; i < 5; i++) {
                            newRule.addInputEntry(createInputEntry("nationality", "-", ruleNumberCounter, i + 2));
                        }
                        
                        newRule.addOutputEntry(createOutputEntry(adviseValue, ruleNumberCounter));
                        decisionTable.getRules().add(newRule);
                        ruleNumberCounter++;
                    }
                }
            }

            byte[] xmlBytes = dmnXMLConverter.convertToXML(dmnDefinition);
            
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            FlowElement flowElement = bpmnModel.getFlowElement(definitionKey);
            
            ServiceTask newDmnServiceTask = new ServiceTask();
            newDmnServiceTask.setId(flowElement.getId());
            newDmnServiceTask.setType(ServiceTask.DMN_TASK);
            FieldExtension decisionTableKeyField = new FieldExtension();
            decisionTableKeyField.setFieldName("decisionTableReferenceKey");
            decisionTableKeyField.setStringValue(decisions.get(0).getId());
            newDmnServiceTask.getFieldExtensions().add(decisionTableKeyField);
            
            bpmnModel.getMainProcess().removeFlowElement(flowElement.getId());
            bpmnModel.getMainProcess().addFlowElement(newDmnServiceTask);
            
            Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(processDefinition.getDeploymentId()).singleResult();
            
            DeploymentBuilder newDeploymentBuilder = repositoryService.createDeployment().addBpmnModel("devoxx.bpmn", bpmnModel).name(deployment.getName()).key(deployment.getKey());
            
            List<String> resourceNames = repositoryService.getDeploymentResourceNames(deployment.getId());
            for (String resourceName : resourceNames) {
                if (resourceName.endsWith(".app") || resourceName.endsWith(".form")) {
                    InputStream resourceStream = repositoryService.getResourceAsStream(deployment.getId(), resourceName);
                    newDeploymentBuilder.addInputStream(resourceName, resourceStream);
                }
            }
            
            newDeploymentBuilder.addBytes("advise.dmn", xmlBytes);
            newDeploymentBuilder.deploy();
            
            importDmnModel(xmlBytes);
            
        } catch (Exception e) {
            LOGGER.error("Error creating the DMN model", e);
            throw new InternalServerErrorException("Error creating the DMN model: " + e.getMessage());
        }
    }
    
    protected RuleInputClauseContainer createInputEntry(String inputName, String inputValue, int ruleNumberCounter, int inputNumberCounter) {
        RuleInputClauseContainer ruleInputClause = new RuleInputClauseContainer();
        InputClause inputClause = new InputClause();
        inputClause.setInputNumber(inputNumberCounter);
        LiteralExpression inputExpression = new LiteralExpression();
        inputExpression.setId("inputExpression_" + ruleNumberCounter + "_" + inputNumberCounter);
        inputExpression.setText(inputName);
        inputClause.setInputExpression(inputExpression);
        ruleInputClause.setInputClause(inputClause);
        UnaryTests textEntry = new UnaryTests();
        textEntry.setId("inputEntry_" + ruleNumberCounter + "_" + inputNumberCounter);
        textEntry.setText(inputValue);
        ruleInputClause.setInputEntry(textEntry);
        return ruleInputClause;
    }
    
    protected RuleOutputClauseContainer createOutputEntry(String outputValue, int ruleNumberCounter) {
        RuleOutputClauseContainer ruleOutputClause = new RuleOutputClauseContainer();
        OutputClause outputClause = new OutputClause();
        outputClause.setId("outputExpression_" +  ruleNumberCounter + "_1");
        outputClause.setOutputNumber(ruleNumberCounter);
        ruleOutputClause.setOutputClause(outputClause);
        LiteralExpression outputEntry = new LiteralExpression();
        outputEntry.setText("\"" + outputValue + "\"");
        outputEntry.setId("outputEntry_" + ruleNumberCounter + "_1");
        ruleOutputClause.setOutputEntry(outputEntry);
        return ruleOutputClause;
    }

    protected JsonNode doAdviseDataCall(String processDefinitionId, String definitionKey) {
        String adviseUrl = environment.getRequiredProperty("advisedata.rest.url");

        HttpGet httpGet = new HttpGet(adviseUrl + "?processDefinitionId=" + processDefinitionId + "&definitionKey=" + definitionKey);

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        CloseableHttpClient client = clientBuilder.build();

        try {
            HttpResponse response = client.execute(httpGet);
            InputStream responseContent = response.getEntity().getContent();
            String strResponse = IOUtils.toString(responseContent, "utf-8");
            return objectMapper.readTree(strResponse);
            
        } catch (Exception e) {
            LOGGER.error("Error calling advise service endpoint", e);
            throw new InternalServerErrorException("Error calling advise service endpoint: " + e.getMessage());
            
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    LOGGER.warn("Exception while closing http client", e);
                }
            }
        }
    }
    
    protected void importDmnModel(byte[] xmlBytes) {
        String modelerUrl = environment.getRequiredProperty("import.dmn.url");

        HttpPost httpPost = new HttpPost(modelerUrl);
        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        entityBuilder.addPart("file", new ByteArrayBody(xmlBytes, "advise.dmn"));
        httpPost.setEntity(entityBuilder.build());

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        CloseableHttpClient client = clientBuilder.build();

        try {
            client.execute(httpPost);
            
        } catch (Exception e) {
            LOGGER.error("Error calling advise service endpoint", e);
            throw new InternalServerErrorException("Error calling advise service endpoint: " + e.getMessage());
            
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    LOGGER.warn("Exception while closing http client", e);
                }
            }
        }
    }
}
