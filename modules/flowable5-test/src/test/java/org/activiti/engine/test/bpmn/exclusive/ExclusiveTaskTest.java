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
package org.activiti.engine.test.bpmn.exclusive;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * 
 * @author Daniel Meyer
 */
public class ExclusiveTaskTest extends PluggableFlowableTestCase {

    @Deployment
    public void testNonExclusiveService() {
        // start process
        runtimeService.startProcessInstanceByKey("exclusive");
        // now there should be 1 non-exclusive job in the database:
        Job job = managementService.createJobQuery().singleResult();
        assertNotNull(job);
        assertFalse(((JobEntity) job).isExclusive());

        waitForJobExecutorToProcessAllJobs(6000L, 300L);

        // all the jobs are done
        assertEquals(0, managementService.createJobQuery().count());
    }

    @Deployment
    public void testExclusiveService() {
        // start process
        runtimeService.startProcessInstanceByKey("exclusive");
        // now there should be 1 exclusive job in the database:
        Job job = managementService.createJobQuery().singleResult();
        assertNotNull(job);
        assertTrue(((JobEntity) job).isExclusive());

        waitForJobExecutorToProcessAllJobs(6000L, 300L);

        // all the jobs are done
        assertEquals(0, managementService.createJobQuery().count());
    }

    @Deployment
    public void testExclusiveServiceConcurrent() {
        // start process
        runtimeService.startProcessInstanceByKey("exclusive");
        // now there should be 3 exclusive jobs in the database:
        assertEquals(3, managementService.createJobQuery().count());

        waitForJobExecutorToProcessAllJobs(10000L, 400L);

        // all the jobs are done
        assertEquals(0, managementService.createJobQuery().count());
    }

}
