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
package org.flowable.engine.test.bpmn.servicetask;

import org.flowable.engine.impl.test.AbstractFlowableTestCase;
import org.flowable.engine.impl.test.PluggableFlowableExtension;
import org.flowable.engine.impl.webservice.MockWebServiceExtension;
import org.flowable.engine.impl.webservice.WebServiceMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Esteban Robles Luna
 */
@Tag("webservice")
@Tag("pluggable")
@ExtendWith(MockWebServiceExtension.class)
@ExtendWith(PluggableFlowableExtension.class)
public abstract class AbstractWebServiceTaskTest extends
    AbstractFlowableTestCase {

    protected WebServiceMock webServiceMock;

    @BeforeEach
    protected void setUp(WebServiceMock webServiceMock) {
        this.webServiceMock = webServiceMock;
    }

}
