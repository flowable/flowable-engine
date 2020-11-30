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
package org.flowable.form.engine.test;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * Convenience for FormEngine and services initialization in the form of a JUnit rule.
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>
 * public class YourTest {
 * 
 *   &#64;Rule
 *   public FlowableFormRule flowableFormRule = new FlowableFormRule();
 *   
 *   ...
 * }
 * </pre>
 * 
 * <p>
 * The DmnEngine and the services will be made available to the test class through the getters of the FlowableRule. The dmnEngine will be initialized by default with the activiti.dmn.cfg.xml resource
 * on the classpath. To specify a different configuration file, pass the resource location in {@link #FlowableFormRule(String) the appropriate constructor}. Process engines will be cached statically.
 * Right before the first time the setUp is called for a given configuration resource, the process engine will be constructed.
 * </p>
 * 
 * <p>
 * You can declare a deployment with the {@link FormDeploymentAnnotation} annotation. This base class will make sure that this deployment gets deployed before the setUp and
 * {@link FormRepositoryService#deleteDeployment(String)} after the tearDown.
 * </p>
 * 
 * @author Tijs Rademakers
 */
public class FlowableFormRule implements TestRule {

    protected String configurationResource = "flowable.form.cfg.xml";
    protected String deploymentId;

    protected FormEngineConfiguration formEngineConfiguration;
    protected FormEngine formEngine;
    protected FormRepositoryService repositoryService;

    public FlowableFormRule() {
    }

    public FlowableFormRule(String configurationResource) {
        this.configurationResource = configurationResource;
    }

    public FlowableFormRule(FormEngine formEngine) {
        setFormEngine(formEngine);
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
        if (formEngine == null) {
            initializeFormEngine();
        }

        if (formEngineConfiguration == null) {
            initializeServices();
        }

        configureFormEngine();

        try {
            deploymentId = FormTestHelper.annotationDeploymentSetUp(formEngine, Class.forName(description.getClassName()), description.getMethodName());
        } catch (ClassNotFoundException e) {
            throw new FlowableException("Programmatic error: could not instantiate " + description.getClassName(), e);
        }
    }

    protected void initializeFormEngine() {
        formEngine = FormTestHelper.getFormEngine(configurationResource);
    }

    protected void initializeServices() {
        formEngineConfiguration = formEngine.getFormEngineConfiguration();
        repositoryService = formEngine.getFormRepositoryService();
    }

    protected void configureFormEngine() {
        /* meant to be overridden */
    }

    protected void finished(Description description) {

        // Remove the test deployment
        try {
            FormTestHelper.annotationDeploymentTearDown(formEngine, deploymentId, Class.forName(description.getClassName()), description.getMethodName());
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

    public FormEngine getFormEngine() {
        return formEngine;
    }

    public void setFormEngine(FormEngine formEngine) {
        this.formEngine = formEngine;
        initializeServices();
    }

    public FormRepositoryService getRepositoryService() {
        return repositoryService;
    }

    public void setRepositoryService(FormRepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    public void setFormEngineConfiguration(FormEngineConfiguration formEngineConfiguration) {
        this.formEngineConfiguration = formEngineConfiguration;
    }
}
