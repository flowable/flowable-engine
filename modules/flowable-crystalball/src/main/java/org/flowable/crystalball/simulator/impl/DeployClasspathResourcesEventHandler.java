package org.flowable.crystalball.simulator.impl;

import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.crystalball.simulator.SimulationEventHandler;
import org.flowable.crystalball.simulator.SimulationRunContext;
import org.flowable.engine.repository.DeploymentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This class deploys resources from classpath
 */
public class DeployClasspathResourcesEventHandler implements SimulationEventHandler {

  private static Logger log = LoggerFactory.getLogger(DeployClasspathResourcesEventHandler.class);

  /**
   * process to start key
   */
  protected String resourcesKey;

  public DeployClasspathResourcesEventHandler(String resourcesKey) {
    this.resourcesKey = resourcesKey;
  }

  @Override
  public void init() {
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handle(SimulationEvent event) {

    List<String> resources = (List<String>) event.getProperty(resourcesKey);

    DeploymentBuilder deploymentBuilder = SimulationRunContext.getRepositoryService().createDeployment();

    for (String resource : resources) {
      log.debug("adding resource [{}] to repository", resource, SimulationRunContext.getRepositoryService());
      deploymentBuilder.addClasspathResource(resource);
    }

    deploymentBuilder.deploy();
  }

}
