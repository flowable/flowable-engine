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

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.DeadLetterJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.junit.Test;

/**
 * @author Simon Amport
 */
public class DeadLetterJobQueryTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void testQueryByCaseDefinitionKey() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .start();

        DeadLetterJobEntity deadLetterJob = cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            DeadLetterJobEntityManager deadLetterJobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getDeadLetterJobEntityManager();
            DeadLetterJobEntity deadLetterJobEntity = deadLetterJobEntityManager.create();
            deadLetterJobEntity.setScopeId(caseInstance.getId());
            deadLetterJobEntity.setScopeDefinitionId(caseInstance.getCaseDefinitionId());
            deadLetterJobEntity.setScopeType(ScopeTypes.CMMN);
            deadLetterJobEntity.setJobType(SuspendedJobEntity.JOB_TYPE_MESSAGE);
            deadLetterJobEntity.setJobHandlerType("testJobHandlerType");

            deadLetterJobEntityManager.insert(deadLetterJobEntity);
            return deadLetterJobEntity;
        });

        DeadLetterJobEntity deadLetterJob2 = cmmnEngineConfiguration.getCommandExecutor().execute(commandContext -> {
            DeadLetterJobEntityManager deadLetterJobEntityManager = cmmnEngineConfiguration.getJobServiceConfiguration().getDeadLetterJobEntityManager();
            DeadLetterJobEntity deadLetterJobEntity = deadLetterJobEntityManager.create();
            deadLetterJobEntity.setProcessInstanceId("PRC-1");
            deadLetterJobEntity.setProcessDefinitionId("PRC-DEF-1");
            deadLetterJobEntity.setJobType(SuspendedJobEntity.JOB_TYPE_MESSAGE);
            deadLetterJobEntity.setJobHandlerType("testJobHandlerType");

            deadLetterJobEntityManager.insert(deadLetterJobEntity);
            return deadLetterJobEntity;
        });

        DeadLetterJobQuery query = cmmnEngineConfiguration.getCmmnManagementService().createDeadLetterJobQuery().caseDefinitionKey("oneTaskCase");
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.list()).extracting(Job::getId).containsExactly(deadLetterJob.getId());
        assertThat(query.singleResult().getId()).isEqualTo(deadLetterJob.getId());

        query = cmmnEngineConfiguration.getCmmnManagementService().createDeadLetterJobQuery().caseDefinitionKey("invalid");
        assertThat(query.count()).isZero();
        assertThat(query.list()).isEmpty();
        assertThat(query.singleResult()).isNull();

        cmmnEngineConfiguration.getCommandExecutor().execute((Command<Void>) commandContext -> {
            JobServiceConfiguration jobServiceConfiguration = cmmnEngineConfiguration.getJobServiceConfiguration();
            DeadLetterJobEntityManager deadLetterJobService = jobServiceConfiguration.getDeadLetterJobEntityManager();
            List<Job> jobs = deadLetterJobService.findJobsByQueryCriteria(new DeadLetterJobQueryImpl(commandContext, jobServiceConfiguration));
            for (Job job : jobs) {
                deadLetterJobService.delete(job.getId());
            }
            return null;
        });
    }

}
