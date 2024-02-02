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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author Joram Barrez
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CmmnDeployment {

    /**
     * Specify all the resources that make up the deployment.
     * When using this property, all resources should be passed, as no automatic detection will be done.
     */
    String[] resources() default {};

    /**
     * Specify resources that are extra, on top of the automatically detected test resources.
     *
     * This is for example useful when testing a CMMN model with a case task and that case definition needs to be included too.
     * When using the 'resources' property, both should be passed. With this property, only the called case definition needs to be set.
     */
    String[] extraResources() default {};

    /**
     * Specify tenantId to deploy for
     */
    String tenantId() default "";
    
}
