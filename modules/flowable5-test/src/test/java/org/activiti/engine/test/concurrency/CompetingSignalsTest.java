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

package org.activiti.engine.test.concurrency;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.cfg.CommandExecutorImpl;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.RetryInterceptor;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.impl.delegate.ActivityBehavior;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 */
public class CompetingSignalsTest extends PluggableFlowableTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompetingSignalsTest.class);

    Thread testThread = Thread.currentThread();
    static ControllableThread activeThread;

    public class SignalThread extends ControllableThread {

        String executionId;
        FlowableOptimisticLockingException exception;

        public SignalThread(String executionId) {
            this.executionId = executionId;
        }

        @Override
        public synchronized void startAndWaitUntilControlIsReturned() {
            activeThread = this;
            super.startAndWaitUntilControlIsReturned();
        }

        public void run() {
            try {
                runtimeService.trigger(executionId);
            } catch (FlowableOptimisticLockingException e) {
                this.exception = e;
            }
            LOGGER.debug("{} ends", getName());
        }
    }

    public static class ControlledConcurrencyBehavior implements ActivityBehavior {
        private static final long serialVersionUID = 1L;

        public void execute(DelegateExecution execution) {
            activeThread.returnControlToTestThreadAndWait();
        }
    }

    @Deployment
    public void testCompetingSignals() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("CompetingSignalsProcess");
        String processInstanceId = processInstance.getId();

        LOGGER.debug("test thread starts thread one");
        SignalThread threadOne = new SignalThread(processInstanceId);
        threadOne.startAndWaitUntilControlIsReturned();

        LOGGER.debug("test thread continues to start thread two");
        SignalThread threadTwo = new SignalThread(processInstanceId);
        threadTwo.startAndWaitUntilControlIsReturned();

        LOGGER.debug("test thread notifies thread 1");
        threadOne.proceedAndWaitTillDone();
        assertNull(threadOne.exception);

        LOGGER.debug("test thread notifies thread 2");
        threadTwo.proceedAndWaitTillDone();
        assertNotNull(threadTwo.exception);
        assertTextPresent("was updated by another transaction concurrently", threadTwo.exception.getMessage());
    }

    @Deployment(resources = { "org/activiti/engine/test/concurrency/CompetingSignalsTest.testCompetingSignals.bpmn20.xml" })
    public void testCompetingSignalsWithRetry() throws Exception {
        RuntimeServiceImpl runtimeServiceImpl = (RuntimeServiceImpl) runtimeService;
        CommandExecutorImpl before = (CommandExecutorImpl) runtimeServiceImpl.getCommandExecutor();
        try {
            CommandInterceptor retryInterceptor = new RetryInterceptor();
            retryInterceptor.setNext(before.getFirst());

            runtimeServiceImpl.setCommandExecutor(new CommandExecutorImpl(before.getDefaultConfig(), retryInterceptor));

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("CompetingSignalsProcess");
            String processInstanceId = processInstance.getId();

            LOGGER.debug("test thread starts thread one");
            SignalThread threadOne = new SignalThread(processInstanceId);
            threadOne.startAndWaitUntilControlIsReturned();

            LOGGER.debug("test thread continues to start thread two");
            SignalThread threadTwo = new SignalThread(processInstanceId);
            threadTwo.startAndWaitUntilControlIsReturned();

            LOGGER.debug("test thread notifies thread 1");
            threadOne.proceedAndWaitTillDone();
            assertNull(threadOne.exception);

            LOGGER.debug("test thread notifies thread 2");
            threadTwo.proceedAndWaitTillDone();
            assertNull(threadTwo.exception);
        } finally {
            // restore the command executor
            runtimeServiceImpl.setCommandExecutor(before);
        }

    }
}
