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
package org.flowable.content.spring.impl.test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.impl.test.AbstractFlowableTestCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

/**
 * @author Joram Barrez
 * @author Josh Long
 */
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
public abstract class SpringFlowableTestCase extends AbstractFlowableTestCase implements ApplicationContextAware {

    // we need a data structure to store all the resolved FormEngines and map them to something
    protected Map<Object, ContentEngine> cachedContentEngines = new ConcurrentHashMap<>();

    protected TestContextManager testContextManager;

    @Autowired
    protected ApplicationContext applicationContext;

    public SpringFlowableTestCase() {
        this.testContextManager = new TestContextManager(getClass());
    }

    @Override
    public void runBare() throws Throwable {
        testContextManager.prepareTestInstance(this); // this will initialize all dependencies
        super.runBare();
    }

    @Override
    protected void initializeContentEngine() {
        ContextConfiguration contextConfiguration = getClass().getAnnotation(ContextConfiguration.class);
        String[] value = contextConfiguration.value();
        boolean hasOneArg = value != null && value.length == 1;
        String key = hasOneArg ? value[0] : ContentEngine.class.getName();
        ContentEngine engine = this.cachedContentEngines.containsKey(key) ? this.cachedContentEngines.get(key) : this.applicationContext.getBean(ContentEngine.class);

        this.cachedContentEngines.put(key, engine);
        this.contentEngine = engine;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
