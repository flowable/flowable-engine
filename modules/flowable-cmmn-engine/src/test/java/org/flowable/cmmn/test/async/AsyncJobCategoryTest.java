package org.flowable.cmmn.test.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnJobTestHelper;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Fabio Filippelli
 */
public class AsyncJobCategoryTest extends FlowableCmmnTestCase {

    private final static String CASE_DEF_KEY = "testAsyncServiceTask";
    private final static String MATCHING_CATEGORY = "cmmnCategory";
    private final static String NON_MATCHING_CATEGORY = "nonMatchingCategory";

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testNonMatchingCategory_asyncExecutorOff() {
        try {
            cmmnEngineConfiguration.setAsyncExecutorActivate(false);
            cmmnEngineConfiguration.getAsyncExecutor().shutdown();
            cmmnEngineConfiguration.addEnabledJobCategory(NON_MATCHING_CATEGORY);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(cmmnEngineConfiguration.getEnabledJobCategories());

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(CASE_DEF_KEY).start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            assertThatThrownBy(() -> CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, true))
                    .isInstanceOf(FlowableException.class);

            // job is not executed because of different job category value
            assertThat(cmmnManagementService.createJobQuery().count()).isEqualTo(1);

            cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);
            CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, true);

            // the job is done
            assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        } finally {
            cmmnEngineConfiguration.setEnabledJobCategories(null);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testNonMatchingCategory_asyncExecutorOn() {
        try {
            cmmnEngineConfiguration.getAsyncExecutor().start();
            cmmnEngineConfiguration.setAsyncExecutorActivate(true);
            cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(NON_MATCHING_CATEGORY);

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(CASE_DEF_KEY).start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            assertThatThrownBy(() -> CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, true))
                    .isInstanceOf(FlowableException.class);

            // job is not executed because of different job category value
            assertThat(cmmnManagementService.createJobQuery().count()).isEqualTo(1);

            cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);
            CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, true);

            // the job is done
            assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        } finally {
            cmmnEngineConfiguration.setEnabledJobCategories(null);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testMatchingCategory_asyncExecutorOff() {
        try {
            cmmnEngineConfiguration.setAsyncExecutorActivate(false);
            cmmnEngineConfiguration.getAsyncExecutor().shutdown();
            cmmnEngineConfiguration.addEnabledJobCategory(MATCHING_CATEGORY);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(cmmnEngineConfiguration.getEnabledJobCategories());

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(CASE_DEF_KEY).start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, true);

            // the job is done
            assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        } finally {
            cmmnEngineConfiguration.setEnabledJobCategories(null);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testMatchingCategory_asyncExecutorOn() {
        try {
            cmmnEngineConfiguration.getAsyncExecutor().start();
            cmmnEngineConfiguration.setAsyncExecutorActivate(true);
            cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(CASE_DEF_KEY).start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, true);

            // the job is done
            assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        } finally {
            cmmnEngineConfiguration.setEnabledJobCategories(null);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTask.cmmn")
    public void testEmptyJobCategory_asyncExecutorOff() {
        try {
            cmmnEngineConfiguration.setAsyncExecutorActivate(false);
            cmmnEngineConfiguration.getAsyncExecutor().shutdown();
            cmmnEngineConfiguration.addEnabledJobCategory(NON_MATCHING_CATEGORY);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(cmmnEngineConfiguration.getEnabledJobCategories());

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(CASE_DEF_KEY).start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            assertThatThrownBy(() -> CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, true))
                    .isInstanceOf(FlowableException.class);

            // the job is not done
            assertThat(cmmnManagementService.createJobQuery().count()).isEqualTo(1);
        } finally {
            cmmnEngineConfiguration.setEnabledJobCategories(null);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTask.cmmn")
    public void testEmptyJobCategory_asyncExecutorOn() {
        try {
            cmmnEngineConfiguration.getAsyncExecutor().start();
            cmmnEngineConfiguration.setAsyncExecutorActivate(true);
            cmmnEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(CASE_DEF_KEY).start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            assertThatThrownBy(() -> CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, true))
                    .isInstanceOf(FlowableException.class);

            // the job is not done
            assertThat(cmmnManagementService.createJobQuery().count()).isEqualTo(1);
        } finally {
            cmmnEngineConfiguration.setEnabledJobCategories(null);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testEmptyEnabledCategory_asyncExecutorOff() {
        try {
            cmmnEngineConfiguration.setAsyncExecutorActivate(false);
            cmmnEngineConfiguration.getAsyncExecutor().shutdown();
            cmmnEngineConfiguration.setEnabledJobCategories(null);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(cmmnEngineConfiguration.getEnabledJobCategories());

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(CASE_DEF_KEY).start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, true);

            // the job is done
            assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        } finally {
            cmmnEngineConfiguration.setEnabledJobCategories(null);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/async/AsyncTaskTest.testAsyncServiceTaskWithCategory.cmmn")
    public void testEmptyEnabledCategory_asyncExecutorOn() {
        try {
            cmmnEngineConfiguration.getAsyncExecutor().start();
            cmmnEngineConfiguration.setAsyncExecutorActivate(true);
            cmmnEngineConfiguration.setEnabledJobCategories(null);

            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey(CASE_DEF_KEY).start();
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());

            CmmnJobTestHelper.waitForJobExecutorToProcessAllJobs(cmmnEngineConfiguration, 4000L, 200L, true);

            // the job is done
            assertThat(cmmnManagementService.createJobQuery().count()).isZero();
        } finally {
            cmmnEngineConfiguration.setEnabledJobCategories(null);
            cmmnEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

}
