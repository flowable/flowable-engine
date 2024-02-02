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
package org.flowable.common.engine.impl.cmd;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntity;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityManager;

/**
 * @author Filip Hrisafov
 */
public class LockCmd implements Command<Boolean> {

    protected static final String hostLockDescription;

    static {
        InetAddress localhost;
        try {
            localhost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            localhost = null;
        }

        StringBuilder sb = new StringBuilder();

        if (localhost != null) {
            sb.append(" - ").append(localhost.getHostName()).append('(').append(localhost.getHostAddress()).append(')');
        } else {
            sb.append(" - ").append("unknown");
        }
        // The max length of the host lock description can bee 255 characters (the max length of the property value)
        // minus the ISO8601 length (27 characters on Java 11 with nanosecond precision)
        hostLockDescription = sb.substring(0, Math.min(sb.length(), (255 - 27)));
    }

    protected String lockName;
    protected String engineType;
    protected Duration forceAcquireAfter;

    public LockCmd(String lockName, String engineType) {
        this(lockName, null, engineType);
    }

    public LockCmd(String lockName, Duration forceAcquireAfter, String engineType) {
        this.lockName = lockName;
        this.engineType = engineType;
        this.forceAcquireAfter = forceAcquireAfter;
    }

    @Override
    public Boolean execute(CommandContext commandContext) {
        AbstractEngineConfiguration engineConfiguration = commandContext.getEngineConfigurations().get(engineType);
        PropertyEntityManager propertyEntityManager = engineConfiguration.getPropertyEntityManager();
        PropertyEntity property = propertyEntityManager.findById(lockName);
        if (property == null) {
            property = propertyEntityManager.create();
            property.setName(lockName);
            // The format of the value is the current time in ISO8601 - hostName(hostAddress)
            property.setValue(Instant.now().toString() + hostLockDescription);
            propertyEntityManager.insert(property);
            return true;
        } else if (property.getValue() == null) {
            property.setValue(Instant.now().toString() + hostLockDescription);
            return true;
        } else if (forceAcquireAfter != null) {
            // If the lock is held longer than the force acquire duration we have to force the lock acquire
            // e.g. if the lock was acquired at 17:00
            // When the forceAcquireAfter is 10 minutes it means that the lock should be force acquired
            // when the time is after 17:10, i.e. acquireLock + forceAcquire < now (17:10)
            String value = property.getValue();
            Instant lockAcquireTime = Instant.parse(value.substring(0, value.indexOf('Z') + 1));
            if (lockAcquireTime.plus(forceAcquireAfter).isBefore(Instant.now())) {
                property.setValue(Instant.now().toString() + hostLockDescription);
                return true;
            }

            return false;
        } else {
            return false;
        }
    }
}
