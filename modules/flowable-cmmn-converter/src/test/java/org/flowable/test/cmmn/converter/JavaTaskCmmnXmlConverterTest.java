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
package org.flowable.test.cmmn.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ExtensionElement;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class JavaTaskCmmnXmlConverterTest extends AbstractConverterTest {
    
    private static final String CMMN_RESOURCE = "org/flowable/test/cmmn/converter/java-task.cmmn";
    
    @Test
    public void convertXMLToModel() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        validateModel(cmmnModel);
    }

    @Test
    public void convertModelToXML() throws Exception {
        CmmnModel cmmnModel = readXMLFile(CMMN_RESOURCE);
        CmmnModel parsedModel = exportAndReadXMLFile(cmmnModel);
        validateModel(parsedModel);
    }
    
    public void validateModel(CmmnModel cmmnModel) {
        assertNotNull(cmmnModel);
        assertEquals(1, cmmnModel.getCases().size());
        
        // Case
        Case caze = cmmnModel.getCases().get(0);
        assertEquals("javaCase", caze.getId());
        assertEquals("test", caze.getInitiatorVariableName());
        
        // Plan model
        Stage planModel = caze.getPlanModel();
        assertNotNull(planModel);
        assertEquals("myPlanModel", planModel.getId());
        assertEquals("My CasePlanModel", planModel.getName());
        
        // Plan items definitions
        List<PlanItemDefinition> planItemDefinitions = planModel.getPlanItemDefinitions();
        assertEquals(2, planItemDefinitions.size());
        assertEquals(2, planModel.findPlanItemDefinitionsOfType(Task.class, false).size());
        
        // Plan items
        List<PlanItem> planItems = planModel.getPlanItems();
        assertEquals(2, planItems.size());
        
        PlanItem planItemTaskA = cmmnModel.findPlanItem("planItemTaskA");
        PlanItemDefinition planItemDefinition = planItemTaskA.getPlanItemDefinition();
        assertEquals(0, planItemTaskA.getEntryCriteria().size());
        assertTrue(planItemDefinition instanceof ServiceTask);
        ServiceTask taskA = (ServiceTask) planItemDefinition;
        assertEquals(ServiceTask.JAVA_TASK, taskA.getType());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_CLASS, taskA.getImplementationType());
        assertEquals("org.flowable.TestJavaDelegate", taskA.getImplementation());
        assertEquals("result", taskA.getResultVariableName());
        
        PlanItem planItemTaskB = cmmnModel.findPlanItem("planItemTaskB");
        planItemDefinition = planItemTaskB.getPlanItemDefinition();
        assertEquals(1, planItemTaskB.getEntryCriteria().size());
        assertTrue(planItemDefinition instanceof ServiceTask);
        ServiceTask taskB = (ServiceTask) planItemDefinition;
        assertEquals(ServiceTask.JAVA_TASK, taskB.getType());
        assertEquals(ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION, taskB.getImplementationType());
        assertEquals("${testJavaDelegate}", taskB.getImplementation());
        assertNull(taskB.getResultVariableName());
        
        assertEquals(4, taskB.getFieldExtensions().size());
        FieldExtension fieldExtension = taskB.getFieldExtensions().get(0);
        assertEquals("fieldA", fieldExtension.getFieldName());
        assertEquals("test", fieldExtension.getStringValue());
        fieldExtension = taskB.getFieldExtensions().get(1);
        assertEquals("fieldB", fieldExtension.getFieldName());
        assertEquals("test", fieldExtension.getExpression());
        fieldExtension = taskB.getFieldExtensions().get(2);
        assertEquals("fieldC", fieldExtension.getFieldName());
        assertEquals("test", fieldExtension.getStringValue());
        fieldExtension = taskB.getFieldExtensions().get(3);
        assertEquals("fieldD", fieldExtension.getFieldName());
        assertEquals("test", fieldExtension.getExpression());
        
        assertEquals(1, taskB.getExtensionElements().size());
        List<ExtensionElement> extensionElements = taskB.getExtensionElements().get("taskTest");
        assertEquals(1, extensionElements.size());
        ExtensionElement extensionElement = extensionElements.get(0);
        assertEquals("taskTest", extensionElement.getName());
        assertEquals("hello", extensionElement.getElementText());
    }

}
