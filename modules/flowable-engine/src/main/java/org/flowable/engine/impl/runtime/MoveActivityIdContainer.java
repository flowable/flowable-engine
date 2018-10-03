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
public class MoveActivityIdContainer {

    protected List<String> activityIds;
    protected List<String> moveToActivityIds;
    protected boolean moveToParentProcess;
    protected boolean moveToSubProcessInstance;
    protected String callActivityId;

    public MoveActivityIdContainer(String singleActivityId, String moveToActivityId) {
        this.activityIds = Collections.singletonList(singleActivityId);
        this.moveToActivityIds = Collections.singletonList(moveToActivityId);
    }

    public MoveActivityIdContainer(List<String> activityIds, String moveToActivityId) {
        this.activityIds = activityIds;
        this.moveToActivityIds = Collections.singletonList(moveToActivityId);
    }

    public MoveActivityIdContainer(String singleActivityId, List<String> moveToActivityIds) {
        this.activityIds = Collections.singletonList(singleActivityId);
        this.moveToActivityIds = moveToActivityIds;
    }

    public List<String> getActivityIds() {
        return Optional.ofNullable(activityIds).orElse(Collections.emptyList());
    }

    public List<String> getMoveToActivityIds() {
        return Optional.ofNullable(moveToActivityIds).orElse(Collections.emptyList());
    }

    public boolean isMoveToParentProcess() {
        return moveToParentProcess;
    }

    public void setMoveToParentProcess(boolean moveToParentProcess) {
        this.moveToParentProcess = moveToParentProcess;
    }

    public boolean isMoveToSubProcessInstance() {
        return moveToSubProcessInstance;
    }

    public void setMoveToSubProcessInstance(boolean moveToSubProcessInstance) {
        this.moveToSubProcessInstance = moveToSubProcessInstance;
    }

    public String getCallActivityId() {
        return callActivityId;
    }

    public void setCallActivityId(String callActivityId) {
        this.callActivityId = callActivityId;
    }
}
