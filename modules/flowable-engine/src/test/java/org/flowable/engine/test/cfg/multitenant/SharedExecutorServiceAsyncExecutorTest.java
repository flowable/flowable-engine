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
package org.flowable.engine.test.cfg.multitenant;

import org.flowable.common.engine.impl.cfg.multitenant.TenantInfoHolder;
import org.flowable.job.service.impl.asyncexecutor.multitenant.SharedExecutorServiceAsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.multitenant.TenantAwareAcquireAsyncJobsDueRunnable;
import org.flowable.job.service.impl.asyncexecutor.multitenant.TenantAwareAcquireTimerJobsRunnable;
import org.flowable.job.service.impl.asyncexecutor.multitenant.TenantAwareResetExpiredJobsRunnable;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertTrue;

public class SharedExecutorServiceAsyncExecutorTest {

    private TestSharedExecutorServiceAsyncExecutor sharedExecutorServiceAsyncExecutor;
    private DummyTenantInfoHolder tenantInfoHolder;

    @Before
    public void setUp() {
        tenantInfoHolder = new DummyTenantInfoHolder();
        sharedExecutorServiceAsyncExecutor = new TestSharedExecutorServiceAsyncExecutor(tenantInfoHolder);
    }

    @Test
    public void testConcurrentHashMaps() {
        assertConcurrentHashMap(sharedExecutorServiceAsyncExecutor.getTimerJobAcquisitionThreads());
        assertConcurrentHashMap(sharedExecutorServiceAsyncExecutor.getTimerJobAcquisitionRunnables());
        assertConcurrentHashMap(sharedExecutorServiceAsyncExecutor.getAsyncJobAcquisitionThreads());
        assertConcurrentHashMap(sharedExecutorServiceAsyncExecutor.getAsyncJobAcquisitionRunnables());
        assertConcurrentHashMap(sharedExecutorServiceAsyncExecutor.getResetExpiredJobsThreads());
        assertConcurrentHashMap(sharedExecutorServiceAsyncExecutor.getResetExpiredJobsRunnables());
    }

    private void assertConcurrentHashMap(Object o) {
        assertTrue("Map should be a concurrentHashMap", (o instanceof ConcurrentHashMap));
    }

    private class TestSharedExecutorServiceAsyncExecutor extends SharedExecutorServiceAsyncExecutor {

        public TestSharedExecutorServiceAsyncExecutor(TenantInfoHolder tenantInfoHolder) {
            super(tenantInfoHolder);
        }

        private Map<String, Thread> getTimerJobAcquisitionThreads() {
            return timerJobAcquisitionThreads;
        }

        private Map<String, TenantAwareAcquireTimerJobsRunnable> getTimerJobAcquisitionRunnables() {
            return timerJobAcquisitionRunnables;
        }

        private Map<String, Thread> getAsyncJobAcquisitionThreads() {
            return asyncJobAcquisitionThreads;
        }

        private Map<String, TenantAwareAcquireAsyncJobsDueRunnable> getAsyncJobAcquisitionRunnables() {
            return asyncJobAcquisitionRunnables;
        }

        private Map<String, Thread> getResetExpiredJobsThreads() {
            return resetExpiredJobsThreads;
        }

        private Map<String, TenantAwareResetExpiredJobsRunnable> getResetExpiredJobsRunnables() {
            return resetExpiredJobsRunnables;
        }
    }
}
