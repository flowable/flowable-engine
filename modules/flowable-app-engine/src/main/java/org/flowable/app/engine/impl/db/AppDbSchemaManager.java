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
package org.flowable.app.engine.impl.db;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.db.EngineSchemaManagerLockConfiguration;
import org.flowable.common.engine.impl.db.EngineSqlScriptBasedDbSchemaManager;

public class AppDbSchemaManager extends EngineSqlScriptBasedDbSchemaManager {
    
    protected static final String APP_DB_SCHEMA_LOCK_NAME = "appDbSchemaLock";
    
    protected static final Map<String, String> changeLogVersionMap = Map.ofEntries(
            Map.entry("1", "6.3.1.0"),
            Map.entry("2", "6.4.0.0"),
            Map.entry("3", "6.4.1.3")
    );

    public AppDbSchemaManager() {
        super("app", new EngineSchemaManagerLockConfiguration(CommandContextUtil::getAppEngineConfiguration));
    }

    @Override
    protected String getEngineVersion() {
        return AppEngine.VERSION;
    }

    @Override
    protected String getSchemaVersionPropertyName() {
        return "app.schema.version";
    }

    @Override
    protected String getDbSchemaLockName() {
        return APP_DB_SCHEMA_LOCK_NAME;
    }

    @Override
    protected String getEngineTableName() {
        return "ACT_APP_DEPLOYMENT";
    }

    @Override
    protected String getChangeLogTableName() {
        return "ACT_APP_DATABASECHANGELOG";
    }

    @Override
    protected String getDbVersionForChangelogVersion(String changeLogVersion) {
        if (StringUtils.isNotEmpty(changeLogVersion) && changeLogVersionMap.containsKey(changeLogVersion)) {
            return changeLogVersionMap.get(changeLogVersion);
        }
        return "6.3.0.1";
    }

    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/app/db/";
    }
}
