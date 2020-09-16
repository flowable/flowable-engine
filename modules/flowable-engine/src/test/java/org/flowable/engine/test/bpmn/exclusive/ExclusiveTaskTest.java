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
package org.flowable.engine.test.bpmn.exclusive;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

/**
 * 
 * @author Daniel Meyer
 */
public class ExclusiveTaskTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testNonExclusiveService() {
        // start process
        runtimeService.startProcessInstanceByKey("exclusive");
        // now there should be 1 non-exclusive job in the database:
        Job job = managementService.createJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.isExclusive()).isFalse();

        waitForJobExecutorToProcessAllJobs(6000L, 100L);

        // all the jobs are done
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testExclusiveService() {
        // start process
        runtimeService.startProcessInstanceByKey("exclusive");
        // now there should be 1 exclusive job in the database:
        Job job = managementService.createJobQuery().singleResult();
        assertThat(job).isNotNull();
        assertThat(job.isExclusive()).isTrue();

        waitForJobExecutorToProcessAllJobs(6000L, 100L);

        // all the jobs are done
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment
    public void testExclusiveServiceConcurrent() {
        // start process
        runtimeService.startProcessInstanceByKey("exclusive");
        // now there should be 3 exclusive jobs in the database:
        assertThat(managementService.createJobQuery().count()).isEqualTo(3);

        waitForJobExecutorToProcessAllJobs(20000L, 400L);

        // all the jobs are done
        assertThat(managementService.createJobQuery().count()).isZero();
    }

}
