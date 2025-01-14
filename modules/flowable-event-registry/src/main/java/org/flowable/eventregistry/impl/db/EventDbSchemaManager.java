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
package org.flowable.eventregistry.impl.db;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.db.EngineSchemaManagerLockConfiguration;
import org.flowable.common.engine.impl.db.EngineSqlScriptBasedDbSchemaManager;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

public class EventDbSchemaManager extends EngineSqlScriptBasedDbSchemaManager {

    protected static final String EVENTREGISTRY_DB_SCHEMA_LOCK_NAME = "eventRegistryDbSchemaLock";

    protected static final Map<String, String> changeLogVersionMap = Map.ofEntries(
            Map.entry("1", "6.5.0.6"),
            Map.entry("2", "6.7.2.0"),
            Map.entry("3", "6.7.2.0"),
            Map.entry("4", "7.1.0.0"),
            Map.entry("5", "7.1.0.0")
    );

    public EventDbSchemaManager() {
        super("eventregistry", new EngineSchemaManagerLockConfiguration(CommandContextUtil::getEventRegistryConfiguration));
    }

    @Override
    protected String getEngineVersion() {
        return EventRegistryEngine.VERSION;
    }

    @Override
    protected String getSchemaVersionPropertyName() {
        return "eventregistry.schema.version";
    }

    @Override
    protected String getDbSchemaLockName() {
        return EVENTREGISTRY_DB_SCHEMA_LOCK_NAME;
    }

    @Override
    protected String getEngineTableName() {
        return "FLW_EVENT_DEFINITION";
    }

    @Override
    protected String getChangeLogTableName() {
        return "FLW_EV_DATABASECHANGELOG";
    }

    @Override
    protected String getDbVersionForChangelogVersion(String changeLogVersion) {
        if (StringUtils.isNotEmpty(changeLogVersion) && changeLogVersionMap.containsKey(changeLogVersion)) {
            return changeLogVersionMap.get(changeLogVersion);
        }
        return "6.5.0.0";
    }

    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/eventregistry/db/";
    }

}
