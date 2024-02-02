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
package org.flowable.engine.test.mock;

import java.util.Collections;
import java.util.List;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.bpmn.parser.factory.ActivityBehaviorFactory;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.test.NoOpServiceTask;
import org.flowable.engine.test.TestActivityBehaviorFactory;

/**
 * @author Joram Barrez
 */
public class FlowableMockSupport {

    protected TestActivityBehaviorFactory testActivityBehaviorFactory;

    public FlowableMockSupport(TestActivityBehaviorFactory testActivityBehaviorFactory) {
        this.testActivityBehaviorFactory = testActivityBehaviorFactory;
    }

    public FlowableMockSupport(ProcessEngine processEngine) {
        ProcessEngineConfigurationImpl processEngineConfiguration = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration();
        ActivityBehaviorFactory existingActivityBehaviorFactory = processEngineConfiguration.getActivityBehaviorFactory();
        this.testActivityBehaviorFactory = new TestActivityBehaviorFactory(existingActivityBehaviorFactory);

        processEngineConfiguration.setActivityBehaviorFactory(testActivityBehaviorFactory);
        processEngineConfiguration.getBpmnParser().setActivityBehaviorFactory(testActivityBehaviorFactory);
    }

    public static boolean isMockSupportPossible(ProcessEngine processEngine) {
        return processEngine instanceof ProcessEngineImpl;
    }

    public void mockServiceTaskWithClassDelegate(String originalClassFqn, Class<?> mockedClass) {
        testActivityBehaviorFactory.addClassDelegateMock(originalClassFqn, mockedClass);
    }

    public void mockServiceTaskWithClassDelegate(String originalClassFqn, String mockedClassFqn) {
        testActivityBehaviorFactory.addClassDelegateMock(originalClassFqn, mockedClassFqn);
    }

    public void mockServiceTaskByIdWithClassDelegate(String taskId, Class<?> mockedClass) {
        testActivityBehaviorFactory.addClassDelegateMockByTaskId(taskId, mockedClass);
    }

    public void mockServiceTaskByIdWithClassDelegate(String taskId, String mockedClassFqn) {
        testActivityBehaviorFactory.addClassDelegateMockByTaskId(taskId, mockedClassFqn);
    }

    public void setAllServiceTasksNoOp() {
        testActivityBehaviorFactory.setAllServiceTasksNoOp();
    }

    public void addNoOpServiceTaskById(String id) {
        testActivityBehaviorFactory.addNoOpServiceTaskById(id);
    }

    public void addNoOpServiceTaskByClassName(String className) {
        testActivityBehaviorFactory.addNoOpServiceTaskByClassName(className);
    }

    public int getNrOfNoOpServiceTaskExecutions() {
        return NoOpServiceTask.CALL_COUNT.get();
    }

    public List<String> getExecutedNoOpServiceTaskDelegateClassNames() {
        return Collections.unmodifiableList(NoOpServiceTask.NAMES);
    }

    public void reset() {
        testActivityBehaviorFactory.reset();
    }

}
