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
package org.flowable.common.spring;

import java.time.Duration;

/**
 * @author Filip Hrisafov
 */
public class CommonAutoDeploymentProperties {

    protected boolean useLock;
    protected Duration lockWaitTime;
    protected boolean throwExceptionOnDeploymentFailure;
    protected String lockName;

    public CommonAutoDeploymentProperties() {

    }

    public CommonAutoDeploymentProperties(boolean useLock, Duration lockWaitTime, boolean throwExceptionOnDeploymentFailure) {
        this.useLock = useLock;
        this.lockWaitTime = lockWaitTime;
        this.throwExceptionOnDeploymentFailure = throwExceptionOnDeploymentFailure;
    }

    public boolean isUseLock() {
        return useLock;
    }

    public void setUseLock(boolean useLock) {
        this.useLock = useLock;
    }

    public Duration getLockWaitTime() {
        return lockWaitTime;
    }

    public void setLockWaitTime(Duration lockWaitTime) {
        this.lockWaitTime = lockWaitTime;
    }

    public boolean isThrowExceptionOnDeploymentFailure() {
        return throwExceptionOnDeploymentFailure;
    }

    public void setThrowExceptionOnDeploymentFailure(boolean throwExceptionOnDeploymentFailure) {
        this.throwExceptionOnDeploymentFailure = throwExceptionOnDeploymentFailure;
    }

    public String getLockName() {
        return lockName;
    }

    public void setLockName(String lockName) {
        this.lockName = lockName;
    }

}
