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
package org.flowable.spring.boot;

import org.flowable.common.spring.SpringEngineConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Base auto configuration for the different engines.
 *
 * @author Filip Hrisafov
 * @author Javier Casal
 */
public abstract class AbstractSpringEngineAutoConfiguration extends AbstractEngineAutoConfiguration {

    public AbstractSpringEngineAutoConfiguration(FlowableProperties flowableProperties) {
        super(flowableProperties);
    }

    protected void configureSpringEngine(SpringEngineConfiguration engineConfiguration, PlatformTransactionManager transactionManager) {
        engineConfiguration.setTransactionManager(transactionManager);
    }

    /**
     * Get the Object provided by the {@code availableProvider}, otherwise get a unique object from {@code uniqueProvider}.
     * This can be used when we allow users to provide specific implementations per engine. For example to provide a specific
     * {@link org.springframework.core.task.TaskExecutor} and / or {@link org.flowable.spring.job.service.SpringRejectedJobsHandler} for the CMMN Async
     * Executor. Example:
     * <pre><code>
     * &#064;Configuration
     * public class MyCustomConfiguration {
     *
     *     &#064;Bean
     *     &#064;Cmmn
     *     public TaskExecutor cmmnTaskExecutor() {
     *         return new MyCustomTaskExecutor()
     *     }
     *
     *     &#064;@Bean
     *     &#064;Primary
     *     public TaskExecutor primaryTaskExecutor() {
     *         return new SimpleAsyncTaskExecutor()
     *     }
     *
     * }
     * </code></pre>
     * Then when using:
     * <pre><code>
     * &#064;Configuration
     * public class FlowableJobConfiguration {
     *
     *     public SpringAsyncExecutor cmmnAsyncExecutor(
     *         ObjectProvider&lt;TaskExecutor&gt; taskExecutor,
     *         &#064;Cmmn ObjectProvider&lt;TaskExecutor&gt; cmmnTaskExecutor
     *     ) {
     *         TaskExecutor executor = getIfAvailable(
     *             cmmnTaskExecutor,
     *             taskExecutor
     *         );
     *         // executor is an instance of MyCustomTaskExecutor
     *     }
     *
     *     public SpringAsyncExecutor processAsyncExecutor(
     *         ObjectProvider&lt;TaskExecutor&gt; taskExecutor,
     *         &#064;Process ObjectProvider&lt;TaskExecutor&gt; processTaskExecutor
     *     ) {
     *         TaskExecutor executor = getIfAvailable(
     *             processTaskExecutor,
     *             taskExecutor
     *         );
     *         // executor is an instance of SimpleAsyncTaskExecutor
     *     }
     * }
     * </code></pre>
     *
     * @param availableProvider
     *     a provider that can provide an available object
     * @param uniqueProvider
     *     a provider that would be used if there is no available object, but only if it is unique
     * @param <T>
     *     the type of the object being provided
     * @return the available object from {@code availableProvider} if there, otherwise the unique object from {@code uniqueProvider}
     */
    protected <T> T getIfAvailable(ObjectProvider<T> availableProvider, ObjectProvider<T> uniqueProvider) {
        // This can be implemented by using availableProvider.getIfAvailable(() -> uniqueProvider.getIfUnique()). However, that is only there in Spring 5
        // and we want to be support Spring 4 with the starters as well
        T object = availableProvider.getIfAvailable();
        if (object == null) {
            object = uniqueProvider.getIfUnique();
        }
        return object;
    }
}
