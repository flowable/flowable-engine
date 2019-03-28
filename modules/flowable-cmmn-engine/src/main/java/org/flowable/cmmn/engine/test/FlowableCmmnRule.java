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

package org.flowable.cmmn.engine.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * Convenience for CmmnEngine and services initialization in the form of a JUnit rule.
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>
 * public class YourTest {
 * 
 *   &#64;Rule
 *   public FlowableCmmnRule flowableRule = new FlowableCmmnRule();
 *   
 *   ...
 * }
 * </pre>
 * 
 * <p>
 * The CmmnEngine and the services will be made available to the test class through the getters of the FlowableCmmnRule. The cmmnEngine will be initialized by default with the flowable.cfg.xml
 * resource on the classpath. To specify a different configuration file, pass the resource location in {@link #FlowableCmmnRule(String) the appropriate constructor}. Cmmn engines will be cached
 * statically. Right before the first time the setUp is called for a given configuration resource, the cmmn engine will be constructed.
 * </p>
 * 
 * <p>
 * You can declare a deployment with the {@link CmmnDeployment} annotation. This base class will make sure that this deployment gets deployed before the setUp and
 * {@link CmmnRepositoryService#deleteDeployment(String, boolean) cascade deleted} after the tearDown.
 * </p>
 * 
 * <p>
 * The FlowableRule also lets you {@link FlowableCmmnRule#setCurrentTime(Date) set the current time used by the process engine}. This can be handy to control the exact time that is used by the engine in
 * order to verify e.g. e.g. due dates of timers. Or start, end and duration times in the history service. In the tearDown, the internal clock will automatically be reset to use the current system
 * time rather then the time that was set during a test method.
 * </p>
 * 
 * @author Tom Baeyens
 */
public class FlowableCmmnRule implements TestRule {

    protected String configurationResource = "flowable.cmmn.cfg.xml";
    protected String deploymentId;

    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    protected CmmnEngine cmmnEngine;
    protected CmmnRepositoryService cmmnRepositoryService;
    protected CmmnRuntimeService cmmnRuntimeService;
    protected CmmnTaskService cmmnTaskService;
    protected CmmnHistoryService cmmnHistoryService;
    protected CmmnManagementService cmmnManagementService;

    public FlowableCmmnRule() {
    }

    public FlowableCmmnRule(String configurationResource) {
        this.configurationResource = configurationResource;
    }

    public FlowableCmmnRule(CmmnEngine cmmnEngine) {
        setCmmnEngine(cmmnEngine);
    }

    /**
     * Implementation based on {@link TestWatcher}.
     */
    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                List<Throwable> errors = new ArrayList<>();

                startingQuietly(description, errors);
                try {
                    base.evaluate();
                    succeededQuietly(description, errors);
                } catch (AssumptionViolatedException e) {
                    errors.add(e);
                    skippedQuietly(e, description, errors);
                } catch (Throwable t) {
                    errors.add(t);
                    failedQuietly(t, description, errors);
                } finally {
                    finishedQuietly(description, errors);
                }

                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    private void succeededQuietly(Description description, List<Throwable> errors) {
        try {
            succeeded(description);
        } catch (Throwable t) {
            errors.add(t);
        }
    }

    private void failedQuietly(Throwable t, Description description, List<Throwable> errors) {
        try {
            failed(t, description);
        } catch (Throwable t1) {
            errors.add(t1);
        }
    }

    private void skippedQuietly(AssumptionViolatedException e, Description description, List<Throwable> errors) {
        try {
            skipped(e, description);
        } catch (Throwable t) {
            errors.add(t);
        }
    }

    private void startingQuietly(Description description, List<Throwable> errors) {
        try {
            starting(description);
        } catch (Throwable t) {
            errors.add(t);
        }
    }

    private void finishedQuietly(Description description, List<Throwable> errors) {
        try {
            finished(description);
        } catch (Throwable t) {
            errors.add(t);
        }
    }

    /**
     * Invoked when a test succeeds
     */
    protected void succeeded(Description description) {
    }

    /**
     * Invoked when a test fails
     */
    protected void failed(Throwable e, Description description) {
    }

    /**
     * Invoked when a test is skipped due to a failed assumption.
     */
    protected void skipped(AssumptionViolatedException e, Description description) {
    }

    protected void starting(Description description) {
        if (cmmnEngine == null) {
            initializeCmmnEngine();
        }

        if (cmmnEngineConfiguration == null) {
            initializeServices();
        }

        // Allow for mock configuration
        configureProcessEngine();

        // Allow for annotations
        try {
            deploymentId = CmmnTestHelper.annotationDeploymentSetUp(cmmnEngine, Class.forName(description.getClassName()), description.getMethodName());
        } catch (ClassNotFoundException e) {
            throw new FlowableException("Programmatic error: could not instantiate " + description.getClassName(), e);
        }
    }

    protected void initializeCmmnEngine() {
        cmmnEngine = CmmnTestHelper.getCmmnEngine(configurationResource);
    }

    protected void initializeServices() {
        cmmnEngineConfiguration = cmmnEngine.getCmmnEngineConfiguration();
        cmmnRepositoryService = cmmnEngine.getCmmnRepositoryService();
        cmmnRuntimeService = cmmnEngine.getCmmnRuntimeService();
        cmmnTaskService = cmmnEngine.getCmmnTaskService();
        cmmnHistoryService = cmmnEngine.getCmmnHistoryService();
        cmmnManagementService = cmmnEngine.getCmmnManagementService();
    }

    protected void configureProcessEngine() {
        /* meant to be overridden */
    }

    protected void finished(Description description) {

        // Remove the test deployment
        try {
            CmmnTestHelper.annotationDeploymentTearDown(cmmnEngine, deploymentId, Class.forName(description.getClassName()), description.getMethodName());
        } catch (ClassNotFoundException e) {
            throw new FlowableException("Programmatic error: could not instantiate " + description.getClassName(), e);
        }

        // Reset internal clock
        cmmnEngineConfiguration.getClock().reset();
    }

    public void setCurrentTime(Date currentTime) {
        cmmnEngineConfiguration.getClock().setCurrentTime(currentTime);
    }

    public String getConfigurationResource() {
        return configurationResource;
    }

    public void setConfigurationResource(String configurationResource) {
        this.configurationResource = configurationResource;
    }

    public CmmnEngine getCmmnEngine() {
        return cmmnEngine;
    }

    public void setCmmnEngine(CmmnEngine cmmnEngine) {
        this.cmmnEngine = cmmnEngine;
        initializeServices();
    }
    
    public CmmnEngineConfiguration getCmmnEngineConfiguration() {
        return cmmnEngineConfiguration;
    }

    public CmmnRepositoryService getCmmnRepositoryService() {
        return cmmnRepositoryService;
    }

    public CmmnRuntimeService getCmmnRuntimeService() {
        return cmmnRuntimeService;
    }

    public CmmnHistoryService getCmmnHistoryService() {
        return cmmnHistoryService;
    }

    public CmmnManagementService getCmmnManagementService() {
        return cmmnManagementService;
    }

}
