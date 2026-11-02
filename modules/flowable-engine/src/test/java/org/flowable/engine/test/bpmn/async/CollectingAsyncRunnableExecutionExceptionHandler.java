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

import java.util.ArrayList;
import java.util.Collection;

import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncRunnableExecutionExceptionHandler;

/**
 * @author Filip Hrisafov
 */
public class CollectingAsyncRunnableExecutionExceptionHandler implements AsyncRunnableExecutionExceptionHandler {

    protected final Collection<Throwable> exceptions = new ArrayList<>();

    @Override
    public boolean handleException(JobServiceConfiguration jobServiceConfiguration, JobInfo job, Throwable exception) {
        exceptions.add(exception);
        return false;
    }

    public Collection<Throwable> getExceptions() {
        return exceptions;
    }
}
