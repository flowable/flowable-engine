package org.flowable.camel.cdi;

import javax.enterprise.inject.spi.BeanManager;

import org.flowable.cdi.impl.util.ProgrammaticBeanLookup;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.test.FlowableRule;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for camel CDI tests adapted from CdiFlowableTestCase to avoid dependency on flowable-cdi test jar.
 * 
 * @author Zach Visagie
 */
@RunWith(Arquillian.class)
public abstract class BaseCamelCdiFlowableTestCase {
    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseCamelCdiFlowableTestCase.class);

    @Rule
    public FlowableRule flowableRule = new FlowableRule(ProgrammaticBeanLookup.lookup(ProcessEngine.class));

    protected BeanManager beanManager;

    protected ProcessEngine processEngine;
    protected ManagementService managementService;
    protected RuntimeService runtimeService;
    protected TaskService taskService;

    @Before
    public void setUp() throws Exception {
        processEngine = flowableRule.getProcessEngine();
        runtimeService = flowableRule.getRuntimeService();
        taskService = flowableRule.getTaskService();
        managementService = flowableRule.getManagementService();
    }
}
