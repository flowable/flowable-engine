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

package org.flowable.engine.impl.test;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryLevel;

/**
 * @author Tijs Rademakers
 */

public class HistoryTestHelper {

    public static boolean isHistoryLevelAtLeast(HistoryLevel historyLevel, ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(historyLevel)) {
            if (processEngineConfiguration.isAsyncHistoryEnabled()) {
                JobTestHelper.waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(processEngineConfiguration, 
                                processEngineConfiguration.getManagementService(), 5000, 200);
            }
            
            return true;
        }
        
        return false;
    }
}
