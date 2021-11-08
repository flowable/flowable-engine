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

package org.flowable.cmmn.rest.service.api.management;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.CmmnRestUrls;
import org.flowable.job.api.Job;

/**
 * Test for all REST-operations related to a job collection resource.
 *
 * @author Tijs Rademakers
 */
public class JobCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting a list of case instance, using all possible filters.
     */
    @CmmnDeployment(resources = { "org/flowable/cmmn/rest/service/api/management/timerEventListenerCase.cmmn" })
    public void testGetJobs() throws Exception {
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerExpression").start();
        CaseInstance caseInstance2 = runtimeService.createCaseInstanceBuilder().caseDefinitionKey("testTimerExpression").start();
        
        Job timerJob = managementService.createTimerJobQuery().caseInstanceId(caseInstance.getId()).singleResult();
        Job timerJob2 = managementService.createTimerJobQuery().caseInstanceId(caseInstance2.getId()).singleResult();
        
        // Test without any parameters
        String url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TIMER_JOB_COLLECTION);
        assertResultsPresentInDataResponse(url, timerJob.getId(), timerJob2.getId());

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TIMER_JOB_COLLECTION) + "?id=" + timerJob.getId();
        assertResultsPresentInDataResponse(url, timerJob.getId());

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TIMER_JOB_COLLECTION) + "?id=anotherId";
        assertResultsPresentInDataResponse(url);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TIMER_JOB_COLLECTION) + "?withoutProcessInstanceId=true";
        assertResultsPresentInDataResponse(url, timerJob.getId(), timerJob2.getId());

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_TIMER_JOB_COLLECTION) + "?withoutScopeId=true";
        assertResultsPresentInDataResponse(url);
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_JOB_COLLECTION);
        assertResultsPresentInDataResponse(url);
        
        Job asyncJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        Job asyncJob2 = managementService.moveTimerToExecutableJob(timerJob2.getId());
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_JOB_COLLECTION);
        assertResultsPresentInDataResponse(url, asyncJob.getId(), asyncJob2.getId());
        
        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_JOB_COLLECTION) + "?id=" + asyncJob.getId();
        assertResultsPresentInDataResponse(url, asyncJob.getId());

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_JOB_COLLECTION) + "?id=anotherId";
        assertResultsPresentInDataResponse(url);

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_JOB_COLLECTION) + "?withoutProcessInstanceId=true";
        assertResultsPresentInDataResponse(url, asyncJob.getId(), asyncJob2.getId());

        url = CmmnRestUrls.createRelativeResourceUrl(CmmnRestUrls.URL_JOB_COLLECTION) + "?withoutScopeId=true";
        assertResultsPresentInDataResponse(url);
    }
}
