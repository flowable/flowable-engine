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
package org.flowable.standalone.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.FlowableTest;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
@FlowableTest
@ConfigurationResource("flowable.custom-jupiter.cfg.xml")
class FlowableJupiterCustomResourceTest {

    @Test
    void customResourceUsage(ProcessEngine processEngine) {
        assertThat(processEngine.getName()).as("process engine name").isEqualTo("customName");
    }
}
