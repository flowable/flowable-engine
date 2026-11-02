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
package org.activiti.engine.test.api.repository;

import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.bpmn.model.Lane;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.junit.Assert;

/**
 * Created by P3700487 on 2/19/2015.
 */
public class LaneExtensionTest extends PluggableFlowableTestCase {

    @Deployment
    public void testLaneExtensionElement() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("swimlane-extension").singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        byte[] xml = new BpmnXMLConverter().convertToXML(bpmnModel);
        System.out.println(new String(xml));
        Process bpmnProcess = bpmnModel.getMainProcess();
        for (Lane l : bpmnProcess.getLanes()) {
            Map<String, List<ExtensionElement>> extensions = l.getExtensionElements();
            Assert.assertTrue(extensions.size() > 0);
        }
    }

}
