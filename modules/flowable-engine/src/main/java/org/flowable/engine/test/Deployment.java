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

package org.flowable.engine.test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for a test method to create and delete a deployment around a test method.
 *
 * If no 'resources' or 'extraResources' are set, the current package, test class name and test method are used to find the test resource (see example below).
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>
 * package org.example;
 * 
 * ...
 * 
 * public class ExampleTest {
 * 
 *   &#64;Deployment
 *   public void testForADeploymentWithASingleResource() {
 *     // a deployment will be available in the engine repository
 *     // containing the single resource <b>org/example/ExampleTest.testForADeploymentWithASingleResource.bpmn20.xml</b>
 *   }
 * 
 *   &#64;Deployment(resources = { 
 *     "org/example/processOne.bpmn20.xml",
 *     "org/example/processTwo.bpmn20.xml",
 *     "org/example/some.other.resource" })
 *   public void testForADeploymentWithASingleResource() {
 *     // a deployment will be available in the engine repository
 *     // containing the three resources
 *   }
 *
 *   &#64;Deployment(resources = { "org/example/processOne.bpmn20.xml" },
 *     tenantId = "example")
 *   public void testForATenantDeploymentWithASingleResource() {
 *     // a deployment will be available in the engine repository
 *     // containing the single resource for the specified tenant
 *   }
 * </pre>
 * 
 * @author Dave Syer
 * @author Tom Baeyens
 * @author Tim Stephenson
 * @author Joram Barrez
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Deployment {

    /**
     * Specify all the resources that make up the deployment.
     * When using this property, all resources should be passed, as no automatic detection will be done.
     */
    String[] resources() default {};

    /**
     * Specify resources that are extra, on top of the automatically detected test resources.
     *
     * This is for example useful when testing a BPMN model with a call activity and the called process definition needs to be included too.
     * When using the 'resources' property, both should be passed. With this property, only the called process definition needs to be set.
     */
    String[] extraResources() default {};

    /** Specify tenantId to deploy for */
    String tenantId() default "";
}
