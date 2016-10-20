package org.activiti.examples.runtime;

import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.test.Deployment;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * This test shows that bpmn endless loop with activiti6 is not only fiction
 */
public class StandardAgendaFailingTest extends PluggableActivitiTestCase{

    @Ignore("Endless loop with the standard agenda implementation can run 'forever'.")
    @Deployment(resources = "org/activiti/examples/runtime/WatchDogAgendaTest-endlessloop.bpmn20.xml")
    public void ignoreStandardAgendaWithEndLessLoop() {
        this.runtimeService.startProcessInstanceByKey("endlessloop");
    }

    public void testEmpty() {
        // empty test to let maven build pass through
    }
}
