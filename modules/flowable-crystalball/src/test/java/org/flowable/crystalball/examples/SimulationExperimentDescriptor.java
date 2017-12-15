package org.flowable.crystalball.examples;

/**
 * This describes simulation run
 *
 * @author martin.grofcik
 */
public class SimulationExperimentDescriptor {

    protected String id;
    protected String simulationRunModelResource;
    protected String simulationEngineConfiguration;

    public SimulationExperimentDescriptor(String id, String simulationRunModelResource, String simulationEngineConfiguration) {
        this.simulationRunModelResource = simulationRunModelResource;
        this.simulationEngineConfiguration = simulationEngineConfiguration;
        this.id = id;
    }

    public String getSimulationRunModelResource() {
        return simulationRunModelResource;
    }

    public String getSimulationEngineConfiguration() {
        return simulationEngineConfiguration;
    }

    public String getId() {
        return id;
    }

}
