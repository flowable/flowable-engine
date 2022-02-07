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
 * @author Tijs Rademakers
 */
public interface TimerJobQuery extends BaseJobQuery<TimerJobQuery, Job> {

    /**
     * Only select jobs which are executable, ie. duedate is null or duedate is in the past
     **/
    TimerJobQuery executable();

    /**
     * Select jobs which have given job handler type
     */
    @Override
    TimerJobQuery handlerType(String handlerType);

    /**
     * Only select jobs that are timers. Cannot be used together with {@link #messages()}
     */
    TimerJobQuery timers();

    /**
     * Only select jobs that are messages. Cannot be used together with {@link #timers()}
     */
    TimerJobQuery messages();

}
