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
package org.flowable.spring.boot;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.spring.CommonAutoDeploymentProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.auto-deployment")
public class FlowableAutoDeploymentProperties {

    /**
     * Whether to use a lock when performing the auto deployment.
     */
    private boolean useLock = false;

    /**
     * Duration to wait for the auto deployment lock before giving up.
     */
    private Duration lockWaitTime = Duration.ofMinutes(5);

    /**
     * Whether to throw an exception if there was some kind of failure during the auto deployment.
     */
    private boolean throwExceptionOnDeploymentFailure = true;

    /**
     * Name of the lock that should be used for the auto deployment.
     * If not defined then the deployment name would be used.
     */
    private String lockName;

    /**
     * Engine specific deployment properties.
     */
    private Map<String, EngineDeploymentProperties> engine = new HashMap<>();

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

    public Map<String, EngineDeploymentProperties> getEngine() {
        return engine;
    }

    public void setEngine(Map<String, EngineDeploymentProperties> engine) {
        this.engine = engine;
    }

    public CommonAutoDeploymentProperties deploymentPropertiesForEngine(String engineType) {
        CommonAutoDeploymentProperties properties = new CommonAutoDeploymentProperties();

        EngineDeploymentProperties engineSpecificProperties = engine.get(engineType);
        if (engineSpecificProperties == null) {
            properties.setUseLock(useLock);
            properties.setLockWaitTime(lockWaitTime);
            properties.setThrowExceptionOnDeploymentFailure(throwExceptionOnDeploymentFailure);
            properties.setLockName(lockName);
        } else {
            properties.setUseLock(defaultIfNotNull(engineSpecificProperties.getUseLock(), useLock));
            properties.setLockWaitTime(defaultIfNotNull(engineSpecificProperties.getLockWaitTime(), lockWaitTime));
            properties.setThrowExceptionOnDeploymentFailure(
                defaultIfNotNull(engineSpecificProperties.getThrowExceptionOnDeploymentFailure(), throwExceptionOnDeploymentFailure));
            properties.setLockName(defaultIfNotNull(engineSpecificProperties.getLockName(), lockName));
        }

        return properties;
    }

    protected <T> T defaultIfNotNull(T value, T defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    public static class EngineDeploymentProperties {

        /**
         * Whether to use a lock when performing the auto deployment.
         * If not set then the global default would be used.
         */
        private Boolean useLock;

        /**
         * Duration to wait for the auto deployment lock before giving up.
         * If not set then the global default would be used.
         */
        private Duration lockWaitTime;

        /**
         * Whether to throw an exception if there was some kind of failure during the auto deployment.
         * If not set then the global default would be used.
         */
        private Boolean throwExceptionOnDeploymentFailure;

        /**
         * Name of the lock that should be used for the auto deployment.
         * If not defined then the deployment name or the global default would be used.
         */
        private String lockName;

        public Boolean getUseLock() {
            return useLock;
        }

        public void setUseLock(Boolean useLock) {
            this.useLock = useLock;
        }

        public Duration getLockWaitTime() {
            return lockWaitTime;
        }

        public void setLockWaitTime(Duration lockWaitTime) {
            this.lockWaitTime = lockWaitTime;
        }

        public Boolean getThrowExceptionOnDeploymentFailure() {
            return throwExceptionOnDeploymentFailure;
        }

        public void setThrowExceptionOnDeploymentFailure(Boolean throwExceptionOnDeploymentFailure) {
            this.throwExceptionOnDeploymentFailure = throwExceptionOnDeploymentFailure;
        }

        public String getLockName() {
            return lockName;
        }

        public void setLockName(String lockName) {
            this.lockName = lockName;
        }
    }

}
