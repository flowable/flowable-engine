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
package org.flowable.cmmn.test.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.task.api.Task;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Fabio Filippelli
 */
public class AsyncJobCategoryTest extends FlowableCmmnTestCase {

    private static final String MATCHING_CATEGORY = "cmmnCategory";
    private static final String NON_MATCHING_CATEGORY = "nonMatchingCategory";

    protected List<String> originalEnabledJobCategories;

    @Before
    public void setUp() {
        originalEnabledJobCategories = cmmnEngineConfiguration.getEnabledJobCategories();
        if (originalEnabledJobCategories != null) {
            originalEnabledJobCategories = new ArrayList<>(originalEnabledJobCategories);
        }
    }

    @After
    public void tearDown() {
        cmmnEngineConfiguration.setEnabledJobCategories(originalEnabledJobCategories);
        cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(originalEnabledJobCategories);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testNonMatchingCategory_asyncExecutorOff() {
        cmmnEngineConfiguration.addEnabledJobCategory(NON_MATCHING_CATEGORY);
        cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(cmmnEngineConfiguration.getEnabledJobCategories());

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        Job job = cmmnManagementService.createJobQuery().singleResult();
        assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
            assertThat(jobEntity.getLockOwner()).isNull();
            assertThat(jobEntity.getLockExpirationTime()).isNull();
        });

        cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);
        waitForJobExecutorToProcessAllJobs();

        // the job is done
        assertThat(cmmnManagementService.createJobQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testNonMatchingCategory_asyncExecutorOn() {
        runtWithEnabledJobExecutor(() -> {
            cmmnEngineConfiguration.addEnabledJobCategory(NON_MATCHING_CATEGORY);
            cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(NON_MATCHING_CATEGORY);

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            Job job = cmmnManagementService.createJobQuery().singleResult();
            assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
                assertThat(jobEntity.getLockOwner()).isNull();
                assertThat(jobEntity.getLockExpirationTime()).isNull();
            });

            cmmnEngineConfiguration.addEnabledJobCategory(MATCHING_CATEGORY);
            cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);
            CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, false);

            // the job is done
            assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        });
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testMatchingCategory_asyncExecutorOff() {
        cmmnEngineConfiguration.addEnabledJobCategory(MATCHING_CATEGORY);
        cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(cmmnEngineConfiguration.getEnabledJobCategories());

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        Job job = cmmnManagementService.createJobQuery().singleResult();
        assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
            assertThat(jobEntity.getLockOwner()).isNull();
            assertThat(jobEntity.getLockExpirationTime()).isNull();
        });

        waitForJobExecutorToProcessAllJobs();

        // the job is done
        assertThat(cmmnManagementService.createJobQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testMatchingCategory_asyncExecutorOn() {
        runtWithEnabledJobExecutor(() -> {
            cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            Job job = cmmnManagementService.createJobQuery().singleResult();

            if (job != null) {
                assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
                    assertThat(jobEntity.getLockOwner()).isNotNull();
                    assertThat(jobEntity.getLockExpirationTime()).isNotNull();
                });
            }

            CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, false);

            // the job is done
            assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        });
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTask.cmmn")
    public void testEmptyJobCategory_asyncExecutorOff() {
        cmmnEngineConfiguration.addEnabledJobCategory(NON_MATCHING_CATEGORY);
        cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(cmmnEngineConfiguration.getEnabledJobCategories());

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        Job job = cmmnManagementService.createJobQuery().singleResult();
        assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
            assertThat(jobEntity.getLockOwner()).isNull();
            assertThat(jobEntity.getLockExpirationTime()).isNull();
        });
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTask.cmmn")
    public void testEmptyJobCategory_asyncExecutorOn() {
        runtWithEnabledJobExecutor(() -> {
            cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            Job job = cmmnManagementService.createJobQuery().singleResult();
            assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
                assertThat(jobEntity.getLockOwner()).isNull();
                assertThat(jobEntity.getLockExpirationTime()).isNull();
            });
        });
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testEmptyEnabledCategory_asyncExecutorOff() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnTaskService.complete(task.getId());

        Job job = cmmnManagementService.createJobQuery().singleResult();
        assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
            assertThat(jobEntity.getLockOwner()).isNull();
            assertThat(jobEntity.getLockExpirationTime()).isNull();
        });

        waitForJobExecutorToProcessAllJobs();

        // the job is done
        assertThat(cmmnManagementService.createJobQuery().count()).isZero();
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testEmptyEnabledCategory_asyncExecutorOn() {
        runtWithEnabledJobExecutor(() -> {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testAsyncServiceTask").start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            Job job = cmmnManagementService.createJobQuery().singleResult();

            if (job != null) {
                assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
                    assertThat(jobEntity.getLockOwner()).isNotNull();
                    assertThat(jobEntity.getLockExpirationTime()).isNotNull();
                });
            }

            CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, false);

            // the job is done
            assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        });
    }

    protected void runtWithEnabledJobExecutor(Runnable runnable) {
        AsyncExecutor asyncExecutor = cmmnEngineConfiguration.getAsyncExecutor();
        try {
            asyncExecutor.start();
            cmmnEngineConfiguration.setAsyncExecutorActivate(true);
            runnable.run();
        } finally {
            asyncExecutor.shutdown();
            cmmnEngineConfiguration.setAsyncExecutorActivate(false);
        }
    }

}
