package org.flowable.engine.test.regression;

import java.util.List;

import org.activiti.validation.ProcessValidator;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * From http://forums.activiti.org/content/skip-parse-validation-while-fetching- startformdata
 * 
 * Test for validating that the process validator ONLY kicks in on deployment, not on reading again from database. The two tests should fail, cause the validator kicks in the second time, but not
 * originally (don't do this at home, kids. Disabling the validator on deploy is BAD).
 * 
 */
public class ProcessValidationExecutedAfterDeployTest extends PluggableFlowableTestCase {

  protected ProcessValidator processValidator;

  private void disableValidation() {
    processValidator = processEngineConfiguration.getProcessValidator();
    processEngineConfiguration.setProcessValidator(null);
  }

  private void enableValidation() {
    processEngineConfiguration.setProcessValidator(processValidator);
  }

  private void clearDeploymentCache() {
    processEngineConfiguration.getProcessDefinitionCache().clear();
  }

  protected void tearDown() throws Exception {
    enableValidation();
    super.tearDown();
  }

  private ProcessDefinition getLatestProcessDefinitionVersionByKey(String processDefinitionKey) {
    List<ProcessDefinition> definitions = null;
    try {
      definitions = repositoryService.createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey).orderByProcessDefinitionVersion().latestVersion().desc().list();
      if (definitions.isEmpty()) {
        return null;
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
    return definitions.get(0);
  }

  public void testGetLatestProcessDefinitionTextByKey() {

    disableValidation();
    repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/regression/ProcessValidationExecutedAfterDeployTest.bpmn20.xml").deploy();
    enableValidation();
    clearDeploymentCache();

    ProcessDefinition definition = getLatestProcessDefinitionVersionByKey("testProcess1");
    if (definition == null) {
      fail("Error occurred in fetching process model.");
    }
    try {
      repositoryService.getProcessModel(definition.getId());
      assertTrue(true);
    } catch (FlowableException e) {
      fail("Error occurred in fetching process model.");
    }

    for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId());
    }
  }

  public void testGetStartFormData() {

    disableValidation();
    repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/regression/ProcessValidationExecutedAfterDeployTest.bpmn20.xml").deploy();
    enableValidation();
    clearDeploymentCache();

    ProcessDefinition definition = getLatestProcessDefinitionVersionByKey("testProcess1");
    if (definition == null) {
      fail("Error occurred in fetching process model.");
    }
    try {
      formService.getStartFormData(definition.getId());
      assertTrue(true);
    } catch (FlowableException e) {
      fail("Error occurred in fetching start form data:");
    }

    for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
      repositoryService.deleteDeployment(deployment.getId());
    }
  }
}
