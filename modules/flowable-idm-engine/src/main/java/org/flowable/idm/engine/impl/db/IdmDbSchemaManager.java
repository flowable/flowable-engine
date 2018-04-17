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
package org.flowable.idm.engine.impl.db;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableWrongDbException;
import org.flowable.common.engine.impl.db.ServiceSqlScriptBasedDbSchemaManager;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdmDbSchemaManager extends ServiceSqlScriptBasedDbSchemaManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(IdmDbSchemaManager.class);
    
    private static final String IDM_PROPERTY_TABLE = "ACT_ID_PROPERTY";
    private static final String VERSION_PROPERTY = "schema.version";
    private static final String SCHEMA_COMPONENT = "identity";
    
   public IdmDbSchemaManager() {
       // note; no schema property set, managed by this class itself as it has (for historical reasons) it's own property table
       super(IDM_PROPERTY_TABLE, SCHEMA_COMPONENT, null, VERSION_PROPERTY); 
   }
   
   @Override
   protected String getResourcesRootDirectory() {
       return "org/flowable/idm/db/";
   }
   
   @Override
   protected String getPropertyTable() {
       return IDM_PROPERTY_TABLE;
   }
   
   @Override
   protected String getUpgradeStartVersion() {
       return "5.99.0.0";
   }
   
   @Override
   protected void internalDbSchemaCreate() {
       // User and Group tables can already have been created by the process engine in an earlier version
       if (isIdmGroupTablePresent()) {
           dbSchemaUpdate();

       } else {
           super.internalDbSchemaCreate();
       }
   } 
   
   @Override
   protected boolean isUpdateNeeded() {
       boolean propertyTablePresent = isTablePresent(IDM_PROPERTY_TABLE);
       if (!propertyTablePresent) {
           return isIdmGroupTablePresent();
       }
       return true;
   }
   
   public boolean isIdmGroupTablePresent() {
       return isTablePresent("ACT_ID_GROUP");
   }
   
   public void dbSchemaCheckVersion() {
       try {
           String dbVersion = getSchemaVersion();
           if (!IdmEngine.VERSION.equals(dbVersion)) {
               throw new FlowableWrongDbException(IdmEngine.VERSION, dbVersion);
           }

           String errorMessage = null;
           if (!isTablePresent(IDM_PROPERTY_TABLE)) {
               errorMessage = addMissingComponent(errorMessage, "engine");
           }

           if (errorMessage != null) {
               throw new FlowableException("Flowable IDM database problem: " + errorMessage);
           }

       } catch (Exception e) {
           if (isMissingTablesException(e)) {
               throw new FlowableException(
                       "no flowable tables in db. set <property name=\"databaseSchemaUpdate\" to value=\"true\" or value=\"create-drop\" (use create-drop for testing only!) in bean processEngineConfiguration in flowable.cfg.xml for automatic schema creation",
                       e);
           } else {
               if (e instanceof RuntimeException) {
                   throw (RuntimeException) e;
               } else {
                   throw new FlowableException("couldn't get db schema version", e);
               }
           }
       }

       LOGGER.debug("flowable idm db schema check successful");
   }
   
   protected String addMissingComponent(String missingComponents, String component) {
       if (missingComponents == null) {
           return "Tables missing for component(s) " + component;
       }
       return missingComponents + ", " + component;
   }

   protected boolean isMissingTablesException(Exception e) {
       String exceptionMessage = e.getMessage();
       if (e.getMessage() != null) {
           // Matches message returned from H2
           if ((exceptionMessage.indexOf("Table") != -1) && (exceptionMessage.indexOf("not found") != -1)) {
               return true;
           }

           // Message returned from MySQL and Oracle
           if (((exceptionMessage.indexOf("Table") != -1 || exceptionMessage.indexOf("table") != -1)) && (exceptionMessage.indexOf("doesn't exist") != -1)) {
               return true;
           }

           // Message returned from Postgres
           if (((exceptionMessage.indexOf("relation") != -1 || exceptionMessage.indexOf("table") != -1)) && (exceptionMessage.indexOf("does not exist") != -1)) {
               return true;
           }
       }
       return false;
   }
   
   public void performSchemaOperationsIdmEngineBuild() {
       String databaseSchemaUpdate = CommandContextUtil.getIdmEngineConfiguration().getDatabaseSchemaUpdate();
       if (IdmEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)) {
           try {
               dbSchemaDrop();
           } catch (RuntimeException e) {
               // ignore
           }
       }
       if (IdmEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP.equals(databaseSchemaUpdate) || IdmEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE.equals(databaseSchemaUpdate)
               || IdmEngineConfiguration.DB_SCHEMA_UPDATE_CREATE.equals(databaseSchemaUpdate)) {
           dbSchemaCreate();

       } else if (IdmEngineConfiguration.DB_SCHEMA_UPDATE_FALSE.equals(databaseSchemaUpdate)) {
           dbSchemaCheckVersion();

       } else if (IdmEngineConfiguration.DB_SCHEMA_UPDATE_TRUE.equals(databaseSchemaUpdate)) {
           dbSchemaUpdate();
       }
   }


}
