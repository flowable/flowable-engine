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
package org.flowable.standalone.event;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;

/**
 * Test getProcess on ProcessDefinition {@link FlowableEngineEventType#ENTITY_INITIALIZED} event
 *
 * @author martin.grofcik
 */
public class GetProcessOnProcessDefinitionInitializesEventTest extends ResourceFlowableTestCase {

    public GetProcessOnProcessDefinitionInitializesEventTest() {
        super("org/flowable/standalone/event/flowable-typed-processDefinition.cfg.xml");
    }

    /**
     * Test to verify process is accessible on processDefinitionEntityInitialized event.
     */
    public void testProcessAccessibleProcessDefinitionEntityInitializedEvent() throws Exception {
        // deploy any process
        this.deployOneTaskTestProcess();
        assertThat("ProcessId must be accessible during processDefinition entity initialized event.", GetProcessOnDefinitionInitializedListener.processId, is("oneTaskProcess"));
    }

}
