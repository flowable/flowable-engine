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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that can be used on test classes or as a meta annotation to use a custom configuration resource for the {@link FlowableCmmnExtension}.
 * <p>
 * This annotation can be used to create a custom annotation that would provide the configuration resource to the {@link FlowableCmmnExtension}.
 * </p>
 * Usage
 * <pre><code>
 * &#64;Target({ElementType.TYPE})
 * &#64;Retention(RetentionPolicy.RUNTIME)
 * &#64;Documented
 * &#64;CmmnConfigurationResource("flowable.custom.cmmn.cfg.xm")
 * public &#64;interface MyCustomResource {
 *
 * }
 *
 * &#64;FlowableCmmnTest
 * &#64;MyCustomResource
 * class YourTest {
 *
 *   &#64;BeforeEach
 *   void setUp(CmmnEngine cmmnEngine) {
 *     ...
 *   }
 *
 *   &#64;Test
 *   void myTest(CmmnRuntimeService cmmnRuntimeService) {
 *     ...
 *   }
 *   ...
 * }
 * </code></pre>
 * In this example the configuration flowable.custom.cmmn.cfg.xml will be used to create the CmmnEngine
 *
 * <b>NB:</b> This only works for the tests with JUnit Jupiter.
 *
 * @author Filip Hrisafov
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface CmmnConfigurationResource {

    /**
     * The location of the resource that should be used to create the CmmnEngine.
     */
    String value() default "flowable.cmmn.cfg.xml";
}
