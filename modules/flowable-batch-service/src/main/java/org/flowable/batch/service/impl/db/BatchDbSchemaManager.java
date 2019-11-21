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
package org.flowable.batch.service.impl.db;

import org.flowable.common.engine.impl.db.ServiceSqlScriptBasedDbSchemaManager;

/**
 * @author Tijs Rademakers
 */
public class BatchDbSchemaManager extends ServiceSqlScriptBasedDbSchemaManager {
    
    private static final String TABLE = "FLW_RU_BATCH";
    private static final String VERSION_PROPERTY = "batch.schema.version";
    private static final String SCHEMA_COMPONENT = "batch";
    
    public BatchDbSchemaManager() {
        super(TABLE, SCHEMA_COMPONENT, null, VERSION_PROPERTY);
    }

    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/batch/service/db/";
    }

}
