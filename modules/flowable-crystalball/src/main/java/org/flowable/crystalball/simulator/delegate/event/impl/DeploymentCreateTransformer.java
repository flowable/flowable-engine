package org.flowable.crystalball.simulator.delegate.event.impl;

import org.flowable.crystalball.simulator.SimulationEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEntityEvent;
import org.flowable.engine.common.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * @author martin.grofcik
 */
public class DeploymentCreateTransformer extends Activiti2SimulationEventFunction {

  public static final String PROCESS_INSTANCE_ID = "processInstanceId";
  private final String resourcesKey;

  public DeploymentCreateTransformer(String simulationEventType, String resourcesKey) {
    super(simulationEventType);
    this.resourcesKey = resourcesKey;
  }

  @Override
  public SimulationEvent apply(FlowableEvent event) {
    if (FlowableEngineEventType.ENTITY_CREATED.equals(event.getType()) && (event instanceof FlowableEntityEvent) && ((FlowableEntityEvent) event).getEntity() instanceof DeploymentEntity) {

      DeploymentEntity deploymentEntity = (DeploymentEntity) ((FlowableEntityEvent) event).getEntity();

      Map<String, Object> simEventProperties = new HashMap<String, Object>();
      simEventProperties.put(resourcesKey, deploymentEntity.getResources());

      return new SimulationEvent.Builder(simulationEventType).simulationTime(Context.getProcessEngineConfiguration().getClock().getCurrentTime().getTime()).properties(simEventProperties).priority(1).build();
    }
    return null;
  }
}
