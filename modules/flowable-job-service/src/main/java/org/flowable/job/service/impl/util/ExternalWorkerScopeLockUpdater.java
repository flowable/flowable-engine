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
package org.flowable.job.service.impl.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;

/**
 * Performs a conditional external-worker scope-lock renewal on the current
 * Flowable command transaction.
 */
public final class ExternalWorkerScopeLockUpdater {

    private ExternalWorkerScopeLockUpdater() {
    }

    public static void extend(
            String tableName,
            String scopeId,
            String lockOwner,
            Date newLockExpirationTime,
            Date currentTime) {

        if (tableName == null || tableName.isBlank()) {
            throw new FlowableException("Scope-lock table name is required");
        }
        if (scopeId == null || scopeId.isBlank()) {
            throw new FlowableOptimisticLockingException("External-worker scope id is missing");
        }
        if (lockOwner == null || lockOwner.isBlank() || newLockExpirationTime == null || currentTime == null) {
            throw new FlowableOptimisticLockingException("External-worker scope-lock ownership is incomplete");
        }

        String sql = "update " + tableName
                + " set LOCK_TIME_ = ?, LOCK_OWNER_ = ?, REV_ = REV_ + 1"
                + " where ID_ = ? and LOCK_OWNER_ = ? and LOCK_TIME_ > ?";

        Connection connection = CommandContextUtil.getDbSqlSession().getSqlSession().getConnection();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, new Timestamp(newLockExpirationTime.getTime()));
            statement.setString(2, lockOwner);
            statement.setString(3, scopeId);
            statement.setString(4, lockOwner);
            statement.setTimestamp(5, new Timestamp(currentTime.getTime()));

            int updated = statement.executeUpdate();
            if (updated != 1) {
                throw new FlowableOptimisticLockingException(
                        "The external-worker scope lock for " + scopeId + " is no longer owned and active");
            }
        } catch (SQLException exception) {
            throw new FlowableException("Could not extend external-worker scope lock for " + scopeId, exception);
        }
    }
}
