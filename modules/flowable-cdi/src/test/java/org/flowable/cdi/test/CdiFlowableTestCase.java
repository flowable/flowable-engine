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
package org.flowable.cdi.test;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for executing flowable-cdi tests in a Java SE environment, using Weld-SE.
 * 
 * @author Daniel Meyer
 */
public abstract class CdiFlowableTestCase extends BaseCdiFlowableTestCase {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CdiFlowableTestCase.class);

    @Deployment
    public static JavaArchive createDeployment() {

        return ShrinkWrap.create(JavaArchive.class).addPackages(true, "org.flowable.cdi").addAsManifestResource("META-INF/beans.xml", "beans.xml");
    }
}
