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
package org.flowable.cdi.test.impl.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.cdi.test.impl.beans.ProcessScopedMessageBean;
import org.flowable.engine.test.Deployment;
import org.junit.Test;

/**
 * 
 * @author Daniel Meyer
 */
public class ThreadContextAssociationTest extends CdiFlowableTestCase {

    @Test
    @Deployment
    public void testBusinessProcessScopedWithJobExecutor() throws InterruptedException {
        String pid = runtimeService.startProcessInstanceByKey("processkey").getId();

        waitForJobExecutorToProcessAllJobs(50000000L, 100L);

        assertNull(managementService.createJobQuery().singleResult());

        ProcessScopedMessageBean messageBean = (ProcessScopedMessageBean) runtimeService.getVariable(pid, "processScopedMessageBean");
        assertEquals("test", messageBean.getMessage());

    }

}
