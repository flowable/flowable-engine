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
package org.flowable.cmmn.engine.impl.db;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.db.EngineSchemaManagerLockConfiguration;
import org.flowable.common.engine.impl.db.EngineSqlScriptBasedDbSchemaManager;

public class CmmnDbSchemaManager extends EngineSqlScriptBasedDbSchemaManager {

    protected static final String CMMN_DB_SCHEMA_LOCK_NAME = "cmmnDbSchemaLock";

    protected static final Map<String, String> changeLogVersionMap = Map.ofEntries(
            Map.entry("1", "6.2.0.0"),
            Map.entry("2", "6.2.1.0"),
            Map.entry("3", "6.3.0.0"),
            Map.entry("4", "6.3.1.0"),
            Map.entry("5", "6.4.0.0"),
            Map.entry("6", "6.4.1.3"),
            Map.entry("7", "6.4.1.3"),
            Map.entry("8", "6.5.0.6"),
            Map.entry("9", "6.5.0.6"),
            Map.entry("10", "6.5.0.6"),
            Map.entry("11", "6.5.0.6"),
            Map.entry("12", "6.6.0.0"),
            Map.entry("13", "6.6.0.0"),
            Map.entry("14", "6.7.0.0"),
            Map.entry("15", "6.7.1.0"),
            Map.entry("16", "6.7.1.0"),
            Map.entry("17", "6.8.0.0"),
            Map.entry("18", "7.0.1.1"),
            Map.entry("19", "7.1.0.0"),
            Map.entry("20", "7.1.0.0")
    );

    public CmmnDbSchemaManager() {
        super("cmmn", new EngineSchemaManagerLockConfiguration(CommandContextUtil::getCmmnEngineConfiguration));
    }

    @Override
    protected String getEngineVersion() {
        return CmmnEngine.VERSION;
    }

    @Override
    protected String getSchemaVersionPropertyName() {
        return "cmmn.schema.version";
    }

    @Override
    protected String getDbSchemaLockName() {
        return CMMN_DB_SCHEMA_LOCK_NAME;
    }

    @Override
    protected String getEngineTableName() {
        return "ACT_CMMN_RU_CASE_INST";
    }

    @Override
    protected String getChangeLogTableName() {
        return "ACT_CMMN_DATABASECHANGELOG";
    }

    @Override
    protected String getDbVersionForChangelogVersion(String changeLogVersion) {
        if (StringUtils.isNotEmpty(changeLogVersion) && changeLogVersionMap.containsKey(changeLogVersion)) {
            return changeLogVersionMap.get(changeLogVersion);
        }
        return "6.1.2.0";
    }

    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/cmmn/db/";
    }
}
