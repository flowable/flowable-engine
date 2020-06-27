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
package org.flowable.cdi.test.impl.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.Test;

public class MultiInstanceServiceTaskEventTest extends CdiFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/cdi/test/impl/event/MultiInstanceServiceTaskEvent.bpmn20.xml" })
    public void testReceiveAll() {

        TestEventListener listenerBean = getBeanInstance(TestEventListener.class);
        listenerBean.reset();

        assertThat(listenerBean.getStartActivityService1WithLoopCounter()).isZero();
        assertThat(listenerBean.getEndActivityService1WithLoopCounter()).isZero();
        assertThat(listenerBean.getEndActivityService1WithoutLoopCounter()).isZero();

        assertThat(listenerBean.getStartActivityService2WithLoopCounter()).isZero();
        assertThat(listenerBean.getEndActivityService2WithLoopCounter()).isZero();
        assertThat(listenerBean.getEndActivityService2WithoutLoopCounter()).isZero();

        // start the process
        runtimeService.startProcessInstanceByKey("process1");

        // assert
        assertThat(listenerBean.getTakeTransitiont1()).isEqualTo(1);
        assertThat(listenerBean.getTakeTransitiont2()).isEqualTo(1);
        assertThat(listenerBean.getTakeTransitiont3()).isEqualTo(1);

        assertThat(listenerBean.getStartActivityService1WithLoopCounter()).isEqualTo(2);
        assertThat(listenerBean.getStartActivityService2WithLoopCounter()).isEqualTo(3);

        assertThat(listenerBean.getEndActivityService1WithLoopCounter()).isEqualTo(2);
        assertThat(listenerBean.getEndActivityService1WithoutLoopCounter()).isEqualTo(1);
        assertThat(listenerBean.getEndActivityService2WithLoopCounter()).isEqualTo(3);
        assertThat(listenerBean.getEndActivityService2WithoutLoopCounter()).isEqualTo(1);

    }
}
