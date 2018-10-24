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
package org.flowable.engine.impl.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Tijs Rademakers
 */
public class MoveExecutionIdContainer {

    protected List<String> executionIds;
    protected List<String> moveToActivityIds;
    protected Optional<String> newAssigneeId;

    public MoveExecutionIdContainer(String singleExecutionId, String moveToActivityId) {
        this(singleExecutionId, moveToActivityId, null);
    }

    public MoveExecutionIdContainer(String singleExecutionId, String moveToActivityId, String newAssigneeId) {
        this.executionIds = Collections.singletonList(singleExecutionId);
        this.moveToActivityIds = Collections.singletonList(moveToActivityId);
        this.newAssigneeId = Optional.ofNullable(newAssigneeId);
    }

    public MoveExecutionIdContainer(List<String> executionIds, String moveToActivityId) {
        this(executionIds, moveToActivityId, null);

    }

    public MoveExecutionIdContainer(List<String> executionIds, String moveToActivityId, String newAssigneeId) {
        this.executionIds = executionIds;
        this.moveToActivityIds = Collections.singletonList(moveToActivityId);
        this.newAssigneeId = Optional.ofNullable(newAssigneeId);
    }

    public MoveExecutionIdContainer(String singleExecutionId, List<String> moveToActivityIds) {
        this(singleExecutionId, moveToActivityIds, null);
    }

    public MoveExecutionIdContainer(String singleExecutionId, List<String> moveToActivityIds, String newAssigneeId) {
        this.executionIds = Collections.singletonList(singleExecutionId);
        this.moveToActivityIds = moveToActivityIds;
        this.newAssigneeId = Optional.ofNullable(newAssigneeId);
    }

    public List<String> getExecutionIds() {
        return Optional.ofNullable(executionIds).orElse(Collections.emptyList());
    }

    public List<String> getMoveToActivityIds() {
        return Optional.ofNullable(moveToActivityIds).orElse(Collections.emptyList());
    }

    public Optional<String> getNewAssigneeId() {
        return newAssigneeId;
    }
}
