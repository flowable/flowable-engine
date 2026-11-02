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
package org.flowable.engine.test.bpmn.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Fabio Filippelli
 */
public class AsyncJobCategoryTest extends PluggableFlowableTestCase {

    private static final String MATCHING_CATEGORY = "testCategory";
    private static final String NON_MATCHING_CATEGORY = "nonMatchingCategory";

    protected List<String> originalEnabledJobCategories;

    @BeforeEach
    void setUp() {
        originalEnabledJobCategories = processEngineConfiguration.getEnabledJobCategories();
        if (originalEnabledJobCategories != null) {
            originalEnabledJobCategories = new ArrayList<>(originalEnabledJobCategories);
        }
    }

    @AfterEach
    void tearDown() {
        processEngineConfiguration.setEnabledJobCategories(originalEnabledJobCategories);
        processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(originalEnabledJobCategories);
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testNonMatchingCategory_asyncExecutorOff() {
        processEngineConfiguration.addEnabledJobCategory(NON_MATCHING_CATEGORY);
        processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(processEngineConfiguration.getEnabledJobCategories());

        runtimeService.startProcessInstanceByKey("asyncTask");

        Job job = managementService.createJobQuery().singleResult();
        assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
            assertThat(jobEntity.getLockOwner()).isNull();
            assertThat(jobEntity.getLockExpirationTime()).isNull();
        });
        processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);
        waitForJobExecutorToProcessAllJobs(4000L, 200L);

        // the job is done
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testNonMatchingCategory_asyncExecutorOn() {
        runtWithEnabledJobExecutor(() -> {
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(NON_MATCHING_CATEGORY);

            runtimeService.startProcessInstanceByKey("asyncTask");

            Job job = managementService.createJobQuery().singleResult();
            assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
                assertThat(jobEntity.getLockOwner()).isNull();
                assertThat(jobEntity.getLockExpirationTime()).isNull();
            });

            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);
            JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 4000L, 200L, false);

            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();
        });
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testMatchingCategory_asyncExecutorOff() {
        processEngineConfiguration.addEnabledJobCategory(MATCHING_CATEGORY);
        processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(processEngineConfiguration.getEnabledJobCategories());

        runtimeService.startProcessInstanceByKey("asyncTask");

        Job job = managementService.createJobQuery().singleResult();
        assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
            assertThat(jobEntity.getLockOwner()).isNull();
            assertThat(jobEntity.getLockExpirationTime()).isNull();
        });

        waitForJobExecutorToProcessAllJobs(4000L, 200L);

        // the job is done
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testMatchingCategory_asyncExecutorOn() {
        runtWithEnabledJobExecutor(() -> {
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);

            runtimeService.startProcessInstanceByKey("asyncTask");

            Job job = managementService.createJobQuery().singleResult();

            if (job != null) {
                assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
                    assertThat(jobEntity.getLockOwner()).isNotNull();
                    assertThat(jobEntity.getLockExpirationTime()).isNotNull();
                });
            }

            JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 4000L, 200L, false);

            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();
        });
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTask.bpmn20.xml")
    public void testEmptyJobCategory_asyncExecutorOff() {
        processEngineConfiguration.addEnabledJobCategory(NON_MATCHING_CATEGORY);
        processEngineConfiguration.getJobServiceConfiguration().setEnabledJobCategories(processEngineConfiguration.getEnabledJobCategories());

        runtimeService.startProcessInstanceByKey("asyncTask");

        Job job = managementService.createJobQuery().singleResult();
        assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
            assertThat(jobEntity.getLockOwner()).isNull();
            assertThat(jobEntity.getLockExpirationTime()).isNull();
        });
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTask.bpmn20.xml")
    public void testEmptyJobCategory_asyncExecutorOn() {
        runtWithEnabledJobExecutor(() -> {
            processEngineConfiguration.getJobServiceConfiguration().addEnabledJobCategory(MATCHING_CATEGORY);

            runtimeService.startProcessInstanceByKey("asyncTask");

            Job job = managementService.createJobQuery().singleResult();
            assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
                assertThat(jobEntity.getLockOwner()).isNull();
                assertThat(jobEntity.getLockExpirationTime()).isNull();
            });
        });
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testEmptyEnabledCategory_asyncExecutorOff() {
        runtimeService.startProcessInstanceByKey("asyncTask");

        Job job = managementService.createJobQuery().singleResult();
        assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
            assertThat(jobEntity.getLockOwner()).isNull();
            assertThat(jobEntity.getLockExpirationTime()).isNull();
        });

        waitForJobExecutorToProcessAllJobs(4000L, 200L);

        // the job is done
        assertThat(managementService.createJobQuery().count()).isZero();
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/bpmn/async/AsyncTaskTest.testAsyncTaskWithJobCategory.bpmn20.xml")
    public void testEmptyEnabledCategory_asyncExecutorOn() {
        runtWithEnabledJobExecutor(() -> {
            runtimeService.startProcessInstanceByKey("asyncTask");

            Job job = managementService.createJobQuery().singleResult();

            if (job != null) {
                assertThat(job).isInstanceOfSatisfying(JobEntity.class, jobEntity -> {
                    assertThat(jobEntity.getLockOwner()).isNotNull();
                    assertThat(jobEntity.getLockExpirationTime()).isNotNull();
                });
            }

            JobTestHelper.waitForJobExecutorToProcessAllJobs(processEngineConfiguration, managementService, 4000L, 200L, false);

            // the job is done
            assertThat(managementService.createJobQuery().count()).isZero();

        });
    }

    protected void runtWithEnabledJobExecutor(Runnable runnable) {
        AsyncExecutor asyncExecutor = processEngineConfiguration.getAsyncExecutor();
        try {
            asyncExecutor.start();
            processEngineConfiguration.setAsyncExecutorActivate(true);
            runnable.run();
        } finally {
            asyncExecutor.shutdown();
            processEngineConfiguration.setAsyncExecutorActivate(false);
        }
    }

}
