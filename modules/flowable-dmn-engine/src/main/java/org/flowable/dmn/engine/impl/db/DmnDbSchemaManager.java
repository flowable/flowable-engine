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

package org.flowable.dmn.engine.impl.db;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.db.EngineSchemaManagerLockConfiguration;
import org.flowable.common.engine.impl.db.EngineSqlScriptBasedDbSchemaManager;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;

public class DmnDbSchemaManager extends EngineSqlScriptBasedDbSchemaManager {

    protected static final String DMN_DB_SCHEMA_LOCK_NAME = "dmnDbSchemaLock";

    protected static final Map<String, String> changeLogVersionMap = Map.ofEntries(
            Map.entry("1", "6.0.0.5"),
            Map.entry("2", "6.1.1.0"),
            Map.entry("3", "6.3.0.0"),
            Map.entry("4", "6.3.1.0"),
            Map.entry("5", "6.4.0.0"),
            Map.entry("6", "6.4.1.3"),
            Map.entry("7", "6.6.0.0"),
            Map.entry("8", "6.6.0.0"),
            Map.entry("9", "6.8.0.0"),
            Map.entry("10", "7.1.0.0")
    );

    public DmnDbSchemaManager() {
        super("dmn", new EngineSchemaManagerLockConfiguration(CommandContextUtil::getDmnEngineConfiguration));
    }

    @Override
    protected String getEngineVersion() {
        return DmnEngine.VERSION;
    }

    @Override
    protected String getSchemaVersionPropertyName() {
        return "dmn.schema.version";
    }

    @Override
    protected String getDbSchemaLockName() {
        return DMN_DB_SCHEMA_LOCK_NAME;
    }

    @Override
    protected String getEngineTableName() {
        return "ACT_DMN_DECISION";
    }

    @Override
    protected String getChangeLogTableName() {
        return "ACT_DMN_DATABASECHANGELOG";
    }

    @Override
    protected String getDbVersionForChangelogVersion(String changeLogVersion) {
        if (StringUtils.isNotEmpty(changeLogVersion) && changeLogVersionMap.containsKey(changeLogVersion)) {
            return changeLogVersionMap.get(changeLogVersion);
        }
        return "5.99.0.0";
    }

    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/dmn/db/";
    }
}
