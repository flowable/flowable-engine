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
package org.flowable.job.service.impl.db;

import org.flowable.common.engine.impl.db.ServiceSqlScriptBasedDbSchemaManager;

/**
 * @author Joram Barrez 
 */
public class JobDbSchemaManager extends ServiceSqlScriptBasedDbSchemaManager {
    
    private static final String JOB_TABLE = "ACT_RU_JOB";
    private static final String JOB_VERSION_PROPERTY = "job.schema.version";
    private static final String SCHEMA_COMPONENT = "job";
    
    public JobDbSchemaManager() {
        super(JOB_TABLE, SCHEMA_COMPONENT, null, JOB_VERSION_PROPERTY);
    }

    @Override
    protected String getResourcesRootDirectory() {
        return "org/flowable/job/service/db/";
    }

}
