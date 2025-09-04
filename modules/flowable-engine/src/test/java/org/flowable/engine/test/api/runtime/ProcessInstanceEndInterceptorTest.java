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
package org.flowable.engine.test.api.runtime;
import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.interceptor.EndProcessInstanceInterceptor;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Christopher Welsch
 */
public class ProcessInstanceEndInterceptorTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testEndProcessInterceptorIsCalled() {
        ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
        try {
            TestEndProcessInstanceInterceptor testEndProcessInstanceInterceptor = new TestEndProcessInstanceInterceptor();
            processEngineConfiguration.setEndProcessInstanceInterceptor(testEndProcessInstanceInterceptor);

            runtimeService.startProcessInstanceByKey("oneTaskProcess");
            taskService.complete(taskService.createTaskQuery().singleResult().getId());
            assertThat(testEndProcessInstanceInterceptor.beforeIsCalled).isTrue();
            assertThat(testEndProcessInstanceInterceptor.beforeIsTerminated).isFalse();

            assertThat(testEndProcessInstanceInterceptor.afterIsCalled).isTrue();
            assertThat(testEndProcessInstanceInterceptor.afterIsTerminated).isFalse();
        } finally {
            processEngineConfiguration.setEndProcessInstanceInterceptor(null);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
    public void testEndProcessInterceptorIsNotCalledForTermination() {
        ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
        try {
            TestEndProcessInstanceInterceptor testEndProcessInstanceInterceptor = new TestEndProcessInstanceInterceptor();
            processEngineConfiguration.setEndProcessInstanceInterceptor(testEndProcessInstanceInterceptor);

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            runtimeService.deleteProcessInstance(processInstance.getId(), "test");
            assertThat(testEndProcessInstanceInterceptor.beforeIsCalled).isTrue();
            assertThat(testEndProcessInstanceInterceptor.beforeIsTerminated).isTrue();
            assertThat(testEndProcessInstanceInterceptor.afterIsCalled).isTrue();
            assertThat(testEndProcessInstanceInterceptor.afterIsTerminated).isTrue();
        } finally {
            processEngineConfiguration.setEndProcessInstanceInterceptor(null);
        }
    }

    public static class TestEndProcessInstanceInterceptor implements EndProcessInstanceInterceptor {

        protected boolean beforeIsCalled = false;
        protected boolean beforeIsTerminated = false;

        protected boolean afterIsCalled = false;
        protected boolean afterIsTerminated = false;
        @Override
        public void beforeEndProcessInstance(ExecutionEntity processInstance, boolean isTerminated) {
            beforeIsCalled = true;
            beforeIsTerminated= isTerminated;
        }

        @Override
        public void afterEndProcessInstance(String processInstanceId, boolean isTerminated) {
            afterIsCalled = true;
            afterIsTerminated = isTerminated;
        }
    }
}
