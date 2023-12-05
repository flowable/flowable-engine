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

package org.flowable.cmmn.test.mgmt;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.junit.Test;

/**
 * @author Simon Amport
 */
public class JobQueryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/TimerJobQueryTest.cmmn")
    public void testQueryByCaseDefinitionKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("timerJobQueryTest")
                .start();

        Job timerJob = cmmnManagementService.createTimerJobQuery().singleResult();

        Job executableJob = cmmnManagementService.moveTimerToExecutableJob(timerJob.getId());
        assertThat(executableJob).isNotNull();

        JobQuery jobQuery = cmmnManagementService.createJobQuery().caseDefinitionKey("timerJobQueryTest");
        assertThat(jobQuery.count()).isEqualTo(1);
        assertThat(jobQuery.singleResult().getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(jobQuery.list()).extracting(Job::getId)
                .containsExactly(executableJob.getId());

        jobQuery = cmmnManagementService.createJobQuery().caseDefinitionKey("invalid");
        assertThat(jobQuery.count()).isZero();
        assertThat(jobQuery.singleResult()).isNull();
        assertThat(jobQuery.list()).isEmpty();
    }
}
