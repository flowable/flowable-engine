package org.flowable.engine.test.bpmn.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Fabio Filippelli
 */
public class AsyncJobCategoryTest extends PluggableFlowableTestCase {

    private final static String PROCESS_DEF_KEY = "asyncTask";
    private final static String MATCHING_CATEGORY = "testCategory";
    private final static String NON_MATCHING_CATEGORY = "nonMatchingCategory";

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testNonMatchingCategory_asyncExecutorOff() {
        try {
            processEngineConfiguration.setAsyncExecutorActivate(false);
            processEngineConfiguration.getAsyncExecutor().shutdown();
            processEngineConfiguration.addEnabledJobCategory(NON_MATCHING_CATEGORY);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(processEngineConfiguration.getEnabledJobCategories());

            runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY);

            assertThatThrownBy(() -> waitForJobExecutorToProcessAllJobs(4000L, 200L))
                    .isInstanceOf(FlowableException.class);

            // job is not executed because of different job category value
            assertThat(managementService.createJobQuery().count()).isEqualTo(1);

            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);
            waitForJobExecutorToProcessAllJobs(4000L, 200L);

            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();
        } finally {
            processEngineConfiguration.setEnabledJobCategories(null);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testNonMatchingCategory_asyncExecutorOn() {
        try {
            processEngineConfiguration.getAsyncExecutor().start();
            processEngineConfiguration.setAsyncExecutorActivate(true);
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(NON_MATCHING_CATEGORY);

            runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY);

            assertThatThrownBy(() -> waitForJobExecutorToProcessAllJobs(4000L, 200L))
                    .isInstanceOf(FlowableException.class);

            // job is not executed because of different job category value
            assertThat(managementService.createJobQuery().count()).isEqualTo(1);

            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);
            waitForJobExecutorToProcessAllJobs(4000L, 200L);

            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();
        } finally {
            processEngineConfiguration.setEnabledJobCategories(null);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testMatchingCategory_asyncExecutorOff() {
        try {
            processEngineConfiguration.setAsyncExecutorActivate(false);
            processEngineConfiguration.getAsyncExecutor().shutdown();
            processEngineConfiguration.addEnabledJobCategory(MATCHING_CATEGORY);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(processEngineConfiguration.getEnabledJobCategories());

            runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY);

            waitForJobExecutorToProcessAllJobs(4000L, 200L);

            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();
        } finally {
            processEngineConfiguration.setEnabledJobCategories(null);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testMatchingCategory_asyncExecutorOn() {
        try {
            processEngineConfiguration.getAsyncExecutor().start();
            processEngineConfiguration.setAsyncExecutorActivate(true);
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);

            runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY);

            waitForJobExecutorToProcessAllJobs(4000L, 200L);

            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();
        } finally {
            processEngineConfiguration.setEnabledJobCategories(null);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTask.bpmn20.xml")
    public void testEmptyJobCategory_asyncExecutorOff() {
        try {
            processEngineConfiguration.setAsyncExecutorActivate(false);
            processEngineConfiguration.getAsyncExecutor().shutdown();
            processEngineConfiguration.addEnabledJobCategory(NON_MATCHING_CATEGORY);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(processEngineConfiguration.getEnabledJobCategories());

            runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY);

            assertThatThrownBy(() -> waitForJobExecutorToProcessAllJobs(4000L, 200L))
                    .isInstanceOf(FlowableException.class);

            // the job is not done
            assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        } finally {
            processEngineConfiguration.setEnabledJobCategories(null);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTask.bpmn20.xml")
    public void testEmptyJobCategory_asyncExecutorOn() {
        try {
            processEngineConfiguration.getAsyncExecutor().start();
            processEngineConfiguration.setAsyncExecutorActivate(true);
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);

            runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY);

            assertThatThrownBy(() -> waitForJobExecutorToProcessAllJobs(4000L, 200L))
                    .isInstanceOf(FlowableException.class);

            // the job is not done
            assertThat(managementService.createJobQuery().count()).isEqualTo(1);
        } finally {
            processEngineConfiguration.setEnabledJobCategories(null);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testEmptyEnabledCategory_asyncExecutorOff() {
        try {
            processEngineConfiguration.setAsyncExecutorActivate(false);
            processEngineConfiguration.getAsyncExecutor().shutdown();
            processEngineConfiguration.setEnabledJobCategories(null);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(processEngineConfiguration.getEnabledJobCategories());

            runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY);

            waitForJobExecutorToProcessAllJobs(4000L, 200L);

            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();
        } finally {
            processEngineConfiguration.setEnabledJobCategories(null);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testEmptyEnabledCategory_asyncExecutorOn() {
        try {
            processEngineConfiguration.getAsyncExecutor().start();
            processEngineConfiguration.setAsyncExecutorActivate(true);
            processEngineConfiguration.setEnabledJobCategories(null);

            runtimeService.startProcessInstanceByKey(PROCESS_DEF_KEY);

            waitForJobExecutorToProcessAllJobs(4000L, 200L);

            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();
        } finally {
            processEngineConfiguration.setEnabledJobCategories(null);
            processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(null);
        }
    }

}
