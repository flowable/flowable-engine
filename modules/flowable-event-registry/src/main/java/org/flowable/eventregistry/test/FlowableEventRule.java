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
package org.flowable.eventregistry.test;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

/**
 * Convenience for EventRegistryEngine and services initialization in the form of a JUnit rule.
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>
 * public class YourTest {
 * 
 *   &#64;Rule
 *   public FlowableFormRule flowableEventRule = new FlowableEventRule();
 *   
 *   ...
 * }
 * </pre>
 * 
 * <p>
 * The EventRegistryEngine and the services will be made available to the test class through the getters of the FlowableRule. The dmnEngine will be initialized by default with the flowable.eventregistry.cfg.xml resource
 * on the classpath. To specify a different configuration file, pass the resource location in {@link #FlowableEventRule(String) the appropriate constructor}. Event registry engines will be cached statically.
 * Right before the first time the setUp is called for a given configuration resource, the event registry engine will be constructed.
 * </p>
 * 
 * <p>
 * You can declare a deployment with the {@link EventDeploymentAnnotation} annotation. This base class will make sure that this deployment gets deployed before the setUp and
 * {@link EventRepositoryService#deleteDeployment(String)} after the tearDown.
 * </p>
 *
 * @author Tijs Rademakers
 */
public class FlowableEventRule implements TestRule {

    protected String configurationResource = "flowable.eventregistry.cfg.xml";
    protected String deploymentId;

    protected EventRegistryEngineConfiguration eventEngineConfiguration;
    protected EventRegistryEngine eventRegistryEngine;
    protected EventRepositoryService repositoryService;

    public FlowableEventRule() {
    }

    public FlowableEventRule(String configurationResource) {
        this.configurationResource = configurationResource;
    }

    public FlowableEventRule(EventRegistryEngine eventRegistryEngine) {
        setEventRegistryEngine(eventRegistryEngine);
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
        if (eventRegistryEngine == null) {
            initializeEventRegistryEngine();
        }

        if (eventEngineConfiguration == null) {
            initializeServices();
        }

        configureEventRegistryEngine();

        try {
            deploymentId = EventTestHelper.annotationDeploymentSetUp(repositoryService, Class.forName(description.getClassName()), description.getMethodName());
        } catch (ClassNotFoundException e) {
            throw new FlowableException("Programmatic error: could not instantiate " + description.getClassName(), e);
        }
    }

    protected void initializeEventRegistryEngine() {
        eventRegistryEngine = EventTestHelper.getEventRegistryEngine(configurationResource);
    }

    protected void initializeServices() {
        eventEngineConfiguration = eventRegistryEngine.getEventRegistryEngineConfiguration();
        repositoryService = eventRegistryEngine.getEventRepositoryService();
    }

    protected void configureEventRegistryEngine() {
        /* meant to be overridden */
    }

    protected void finished(Description description) {

        // Remove the test deployment
        try {
            EventTestHelper.annotationDeploymentTearDown(repositoryService, deploymentId, Class.forName(description.getClassName()), description.getMethodName());
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

    public EventRegistryEngine getEventRegistryEngine() {
        return eventRegistryEngine;
    }

    public void setEventRegistryEngine(EventRegistryEngine eventRegistryEngine) {
        this.eventRegistryEngine = eventRegistryEngine;
        initializeServices();
    }

    public EventRepositoryService getRepositoryService() {
        return repositoryService;
    }

    public void setRepositoryService(EventRepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    public void EventRegistryEngineConfiguration(EventRegistryEngineConfiguration eventEngineConfiguration) {
        this.eventEngineConfiguration = eventEngineConfiguration;
    }
}
