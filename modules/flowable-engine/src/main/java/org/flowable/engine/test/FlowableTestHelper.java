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
package org.flowable.engine.test;

import java.time.Instant;
import java.util.Date;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.impl.test.JobTestHelper;
import org.flowable.engine.test.mock.FlowableMockSupport;

/**
 * A Helper for the Flowable {@link FlowableExtension} that can be used within the JUnit Jupiter context store
 * and users can use it in the tests for easy modifying of the {@link ProcessEngine} time and easy access for waiting on the job executor.
 *
 * @author Filip Hrisafov
 */
public class FlowableTestHelper {

    protected final ProcessEngine processEngine;
    protected final FlowableMockSupport mockSupport;
    protected String deploymentIdFromDeploymentAnnotation;

    public FlowableTestHelper(ProcessEngine processEngine) {
        this.processEngine = processEngine;
        if (FlowableMockSupport.isMockSupportPossible(this.processEngine)) {
            this.mockSupport = new FlowableMockSupport(this.processEngine);
        } else {
            this.mockSupport = null;
        }
    }

    public ProcessEngine getProcessEngine() {
        return processEngine;
    }

    public String getDeploymentIdFromDeploymentAnnotation() {
        return deploymentIdFromDeploymentAnnotation;
    }

    public void setDeploymentIdFromDeploymentAnnotation(String deploymentIdFromDeploymentAnnotation) {
        this.deploymentIdFromDeploymentAnnotation = deploymentIdFromDeploymentAnnotation;
    }

    public FlowableMockSupport getMockSupport() {
        return mockSupport;
    }

    public void waitForJobExecutorToProcessAllJobs(long maxMillisToWait, long intervalMillis) {
        JobTestHelper
            .waitForJobExecutorToProcessAllJobs(processEngine.getProcessEngineConfiguration(), processEngine.getManagementService(), maxMillisToWait,
                intervalMillis);
    }

    public void setCurrentTime(Date date) {
        processEngine.getProcessEngineConfiguration().getClock().setCurrentTime(date);
    }

    public void setCurrentTime(Instant instant) {
        processEngine.getProcessEngineConfiguration().getClock().setCurrentTime(instant == null ? null : Date.from(instant));
    }

}
