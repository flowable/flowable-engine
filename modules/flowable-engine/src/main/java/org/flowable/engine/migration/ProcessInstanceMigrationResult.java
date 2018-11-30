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

package org.flowable.engine.migration;

import java.util.List;
import java.util.Optional;

/**
 * @author Dennis Federico
 */
public interface ProcessInstanceMigrationResult {

    String STATUS_COMPLETED = "Completed";
    String STATUS_IN_PROGRESS = "InProgress";

    Optional<String> getBatchId();

    /**
     * Returns true if this is the parent of many individual by process instance results.
     * In which case iterate over @see getParts() to get the individual results
     *
     * @return
     */
    boolean isParentResult();

    String getStatus();

    Optional<String> getProcessInstanceId();

    Optional<Result> getResult();

    List<ProcessInstanceMigrationResult> getParts();

    List<ProcessInstanceMigrationResult> getSuccessfulParts();

    List<ProcessInstanceMigrationResult> getFailedParts();

    List<ProcessInstanceMigrationResult> getInProgressParts();

    List<ProcessInstanceMigrationResult> getCompletedParts();

    long getPartsCount();

    long getSuccessfulPartsCount();

    long getFailedPartsCount();

    long getInProgressPartsCount();

    long getCompletedPartsCount();

    //getAsJson

    interface Result {

        String RESULT_SUCCESSFUL = "Successful";
        String RESULT_FAILED = "Failed";

        String getStatus();

        Optional<String> getMessage();
    }
}
