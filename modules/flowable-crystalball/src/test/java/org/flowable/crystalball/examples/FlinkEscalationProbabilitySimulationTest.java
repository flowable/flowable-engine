package org.flowable.crystalball.examples;

import org.apache.commons.io.FileUtils;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.CsvOutputFormat;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.Path;
import org.flowable.engine.ProcessEngines;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import static org.apache.flink.core.fs.FileSystem.WriteMode.OVERWRITE;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class FlinkEscalationProbabilitySimulationTest {

    public static final String SIMULATION_RUN_MODEL = "org/flowable/crystalball/examples/FlinkEscalationProbability-simulationRun.bpmn20.xml";
    public static final String SIMULATION_ENGINE_CONFIG = "org/flowable/crystalball/examples/EscalationProbabilitySimulationTest-realProcessEngine.cfg.xml";

    protected File tempFile;

    @Before
    public void setUp() throws IOException {
        this.tempFile = File.createTempFile("temp-output", ".tmp");
    }

    @After
    public void tearDowr() {
        this.tempFile.delete();
    }

    @Test
    public void testSimulationRun() throws Exception {
        ProcessEngines.setInitialized(true);

        // set up the execution environment
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();


        // get input data
        DataSet<SimulationExperimentDescriptor> simulationRunToExecute = env.fromElements(new SimulationExperimentDescriptor(
                "1" ,
                SIMULATION_RUN_MODEL,
                SIMULATION_ENGINE_CONFIG
        ));

        simulationRunToExecute.flatMap(
                new FlinkSimulationExecutor()
        ).write(new CsvOutputFormat<Tuple2<String, Integer>>(new Path("flink-output-file")),
                tempFile.getAbsolutePath(),
                OVERWRITE
        );

        env.execute("SimulationRunTest");

        assertThat(FileUtils.readFileToString(tempFile, "UTF-8"), startsWith("Escalated,"));
    }

    @Test
    public void simulationExperiment() throws Exception {
        ProcessEngines.setInitialized(true);

        // set up the execution environment
        final ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

        Collection<SimulationExperimentDescriptor> simulationRunDescriptors = new ArrayList<>(100);
        for (int i = 0; i< 10; i++) {
            simulationRunDescriptors.add(new SimulationExperimentDescriptor(
                    Integer.toString(i),
                    SIMULATION_RUN_MODEL,
                    SIMULATION_ENGINE_CONFIG
            ));
        }
        // get input data
        DataSet<SimulationExperimentDescriptor> simulationRunToExecute = env.fromCollection(simulationRunDescriptors);

        simulationRunToExecute.
                flatMap(
                    new FlinkSimulationExecutor()
                ).
                writeAsText(tempFile.getAbsolutePath(), OVERWRITE);

        env.execute("simulationExperiment");

        assertThat(FileUtils.readFileToString(this.tempFile, "UTF-8"), startsWith("(Escalated,"));
    }
}
