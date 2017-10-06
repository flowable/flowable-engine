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
package org.flowable.job.service.impl.persistence.entity;

import java.util.Date;
import java.util.Map;

/**
 * TimerJob entity, necessary for persistence.
 *
 * @author Tijs Rademakers
 */
public class TimerJobEntityImpl extends AbstractJobEntityImpl implements TimerJobEntity {

    private static final long serialVersionUID = 1L;

    protected String lockOwner;
    protected Date lockExpirationTime;

    @SuppressWarnings("unchecked")
    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = (Map<String, Object>) super.getPersistentState();
        persistentState.put("lockOwner", lockOwner);
        persistentState.put("lockExpirationTime", lockExpirationTime);

        return persistentState;
    }

    // getters and setters ////////////////////////////////////////////////////////

    @Override
    public String getLockOwner() {
        return lockOwner;
    }

    @Override
    public void setLockOwner(String claimedBy) {
        this.lockOwner = claimedBy;
    }

    @Override
    public Date getLockExpirationTime() {
        return lockExpirationTime;
    }

    @Override
    public void setLockExpirationTime(Date claimedUntil) {
        this.lockExpirationTime = claimedUntil;
    }

    @Override
    public String toString() {
        return "TimerJobEntity [id=" + id + "]";
    }

}
