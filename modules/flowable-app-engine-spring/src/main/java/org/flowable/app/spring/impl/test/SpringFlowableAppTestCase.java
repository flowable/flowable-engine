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
package org.flowable.app.spring.impl.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.app.engine.AppEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import junit.framework.TestCase;

/**
 * @author Joram Barrez
 * @author Josh Long
 */
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public abstract class SpringFlowableAppTestCase extends TestCase implements ApplicationContextAware {

    // we need a data structure to store all the resolved FormEngines and map them to something
    protected Map<Object, AppEngine> cachedAppEngines = new ConcurrentHashMap<>();
    protected AppEngine appEngine;

    protected TestContextManager testContextManager;

    @Autowired
    protected ApplicationContext applicationContext;

    public SpringFlowableAppTestCase() {
        this.testContextManager = new TestContextManager(getClass());
    }

    @Override
    public void runBare() throws Throwable {
        testContextManager.prepareTestInstance(this); // this will initialize all dependencies
        super.runBare();
    }

    protected void initializeAppEngine() {
        ContextConfiguration contextConfiguration = getClass().getAnnotation(ContextConfiguration.class);
        String[] value = contextConfiguration.value();
        boolean hasOneArg = value != null && value.length == 1;
        String key = hasOneArg ? value[0] : AppEngine.class.getName();
        AppEngine engine = this.cachedAppEngines.containsKey(key) ? this.cachedAppEngines.get(key) : this.applicationContext.getBean(AppEngine.class);

        this.cachedAppEngines.put(key, engine);
        this.appEngine = engine;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
