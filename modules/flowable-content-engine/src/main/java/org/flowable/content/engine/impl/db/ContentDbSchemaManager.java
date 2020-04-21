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
package org.flowable.content.engine.impl.db;

import org.flowable.common.engine.impl.db.EngineDatabaseConfiguration;
import org.flowable.common.engine.impl.db.LiquibaseBasedSchemaManager;
import org.flowable.common.engine.impl.db.LiquibaseDatabaseConfiguration;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.util.CommandContextUtil;

public class ContentDbSchemaManager extends LiquibaseBasedSchemaManager {
    
    public static final String LIQUIBASE_CHANGELOG = "org/flowable/content/db/liquibase/flowable-content-db-changelog.xml";

    public ContentDbSchemaManager() {
        super("content", LIQUIBASE_CHANGELOG, ContentEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX);
    }

    @Override
    protected LiquibaseDatabaseConfiguration getDatabaseConfiguration() {
        return new EngineDatabaseConfiguration(CommandContextUtil.getContentEngineConfiguration());
    }

    public void initSchema() {
        initSchema(CommandContextUtil.getContentEngineConfiguration().getDatabaseSchemaUpdate());
    }
}
