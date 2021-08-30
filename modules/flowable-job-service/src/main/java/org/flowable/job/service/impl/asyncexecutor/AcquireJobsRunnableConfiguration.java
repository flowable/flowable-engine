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
package org.flowable.job.service.impl.asyncexecutor;

import java.time.Duration;

/**
 * @author Filip Hrisafov
 */
public interface AcquireJobsRunnableConfiguration {

    AcquireJobsRunnableConfiguration DEFAULT = new AcquireJobsRunnableConfiguration() {

        private Duration lockWaitTime = Duration.ofMinutes(1);
        private Duration lockPollRate = Duration.ofMillis(500);
        private Duration lockForceAcquireAfter = Duration.ofMinutes(1);

        @Override
        public boolean isGlobalAcquireLockEnabled() {
            return false;
        }

        @Override
        public String getGlobalAcquireLockPrefix() {
            return "";
        }

        @Override
        public Duration getLockWaitTime() {
            return lockWaitTime;
        }

        @Override
        public Duration getLockPollRate() {
            return lockPollRate;
        }

        @Override
        public Duration getLockForceAcquireAfter() {
            return lockForceAcquireAfter;
        }
    };

    boolean isGlobalAcquireLockEnabled();

    String getGlobalAcquireLockPrefix();

    Duration getLockWaitTime();

    Duration getLockPollRate();

    Duration getLockForceAcquireAfter();
}
