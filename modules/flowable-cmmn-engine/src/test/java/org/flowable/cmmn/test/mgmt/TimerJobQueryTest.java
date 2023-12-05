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
import static org.assertj.core.api.Assertions.tuple;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;
import org.junit.Test;

/**
 * @author Simon Amport
 */
public class TimerJobQueryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/mgmt/TimerJobQueryTest.cmmn")
    public void testQueryByCaseDefinitionKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("timerJobQueryTest")
                .start();

        TimerJobQuery query = cmmnManagementService.createTimerJobQuery().caseDefinitionKey("timerJobQueryTest");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult().getScopeId()).isEqualTo(caseInstance.getId());
        assertThat(query.list())
                .extracting(Job::getElementId, Job::getScopeId)
                .containsExactly(
                        tuple("timerEventListener1", caseInstance.getId())
                );

        query = cmmnManagementService.createTimerJobQuery().caseDefinitionKey("invalid");
        assertThat(query.count()).isZero();
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
    }

}
