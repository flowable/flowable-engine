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
package org.flowable.dmn.engine.test;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.DmnEngines;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * Convenience for DmnEngine and services initialization in the form of a JUnit rule.
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>
 * public class YourTest {
 * 
 *   &#64;Rule
 *   public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();
 *   
 *   ...
 * }
 * </pre>
 * 
 * <p>
 * The DmnEngine and the services will be made available to the test class through the getters of the FlowableRule. The dmnEngine will be initialized by default with the flowable.dmn.cfg.xml resource
 * on the classpath. To specify a different configuration file, pass the resource location in {@link #FlowableDmnRule(String) the appropriate constructor}. Process engines will be cached statically.
 * Right before the first time the setUp is called for a given configuration resource, the process engine will be constructed.
 * </p>
 * 
 * <p>
 * You can declare a deployment with the {@link DmnDeploymentAnnotation} annotation. This base class will make sure that this deployment gets deployed before the setUp and
 * {@link org.flowable.dmn.api.DmnRepositoryService#deleteDeployment(String)} cascade deleted after the tearDown.
 * </p>
 *
 * @author Tijs Rademakers
 */
public class FlowableDmnRule implements TestRule {

    protected String configurationResource = "flowable.dmn.cfg.xml";
    protected String deploymentId;

    protected DmnEngineConfiguration dmnEngineConfiguration;
    protected DmnEngine dmnEngine;
    protected DmnRepositoryService repositoryService;

    public FlowableDmnRule() {
    }

    public FlowableDmnRule(String configurationResource) {
        this.configurationResource = configurationResource;
    }

    public FlowableDmnRule(DmnEngine dmnEngine) {
        setDmnEngine(dmnEngine);
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
        if (dmnEngine == null) {
            initializeDmnEngine();
        }

        if (dmnEngineConfiguration == null) {
            initializeServices();
        }

        configureDmnEngine();

        try {
            deploymentId = DmnTestHelper.annotationDeploymentSetUp(dmnEngine, Class.forName(description.getClassName()), description.getMethodName());
        } catch (ClassNotFoundException e) {
            throw new FlowableException("Programmatic error: could not instantiate " + description.getClassName(), e);
        }
    }

    protected void initializeDmnEngine() {
        DmnEngines.destroy(); // Just to be sure we're not getting any previously cached version
        dmnEngine = DmnTestHelper.getDmnEngine(configurationResource);
    }

    protected void initializeServices() {
        dmnEngineConfiguration = dmnEngine.getDmnEngineConfiguration();
        repositoryService = dmnEngine.getDmnRepositoryService();
    }

    protected void configureDmnEngine() {
        /* meant to be overridden */
    }

    protected void finished(Description description) {

        // Remove the test deployment
        try {
            DmnTestHelper.annotationDeploymentTearDown(dmnEngine, deploymentId, Class.forName(description.getClassName()), description.getMethodName());
        } catch (ClassNotFoundException e) {
            throw new FlowableException("Programmatic error: could not instantiate " + description.getClassName(), e);
        }
    }

    public String getConfigurationResource() {
        return configurationResource;
    }

    public void setConfigurationResource(String configurationResource) {
        this.configurationResource = configurationResource;
    }

    public DmnEngine getDmnEngine() {
        return dmnEngine;
    }

    public void setDmnEngine(DmnEngine dmnEngine) {
        this.dmnEngine = dmnEngine;
        initializeServices();
    }

    public DmnRepositoryService getRepositoryService() {
        return repositoryService;
    }

    public void setRepositoryService(DmnRepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    public void setDmnEngineConfiguration(DmnEngineConfiguration dmnEngineConfiguration) {
        this.dmnEngineConfiguration = dmnEngineConfiguration;
    }
}
