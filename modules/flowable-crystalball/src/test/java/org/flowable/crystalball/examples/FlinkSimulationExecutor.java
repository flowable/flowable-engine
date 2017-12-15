package org.flowable.crystalball.examples;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.util.Collector;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Executes simulation run as {@link FlatMapFunction}
 *
 * @author martin.grofcik
 */
class FlinkSimulationExecutor implements FlatMapFunction<SimulationExperimentDescriptor, Tuple2<String, Integer>> {

    @Override
    public void flatMap(SimulationExperimentDescriptor simulationRunDescriptor, Collector<Tuple2<String, Integer>> collector) throws Exception {

        ProcessEngineConfiguration processEngineConfiguration;
        if (StringUtils.isEmpty(simulationRunDescriptor.simulationEngineConfiguration)) {
            processEngineConfiguration = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration();
        } else {
            processEngineConfiguration = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource(simulationRunDescriptor.getSimulationEngineConfiguration());
        }
        Map<Object, Object> additionalBeans = new HashMap<>();
        additionalBeans.put("collector", collector);
        processEngineConfiguration.setBeans(new MergedMap(additionalBeans, processEngineConfiguration.getBeans()) );
        ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();

        Deployment deployment = processEngine.getRepositoryService().createDeployment().addClasspathResource(
                simulationRunDescriptor.getSimulationRunModelResource()).deploy();

        ProcessDefinition simulationRunDefinition = processEngine.getRepositoryService().createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();


        processEngine.getRuntimeService().createProcessInstanceBuilder().
                processDefinitionId(simulationRunDefinition.getId()).
                start();
        JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngine.getProcessEngineConfiguration(), processEngine.getManagementService(), 30000, 500);
    }

    protected class MergedMap implements Map<Object, Object> {

        protected Map<Object, Object> additionalBeans;
        Map<Object, Object> beans;

        public MergedMap(Map<Object, Object> additionalBeans, Map<Object, Object> beans) {
            this.additionalBeans = additionalBeans;
            this.beans = beans;
        }

        @Override
        public Object get(Object key) {
            Object bean = this.additionalBeans.get(key);
            if (bean == null) {
                bean = this.beans.get(key);
            }
            return bean;
        }

        @Override
        public boolean containsKey(Object key) {
            return this.beans.containsKey(key) || this.additionalBeans.containsKey(key);
        }

        @Override
        public Set<Object> keySet() {
            throw new FlowableException("unsupported operation on configuration beans");
            // List<String> beanNames =
            // Arrays.asList(beanFactory.getBeanDefinitionNames());
            // return new HashSet<Object>(beanNames);
        }

        @Override
        public void clear() {
            throw new FlowableException("can't clear configuration beans");
        }

        @Override
        public boolean containsValue(Object value) {
            throw new FlowableException("can't search values in configuration beans");
        }

        @Override
        public Set<Map.Entry<Object, Object>> entrySet() {
            throw new FlowableException("unsupported operation on configuration beans");
        }

        @Override
        public boolean isEmpty() {
            throw new FlowableException("unsupported operation on configuration beans");
        }

        @Override
        public Object put(Object key, Object value) {
            throw new FlowableException("unsupported operation on configuration beans");
        }

        @Override
        public void putAll(Map<? extends Object, ? extends Object> m) {
            throw new FlowableException("unsupported operation on configuration beans");
        }

        @Override
        public Object remove(Object key) {
            throw new FlowableException("unsupported operation on configuration beans");
        }

        @Override
        public int size() {
            throw new FlowableException("unsupported operation on configuration beans");
        }

        @Override
        public Collection<Object> values() {
            throw new FlowableException("unsupported operation on configuration beans");
        }
    }
}
