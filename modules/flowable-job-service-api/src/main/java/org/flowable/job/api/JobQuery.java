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

package org.flowable.job.api;

/**
 * Allows programmatic querying of {@link Job}s.
 *
 * @author Joram Barrez
 * @author Falko Menge
 */
public interface JobQuery extends BaseJobQuery<JobQuery, Job> {

    /**
     * Only select jobs that are timers. Cannot be used together with {@link #messages()}
     */
    JobQuery timers();

    /**
     * Only select jobs that are messages. Cannot be used together with {@link #timers()}
     */
    JobQuery messages();

    /**
     * Only return jobs with the given lock owner.
     */
    JobQuery lockOwner(String lockOwner);

    /**
     * Only return jobs that are locked (i.e. they are acquired by an executor).
     */
    JobQuery locked();

    /**
     * Only return jobs that are not locked.
     */
    JobQuery unlocked();

}
