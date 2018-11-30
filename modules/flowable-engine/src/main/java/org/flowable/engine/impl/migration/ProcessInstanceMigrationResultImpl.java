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

package org.flowable.engine.impl.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.flowable.engine.migration.ProcessInstanceMigrationResult;

/**
 * @author Dennis Federico
 */
public class ProcessInstanceMigrationResultImpl implements ProcessInstanceMigrationResult {

    protected static Predicate<ProcessInstanceMigrationResult> isSuccessful = result -> result.getResult().map(Result::getStatus).filter(Result.RESULT_SUCCESSFUL::equals).isPresent();
    protected static Predicate<ProcessInstanceMigrationResult> isFailed = result -> result.getResult().map(Result::getStatus).filter(Result.RESULT_FAILED::equals).isPresent();
    protected static Predicate<ProcessInstanceMigrationResult> isCompleted = result -> STATUS_COMPLETED.equals(result.getStatus());
    protected static Predicate<ProcessInstanceMigrationResult> isInProgress = result -> STATUS_IN_PROGRESS.equals(result.getStatus());

    //TODO Timestamps?
    protected String batchId;
    protected String status;
    protected String processInstanceId;
    protected Result result;
    protected List<ProcessInstanceMigrationResult> resultParts = new ArrayList<>();

    public Optional<String> getBatchId() {
        return Optional.ofNullable(batchId);
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    /**
     * Returns true if this is the parent of many individual by process instance results.
     * In which case iterate over @see getParts() to get the individual results
     *
     * @return
     */
    public boolean isParentResult() {
        return resultParts != null && !resultParts.isEmpty();
    }

    public String getStatus() {
        if (isParentResult()) {
            Optional<String> partInProgress = resultParts.stream()
                .map(ProcessInstanceMigrationResult::getStatus)
                .filter(STATUS_IN_PROGRESS::equals)
                .findFirst();
            return partInProgress.orElse(STATUS_COMPLETED);
        } else {
            return status;
        }
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Optional<String> getProcessInstanceId() {
        return Optional.ofNullable(processInstanceId);
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public Optional<Result> getResult() {
        return Optional.ofNullable(result);
    }

    public void setResult(String status, String message) {
        this.result = new ResultImpl(status, message);
    }

    public List<ProcessInstanceMigrationResult> getParts() {
        return resultParts;
    }

    public void addResultPart(ProcessInstanceMigrationResult resultPart) {
        if (resultParts == null) {
            this.resultParts = new ArrayList<>();
        }
        resultParts.add(resultPart);
    }

    public List<ProcessInstanceMigrationResult> getSuccessfulParts() {
        return getFilteredParts(isSuccessful);
    }

    public List<ProcessInstanceMigrationResult> getFailedParts() {
        return getFilteredParts(isFailed);
    }

    public List<ProcessInstanceMigrationResult> getInProgressParts() {
        return getFilteredParts(isInProgress);
    }

    public List<ProcessInstanceMigrationResult> getCompletedParts() {
        return getFilteredParts(isCompleted);
    }

    @Override
    public long getPartsCount() {
        if (isParentResult()) {
            return resultParts.size();
        }
        return 0;
    }

    public long getSuccessfulPartsCount() {
        return getFilteredPartsCount(isSuccessful);
    }

    public long getFailedPartsCount() {
        return getFilteredPartsCount(isFailed);
    }

    public long getInProgressPartsCount() {
        return getFilteredPartsCount(isInProgress);
    }

    public long getCompletedPartsCount() {
        return getFilteredPartsCount(isCompleted);
    }

    protected List<ProcessInstanceMigrationResult> getFilteredParts(Predicate<ProcessInstanceMigrationResult> predicate) {
        if (isParentResult()) {
            return resultParts.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        }
        return null;
    }

    protected long getFilteredPartsCount(Predicate<ProcessInstanceMigrationResult> predicate) {
        if (isParentResult()) {
            return resultParts.stream()
                .filter(predicate)
                .count();
        }
        return 0;
    }

    //getAsJson

    public static class ResultImpl implements Result {

        protected String status;
        protected String message;

        public ResultImpl(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() {
            return status;
        }

        public Optional<String> getMessage() {
            return Optional.ofNullable(message);
        }
    }
}
