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
package org.flowable.common.engine.api;

/**
 * Exception that is thrown when the Flowable engine discovers a mismatch between the database schema version and the engine version.
 * 
 * The check is done when the engine is created in {@link ProcessEngineBuilder#buildProcessEngine()}.
 * 
 * @author Tom Baeyens
 */
public class FlowableWrongDbException extends FlowableException {

    private static final long serialVersionUID = 1L;

    String libraryVersion;
    String dbVersion;

    public FlowableWrongDbException(String libraryVersion, String dbVersion) {
        super(
                "version mismatch: library version is '"
                        + libraryVersion
                        + "', db version is "
                        + dbVersion
                        + " Hint: Set <property name=\"databaseSchemaUpdate\" to value=\"true\" or value=\"create-drop\" (use create-drop for testing only!) in bean processEngineConfiguration in flowable.cfg.xml for automatic schema creation");
        this.libraryVersion = libraryVersion;
        this.dbVersion = dbVersion;
    }

    /**
     * The version of the Flowable library used.
     */
    public String getLibraryVersion() {
        return libraryVersion;
    }

    /**
     * The version of the Flowable library that was used to create the database schema.
     */
    public String getDbVersion() {
        return dbVersion;
    }
}
