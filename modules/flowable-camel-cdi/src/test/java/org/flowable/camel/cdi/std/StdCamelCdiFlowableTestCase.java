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
package org.flowable.camel.cdi.std;

import org.flowable.camel.cdi.BaseCamelCdiFlowableTestCase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * This test base class is for supporting the default camel context which is not a named bean in a CDI environment.
 * 
 * @author Zach Visagie
 *
 */
public abstract class StdCamelCdiFlowableTestCase extends BaseCamelCdiFlowableTestCase {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class).addPackages(true, "org.flowable.cdi").addPackages(true, "org.flowable.camel.cdi.std").addAsManifestResource("META-INF/beans.xml", "beans.xml");
    }

}
