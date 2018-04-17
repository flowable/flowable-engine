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
package org.flowable.form.api;

import java.util.Map;

import org.flowable.common.engine.api.management.TableMetaData;
import org.flowable.common.engine.api.management.TablePageQuery;

/**
 * Service for admin and maintenance operations on the form engine.
 * 
 * These operations will typically not be used in a workflow driven application, but are used in for example the operational console.
 * 
 * @author Tijs Rademakers
 */
public interface FormManagementService {

    /**
     * Get the mapping containing {table name, row count} entries of the Flowable database schema.
     */
    Map<String, Long> getTableCount();

    /**
     * Gets the table name (including any configured prefix) for an Flowable entity like Task, Execution or the like.
     */
    String getTableName(Class<?> idmEntityClass);

    /**
     * Gets the metadata (column names, column types, etc.) of a certain table. Returns null when no table exists with the given name.
     */
    TableMetaData getTableMetaData(String tableName);

    /**
     * Creates a {@link TablePageQuery} that can be used to fetch {@link org.flowable.common.engine.api.management.TablePage} containing specific sections of table row data.
     */
    TablePageQuery createTablePageQuery();

}
