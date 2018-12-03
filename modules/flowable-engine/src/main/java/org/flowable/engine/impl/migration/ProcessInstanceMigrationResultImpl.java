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
public class ProcessInstanceMigrationResultImpl<T> implements ProcessInstanceMigrationResult<T> {

    //TODO Timestamps?
    protected String batchId;
    protected String status = ProcessInstanceMigrationResult.STATUS_IN_PROGRESS;
    protected String processInstanceId;
    protected String resultStatus;
    protected T resultValue;
    protected List<ProcessInstanceMigrationResult<T>> resultParts = new ArrayList<>();

    @Override
    public Optional<String> getBatchId() {
        return Optional.ofNullable(batchId);
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    @Override
    public boolean isParentResult() {
        return isParentResult.test(this);
    }

    @Override
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

    @Override
    public Optional<String> getProcessInstanceId() {
        return Optional.ofNullable(processInstanceId);
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public ProcessInstanceMigrationResult<T> setResult(String resultStatus, T resultValue) {
        this.status = ProcessInstanceMigrationResult.STATUS_COMPLETED;
        this.resultStatus = resultStatus;
        this.resultValue = resultValue;
        return this;
    }

    @Override
    public Optional<String> getResultStatus() {
        return Optional.ofNullable(resultStatus);
    }

    @Override
    public Optional<T> getResultValue() {
        return Optional.ofNullable(resultValue);
    }

    @Override
    public List<ProcessInstanceMigrationResult<T>> getParts() {
        return resultParts;
    }

    public void addResultPart(ProcessInstanceMigrationResult<T> resultPart) {
        if (resultParts == null) {
            this.resultParts = new ArrayList<>();
        }
        resultParts.add(resultPart);
    }

    @Override
    public List<ProcessInstanceMigrationResult<T>> getSuccessfulParts() {
        return getFilteredParts(isSuccessfulResultPredicate);
    }

    @Override
    public List<ProcessInstanceMigrationResult<T>> getFailedParts() {
        return getFilteredParts(isFailedResultPredicate);
    }

    @Override
    public List<ProcessInstanceMigrationResult<T>> getInProgressParts() {
        return getFilteredParts(isInProgressResultPredicate);
    }

    @Override
    public List<ProcessInstanceMigrationResult<T>> getCompletedParts() {
        return getFilteredParts(isCompletedResultPredicate);
    }

    @Override
    public long getPartsCount() {
        if (isParentResult()) {
            return resultParts.size();
        }
        return 0;
    }

    @Override
    public long getSuccessfulPartsCount() {
        return getFilteredPartsCount(isSuccessfulResultPredicate);
    }

    @Override
    public long getFailedPartsCount() {
        return getFilteredPartsCount(isFailedResultPredicate);
    }

    @Override
    public long getInProgressPartsCount() {
        return getFilteredPartsCount(isInProgressResultPredicate);
    }

    @Override
    public long getCompletedPartsCount() {
        return getFilteredPartsCount(isCompletedResultPredicate);
    }

    @Override
    public boolean isSuccessful() {
        return testResultOrChildrenPredicate(isSuccessfulResultPredicate);
    }

    @Override
    public boolean isFailed() {
        return testResultOrChildrenPredicate(isFailedResultPredicate);
    }

    @Override
    public boolean isInProgress() {
        return testResultOrChildrenPredicate(isInProgressResultPredicate);
    }

    @Override
    public boolean isCompleted() {
        return testResultOrChildrenPredicate(isCompletedResultPredicate);
    }

    protected boolean testResultOrChildrenPredicate(Predicate<ProcessInstanceMigrationResult<?>> predicate) {
        if (isParentResult.test(this)) {
            return this.getParts().stream().allMatch(predicate);
        } else {
            return predicate.test(this);
        }
    }

    protected List<ProcessInstanceMigrationResult<T>> getFilteredParts(Predicate<ProcessInstanceMigrationResult<?>> predicate) {
        if (isParentResult()) {
            return resultParts.stream()
                .filter(predicate)
                .collect(Collectors.toList());
        }
        return null;
    }

    protected long getFilteredPartsCount(Predicate<ProcessInstanceMigrationResult<?>> predicate) {
        if (isParentResult()) {
            return resultParts.stream()
                .filter(predicate)
                .count();
        }
        return 0;
    }
}
