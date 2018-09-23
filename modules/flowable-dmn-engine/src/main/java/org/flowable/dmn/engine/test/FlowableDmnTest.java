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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Convenience for annotation that activates the {@link FlowableDmnExtension} JUnit Jupiter annotation.
 *
 * <p>
 * Usage:
 * </p>
 *
 * <pre>
 * &#64;FlowableDmnTest
 * class YourTest {
 *
 *   &#64;BeforeEach
 *   void setUp(DmnEngine dmnEngine) {
 *       ...
 *   }
 *
 *   &#64;Test
 *   void myTest(DmnRuleService dmnRuleService) {
 *       ...
 *   }
 *
 *   ...
 * }
 * </pre>
 *
 * <p>
 * The DmnEngine and the services will be made available to the test class through the parameter resolution (BeforeEach, AfterEach, test methods).
 * The DmnEngine will be initialized by default with the flowable.dmn.cfg.xml resource on the classpath.
 * To specify a different configuration file, annotate your class with {@link DmnConfigurationResource}.
 * Dmn engines will be cached as part of the JUnit Jupiter Extension context.
 * Right before the first time the setUp is called for a given configuration resource, the dmn engine will be constructed.
 * </p>
 *
 * <p>
 * You can declare a deployment with the {@link DmnDeployment} or {@link DmnDeploymentAnnotation} annotation.
 * If both annotations are used then {@link DmnDeployment} takes precedence and {@link DmnDeploymentAnnotation} will be ignored.
 * The extension will make sure that this deployment gets deployed before the setUp and
 * {@link org.flowable.dmn.api.DmnRepositoryService#deleteDeployment(String)} cascade deleted} after the tearDown.
 * The id of the deployment can be accessed by using {@link DmnDeploymentId} in a test method.
 * </p>
 *
 * <p>
 * {@link FlowableDmnTestHelper#setCurrentTime(Instant)}  can be used to set the current time used by the dmn engine}
 * This can be handy to control the exact time that is used by the engine in order to verify e.g. e.g. due dates.
 * Or start, end and duration times in the history service. In the tearDown, the internal clock will automatically be reset to use the current system
 * time rather then the time that was set during a test method.
 * </p>
 *
 * @author Filip Hrisafov
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ExtendWith(FlowableDmnExtension.class)
public @interface FlowableDmnTest {

}
