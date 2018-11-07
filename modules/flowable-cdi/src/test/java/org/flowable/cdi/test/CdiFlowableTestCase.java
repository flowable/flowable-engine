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
package org.flowable.cdi.test;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import javax.enterprise.inject.spi.BeanManager;

import org.flowable.cdi.BusinessProcess;
import org.flowable.cdi.impl.util.ProgrammaticBeanLookup;
import org.flowable.cdi.test.util.ProcessEngineLookupForTestsuite;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.test.FlowableRule;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for executing flowable-cdi tests in a Java SE environment, using Weld-SE.
 * 
 * @author Daniel Meyer
 */
@RunWith(Arquillian.class)
public abstract class CdiFlowableTestCase {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CdiFlowableTestCase.class);

    @Deployment
    public static JavaArchive createDeployment() {

        return ShrinkWrap.create(JavaArchive.class).addPackages(true, "org.flowable.cdi").addAsManifestResource("META-INF/beans.xml", "beans.xml");
    }

    @Rule
    public FlowableRule flowableRule = new FlowableRule(getBeanInstance(ProcessEngine.class));

    protected BeanManager beanManager;

    protected ProcessEngine processEngine;
    protected FormService formService;
    protected HistoryService historyService;
    protected IdentityService identityService;
    protected ManagementService managementService;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected TaskService taskService;
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    @Before
    public void setUp() throws Exception {

        beanManager = ProgrammaticBeanLookup.lookup(BeanManager.class);
        processEngine = ProgrammaticBeanLookup.lookup(ProcessEngine.class);
        processEngineConfiguration = ((ProcessEngineImpl) ProcessEngineLookupForTestsuite.processEngine).getProcessEngineConfiguration();
        flowableRule.setProcessEngineConfiguration(processEngineConfiguration);
        formService = processEngine.getFormService();
        historyService = processEngine.getHistoryService();
        identityService = processEngine.getIdentityService();
        managementService = processEngine.getManagementService();
        repositoryService = processEngine.getRepositoryService();
        runtimeService = processEngine.getRuntimeService();
        taskService = processEngine.getTaskService();
    }

    protected void endConversationAndBeginNew(String processInstanceId) {
        getBeanInstance(BusinessProcess.class).associateExecutionById(processInstanceId);
    }

    protected <T> T getBeanInstance(Class<T> clazz) {
        return ProgrammaticBeanLookup.lookup(clazz);
    }

    protected Object getBeanInstance(String name) {
        return ProgrammaticBeanLookup.lookup(name);
    }

    // ////////////////////// copied from AbstractFlowableTestcase

    public void waitForJobExecutorToProcessAllJobs(long maxMillisToWait, long intervalMillis) {
        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        asyncExecutor.start();

        try {
            Timer timer = new Timer();
            InterruptTask task = new InterruptTask(Thread.currentThread());
            timer.schedule(task, maxMillisToWait);
            boolean areJobsAvailable = true;
            try {
                while (areJobsAvailable && !task.isTimeLimitExceeded()) {
                    Thread.sleep(intervalMillis);
                    areJobsAvailable = areJobsAvailable();
                }
            } catch (InterruptedException e) {
            } finally {
                timer.cancel();
            }
            if (areJobsAvailable) {
                throw new FlowableException("time limit of " + maxMillisToWait + " was exceeded");
            }

        } finally {
            asyncExecutor.shutdown();
        }
    }

    public void waitForJobExecutorOnCondition(long maxMillisToWait, long intervalMillis, Callable<Boolean> condition) {
        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        asyncExecutor.start();

        try {
            Timer timer = new Timer();
            InterruptTask task = new InterruptTask(Thread.currentThread());
            timer.schedule(task, maxMillisToWait);
            boolean conditionIsViolated = true;
            try {
                while (conditionIsViolated) {
                    Thread.sleep(intervalMillis);
                    conditionIsViolated = !condition.call();
                }
            } catch (InterruptedException e) {
            } catch (Exception e) {
                throw new FlowableException("Exception while waiting on condition: " + e.getMessage(), e);
            } finally {
                timer.cancel();
            }
            if (conditionIsViolated) {
                throw new FlowableException("time limit of " + maxMillisToWait + " was exceeded");
            }

        } finally {
            asyncExecutor.shutdown();
        }
    }

    public boolean areJobsAvailable() {
        return !managementService.createJobQuery().list().isEmpty();
    }

    private static class InterruptTask extends TimerTask {
        protected boolean timeLimitExceeded;
        protected Thread thread;

        public InterruptTask(Thread thread) {
            this.thread = thread;
        }

        public boolean isTimeLimitExceeded() {
            return timeLimitExceeded;
        }

        @Override
        public void run() {
            timeLimitExceeded = true;
            thread.interrupt();
        }
    }
}
