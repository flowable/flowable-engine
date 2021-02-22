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
package org.flowable.form.engine.impl.db;

import org.flowable.common.engine.impl.db.EngineDatabaseConfiguration;
import org.flowable.common.engine.impl.db.LiquibaseBasedSchemaManager;
import org.flowable.common.engine.impl.db.LiquibaseDatabaseConfiguration;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.util.CommandContextUtil;

public class FormDbSchemaManager extends LiquibaseBasedSchemaManager {
    
    public static final String LIQUIBASE_CHANGELOG = "org/flowable/form/db/liquibase/flowable-form-db-changelog.xml";

    public FormDbSchemaManager() {
        super("form", LIQUIBASE_CHANGELOG, FormEngineConfiguration.LIQUIBASE_CHANGELOG_PREFIX);
    }

    @Override
    protected LiquibaseDatabaseConfiguration getDatabaseConfiguration() {
        return new EngineDatabaseConfiguration(CommandContextUtil.getFormEngineConfiguration());
    }

    public void initSchema(FormEngineConfiguration formEngineConfiguration) {
        initSchema(formEngineConfiguration.getDatabaseSchemaUpdate());
    }

    @Override
    public void schemaCreate() {
        getCommonSchemaManager().schemaCreate();
        super.schemaCreate();
    }

    @Override
    public void schemaDrop() {
        try {
            super.schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping form engine tables", e);
        }

        try {
            getCommonSchemaManager().schemaDrop();
        } catch (Exception e) {
            logger.info("Error dropping common tables", e);
        }
    }

    @Override
    public String schemaUpdate() {
        getCommonSchemaManager().schemaUpdate();
        return super.schemaUpdate();
    }

    protected SchemaManager getCommonSchemaManager() {
        return CommandContextUtil.getFormEngineConfiguration().getCommonSchemaManager();
    }

}
