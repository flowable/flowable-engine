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
package org.flowable.content.engine.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.flowable.content.api.ContentService;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngineConfiguration;
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
 *   public FlowableContentRule flowableContentRule = new FlowableContentRule();
 *   
 *   ...
 * }
 * </pre>
 * 
 * <p>
 * The DmnEngine and the services will be made available to the test class through the getters of the FlowableContentRule. The dmnEngine will be initialized by default with the flowable.dmn.cfg.xml
 * resource on the classpath. To specify a different configuration file, pass the resource location in {@link #FlowableContentRule(String) the appropriate constructor}. Process engines will be cached
 * statically. Right before the first time the setUp is called for a given configuration resource, the process engine will be constructed.
 * </p>
 * 
 * <p>
 * You can declare a deployment with the {@link FormDeploymentAnnotation} annotation. This base class will make sure that this deployment gets deployed before the setUp and
 * {@link RepositoryService#deleteDeployment(String, boolean) cascade deleted} after the tearDown.
 * </p>
 * 
 * <p>
 * The FlowableRule also lets you {@link FlowableContentRule#setCurrentTime(Date) set the current time used by the process engine}. This can be handy to control the exact time that is used by the
 * engine in order to verify e.g. e.g. due dates of timers. Or start, end and duration times in the history service. In the tearDown, the internal clock will automatically be reset to use the current
 * system time rather then the time that was set during a test method.
 * </p>
 * 
 * @author Tijs Rademakers
 */
public class FlowableContentRule implements TestRule {

    protected String configurationResource = "flowable.content.cfg.xml";
    protected String deploymentId;

    protected ContentEngineConfiguration contentEngineConfiguration;
    protected ContentEngine contentEngine;
    protected ContentService contentService;

    public FlowableContentRule() {
    }

    public FlowableContentRule(String configurationResource) {
        this.configurationResource = configurationResource;
    }

    public FlowableContentRule(ContentEngine contentEngine) {
        setContentEngine(contentEngine);
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
        if (contentEngine == null) {
            initializeContentEngine();
        }

        if (contentEngineConfiguration == null) {
            initializeServices();
        }

        configureContentEngine();
    }

    protected void initializeContentEngine() {
        contentEngine = ContentTestHelper.getContentEngine(configurationResource);
    }

    protected void initializeServices() {
        contentEngineConfiguration = contentEngine.getContentEngineConfiguration();
        contentService = contentEngine.getContentService();
    }

    protected void configureContentEngine() {
        /* meant to be overridden */
    }

    protected void finished(Description description) {
    }

    public String getConfigurationResource() {
        return configurationResource;
    }

    public void setConfigurationResource(String configurationResource) {
        this.configurationResource = configurationResource;
    }

    public ContentEngine getContentEngine() {
        return contentEngine;
    }

    public void setContentEngine(ContentEngine contentEngine) {
        this.contentEngine = contentEngine;
        initializeServices();
    }

    public ContentService getContentService() {
        return contentService;
    }

    public void setContentService(ContentService contentService) {
        this.contentService = contentService;
    }

    public void setContentEngineConfiguration(ContentEngineConfiguration contentEngineConfiguration) {
        this.contentEngineConfiguration = contentEngineConfiguration;
    }
}
