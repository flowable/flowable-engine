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
package org.flowable.engine.test.cfg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.RetryInterceptor;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class RetryInterceptorTest {

    protected ProcessEngine processEngine;

    protected RetryInterceptor retryInterceptor;

    @BeforeEach
    public void setupProcessEngine() {
        ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) new StandaloneInMemProcessEngineConfiguration();
        processEngineConfiguration.setJdbcUrl("jdbc:h2:mem:retryInterceptorTest");
        List<CommandInterceptor> interceptors = new ArrayList<>();
        retryInterceptor = new RetryInterceptor();
        interceptors.add(retryInterceptor);
        processEngineConfiguration.setCustomPreCommandInterceptors(interceptors);
        processEngine = processEngineConfiguration.buildProcessEngine();
    }

    @AfterEach
    public void shutdownProcessEngine() {
        processEngine.close();
    }

    @Test
    public void testRetryInterceptor() {

        try {
            processEngine.getManagementService().executeCommand(new CommandThrowingOptimisticLockingException());
            Assert.fail("ActivitiException expected.");
        } catch (FlowableException e) {
            Assert.assertTrue(e.getMessage().contains(retryInterceptor.getNumOfRetries() + " retries failed"));
        }

        Assert.assertEquals(retryInterceptor.getNumOfRetries() + 1, counter.get()); // +1, we retry 3 times, so one extra for the regular execution
    }

    public static AtomicInteger counter = new AtomicInteger();

    protected class CommandThrowingOptimisticLockingException implements Command<Void> {

        @Override
        public Void execute(CommandContext commandContext) {

            counter.incrementAndGet();

            throw new FlowableOptimisticLockingException("");
        }
    }
}
