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

package org.flowable.engine.test.bpmn.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.bpmn.deployer.ParsedDeployment;
import org.flowable.engine.impl.bpmn.deployer.ParsedDeploymentBuilder;
import org.flowable.engine.impl.bpmn.deployer.ParsedDeploymentBuilderFactory;
import org.flowable.engine.impl.bpmn.deployer.ResourceNameUtil;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntityImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParsedDeploymentTest extends PluggableFlowableTestCase {

    private static final String NAMESPACE = "xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL'";
    private static final String TARGET_NAMESPACE = "targetNamespace='http://activiti.org/BPMN20'";

    private static final String ID1_ID = "id1";
    private static final String ID2_ID = "id2";
    private static final String IDR_PROCESS_XML = assembleXmlResourceString(
            "<process id='" + ID1_ID + "' name='Insurance Damage Report 1' />",
            "<process id='" + ID2_ID + "' name='Insurance Damager Report 2' />");
    private static final String IDR_XML_NAME = "idr." + ResourceNameUtil.BPMN_RESOURCE_SUFFIXES[0];

    private static final String EN1_ID = "en1";
    private static final String EN2_ID = "en2";
    private static final String EN_PROCESS_XML = assembleXmlResourceString(
            "<process id='" + EN1_ID + "' name='Expense Note 1' />",
            "<process id='" + EN2_ID + "' name='Expense Note 2' />");
    private static final String EN_XML_NAME = "en." + ResourceNameUtil.BPMN_RESOURCE_SUFFIXES[1];

    @BeforeEach
    public void setUp() {
        CommandContext commandContext = processEngineConfiguration.getCommandContextFactory().createCommandContext(null);
        if (commandContext.getEngineConfigurations() == null) {
            commandContext.setEngineConfigurations(new HashMap<>());
        }
        commandContext.getEngineConfigurations().put(EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG, processEngineConfiguration);
        Context.setCommandContext(commandContext);
    }

    @AfterEach
    public void tearDown() {
        Context.removeCommandContext();
    }

    @Test
    public void testCreateAndQuery() throws UnsupportedEncodingException {
        DeploymentEntity entity = assembleUnpersistedDeploymentEntity();

        ParsedDeploymentBuilderFactory builderFactory = processEngineConfiguration.getParsedDeploymentBuilderFactory();
        ParsedDeploymentBuilder builder = builderFactory.getBuilderForDeployment(entity);
        ParsedDeployment parsedDeployment = builder.build();

        List<ProcessDefinitionEntity> processDefinitions = parsedDeployment.getAllProcessDefinitions();

        assertThat(parsedDeployment.getDeployment()).isSameAs(entity);
        assertThat(processDefinitions).hasSize(4);

        ProcessDefinitionEntity id1 = getProcessDefinitionEntityFromList(processDefinitions, ID1_ID);
        ProcessDefinitionEntity id2 = getProcessDefinitionEntityFromList(processDefinitions, ID2_ID);
        assertThat(parsedDeployment.getBpmnParseForProcessDefinition(id1)).isSameAs(parsedDeployment.getBpmnParseForProcessDefinition(id2));
        assertThat(parsedDeployment.getBpmnModelForProcessDefinition(id1)).isSameAs(parsedDeployment.getBpmnParseForProcessDefinition(id1).getBpmnModel());
        assertThat(parsedDeployment.getProcessModelForProcessDefinition(id1))
                .isSameAs(parsedDeployment.getBpmnParseForProcessDefinition(id1).getBpmnModel().getProcessById(id1.getKey()));
        assertThat(parsedDeployment.getResourceForProcessDefinition(id1).getName()).isEqualTo(IDR_XML_NAME);
        assertThat(parsedDeployment.getResourceForProcessDefinition(id2).getName()).isEqualTo(IDR_XML_NAME);

        ProcessDefinitionEntity en1 = getProcessDefinitionEntityFromList(processDefinitions, EN1_ID);
        ProcessDefinitionEntity en2 = getProcessDefinitionEntityFromList(processDefinitions, EN2_ID);
        assertThat(parsedDeployment.getBpmnParseForProcessDefinition(en1))
                .isSameAs(parsedDeployment.getBpmnParseForProcessDefinition(en2))
                .isNotEqualTo(parsedDeployment.getBpmnParseForProcessDefinition(id2));
        assertThat(parsedDeployment.getResourceForProcessDefinition(en1).getName()).isEqualTo(EN_XML_NAME);
        assertThat(parsedDeployment.getResourceForProcessDefinition(en2).getName()).isEqualTo(EN_XML_NAME);
    }

    private ProcessDefinitionEntity getProcessDefinitionEntityFromList(List<ProcessDefinitionEntity> list, String idString) {
        for (ProcessDefinitionEntity possible : list) {
            if (possible.getKey().equals(idString)) {
                return possible;
            }
        }
        return null;
    }

    private DeploymentEntity assembleUnpersistedDeploymentEntity() throws UnsupportedEncodingException {
        DeploymentEntity entity = new DeploymentEntityImpl();
        entity.addResource(buildResource(IDR_XML_NAME, IDR_PROCESS_XML));
        entity.addResource(buildResource(EN_XML_NAME, EN_PROCESS_XML));
        return entity;
    }

    private ResourceEntity buildResource(String name, String text) throws UnsupportedEncodingException {
        ResourceEntityImpl result = new ResourceEntityImpl();
        result.setName(name);
        result.setBytes(text.getBytes(StandardCharsets.UTF_8));

        return result;
    }

    private static String assembleXmlResourceString(String... definitions) {
        StringBuilder builder = new StringBuilder("<definitions ");
        builder = builder.append(NAMESPACE).append(" ").append(TARGET_NAMESPACE).append(">\n");

        for (String definition : definitions) {
            builder = builder.append(definition).append("\n");
        }

        builder = builder.append("</definitions>\n");

        return builder.toString();
    }
}
