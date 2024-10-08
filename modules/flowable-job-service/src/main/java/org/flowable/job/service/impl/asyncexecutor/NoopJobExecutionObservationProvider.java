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

import org.flowable.job.api.JobInfo;

/**
 * @author Filip Hrisafov
 */
public class NoopJobExecutionObservationProvider implements JobExecutionObservationProvider {

    @Override
    public JobExecutionObservation create(JobInfo job) {
        return JobExecutionObservation.NOOP;
    }

    static class NoopJobExecutionObservation implements JobExecutionObservation {

        @Override
        public void start() {
            // Do nothing
        }

        @Override
        public void stop() {
            // Do nothing
        }

        @Override
        public Scope lockScope() {
            return NoopScope.INSTANCE;
        }

        @Override
        public void lockError(Throwable lockException) {
            // Do nothing
        }

        @Override
        public Scope executionScope() {
            return NoopScope.INSTANCE;
        }

        @Override
        public void executionError(Throwable exception) {
            // Do nothing
        }
    }

    static class NoopScope implements JobExecutionObservation.Scope {

        static final NoopScope INSTANCE = new NoopScope();

        @Override
        public void close() {
            // Do nothing
        }
    }

}
