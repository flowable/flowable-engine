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
package org.flowable.standalone.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class CustomListenerFactoryTest extends ResourceFlowableTestCase {

    public CustomListenerFactoryTest() {
        super("org/flowable/standalone/parsing/custom.listenerfactory.flowable.cfg.xml");
    }

    // The custom activity factory will change this value
    public static AtomicInteger COUNTER = new AtomicInteger(0);

    @BeforeEach
    protected void setUp() throws Exception {
        COUNTER.set(0);
    }

    @Test
    @Deployment
    public void testCustomListenerFactory() {
        int nrOfProcessInstances = 4;
        for (int i = 0; i < nrOfProcessInstances; i++) {
            runtimeService.startProcessInstanceByKey("oneTaskProcess");
        }

        assertThat(COUNTER.get()).isEqualTo(nrOfProcessInstances * 100); // Each listener invocation will add 100
    }

}
