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
package org.flowable.cdi.test.impl.el;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.cdi.test.impl.beans.MessageBean;
import org.flowable.engine.test.Deployment;
import org.junit.Test;

/**
 * @author Daniel Meyer
 */
public class ElTest extends CdiFlowableTestCase {

    @Test
    @Deployment
    public void testSetBeanProperty() throws Exception {
        MessageBean messageBean = getBeanInstance(MessageBean.class);
        runtimeService.startProcessInstanceByKey("setBeanProperty");
        assertThat(messageBean.getMessage()).isEqualTo("Greetings from Flowable");
    }

    // @Test
    // @Deployment(resources="org/flowable/cdi/test/impl/el/ElTest.testInvalidExpression.bpmn20.xml")
    // public void testInvalidExpressions() throws Exception {
    // ProcessInstance pi =
    // runtimeService.startProcessInstanceByKey("invalidExpression");
    //
    // assertEquals("xxx", runtimeService.getVariable(pi.getId(), "test"));
    //
    // // runtimeService.startProcessInstanceByKey("invalidExpressionDelegate");
    //
    // }
}
