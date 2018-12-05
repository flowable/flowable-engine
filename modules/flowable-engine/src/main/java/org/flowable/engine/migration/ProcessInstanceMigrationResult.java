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
import java.util.function.Predicate;

/**
 * @author Dennis Federico
 */
public interface ProcessInstanceMigrationResult<T> {

    Predicate<ProcessInstanceMigrationResult<?>> isParentResult = result -> result.getParts() != null && !result.getParts().isEmpty();
    Predicate<ProcessInstanceMigrationResult<?>> isSuccessfulResultPredicate = result -> result.getResultStatus().filter(ProcessInstanceMigrationResult.RESULT_SUCCESSFUL::equals).isPresent();
    Predicate<ProcessInstanceMigrationResult<?>> isFailedResultPredicate = result -> result.getResultStatus().filter(ProcessInstanceMigrationResult.RESULT_FAILED::equals).isPresent();
    Predicate<ProcessInstanceMigrationResult<?>> isCompletedResultPredicate = result -> ProcessInstanceMigrationResult.STATUS_COMPLETED.equals(result.getStatus());
    Predicate<ProcessInstanceMigrationResult<?>> isInProgressResultPredicate = result -> ProcessInstanceMigrationResult.STATUS_IN_PROGRESS.equals(result.getStatus());

    String STATUS_COMPLETED = "Completed";
    String STATUS_IN_PROGRESS = "InProgress";
    String RESULT_SUCCESSFUL = "Successful";
    String RESULT_FAILED = "Failed";

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

    Optional<String> getSourceProcessDefinitionId();

    Optional<String> getTargetProcessDefinitionId();

    Optional<String> getResultStatus();

    Optional<T> getResultValue();

    List<ProcessInstanceMigrationResult<T>> getParts();

    List<ProcessInstanceMigrationResult<T>> getSuccessfulParts();

    List<ProcessInstanceMigrationResult<T>> getFailedParts();

    List<ProcessInstanceMigrationResult<T>> getInProgressParts();

    List<ProcessInstanceMigrationResult<T>> getCompletedParts();

    long getPartsCount();

    long getSuccessfulPartsCount();

    long getFailedPartsCount();

    long getInProgressPartsCount();

    long getCompletedPartsCount();

    boolean isSuccessful();

    boolean isFailed();

    boolean isInProgress();

    boolean isCompleted();
}
