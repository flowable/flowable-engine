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
package org.flowable.cmmn.engine.impl.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.engine.impl.db.CmmnDbSchemaManager;
import org.flowable.engine.common.impl.FlowableVersion;
import org.flowable.engine.common.impl.FlowableVersions;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.DatabaseFactory;
import liquibase.database.core.DB2Database;
import liquibase.database.core.H2Database;
import liquibase.database.core.HsqlDatabase;
import liquibase.database.core.MSSQLDatabase;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.core.OracleDatabase;
import liquibase.database.core.PostgresDatabase;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;

/**
 * When this class is executed, it generates
 * - the create DDL script for the cmmn engine
 * - the upgrade script for going from the previous version to the current version.
 * 
 * @author Joram Barrez
 */
public class DbSchemaGenerator {
    
    // The baseline version, i.e. the version at the time that the first sql script was shipped 
    private static final String BASELINE_VERSION = "6.2.0.0";
    
    private static final String PREFIX_CHANGELOG = "src/main/resources/org/flowable/cmmn/db/liquibase/csv/changelog-";
    
    private static final Map<String, String> DATABASES = new HashMap<>();
    
    static {
        DATABASES.put("db2", new DB2Database().getShortName());
        DATABASES.put("h2", new H2Database().getShortName());
        DATABASES.put("hsql", new HsqlDatabase().getShortName());
        DATABASES.put("mssql", new MSSQLDatabase().getShortName());
        DATABASES.put("mysql", new MySQLDatabase().getShortName());
        DATABASES.put("oracle", new OracleDatabase().getShortName()); 
        DATABASES.put("postgres", new PostgresDatabase().getShortName());
    }
    
    public static void main(String[] args) throws Exception {
        for (String db : DATABASES.keySet()) {
            generateCreateScript(db);
            generateUpdateScript(db);
        }
    }

    protected static void generateCreateScript(String db) throws Exception {
        String changeLogFileName = PREFIX_CHANGELOG + "temp.csv";
        File tempChangeLogFile = new File(changeLogFileName);
        if (tempChangeLogFile.exists()) {
            tempChangeLogFile.delete();
        }
        tempChangeLogFile.createNewFile();
        
        Database database = determineDatabase(db, changeLogFileName);
        generateSql(db, false, database);
        tempChangeLogFile.delete();
    }
    
    protected static void generateUpdateScript(String db) throws IOException, DatabaseException, LiquibaseException, FileNotFoundException {
        boolean isUpdate = !FlowableVersions.CURRENT_VERSION.equals(BASELINE_VERSION);
        String changeLogFile = determineChangeLogFile(db, isUpdate);
        Database database = determineDatabase(db, changeLogFile);
        generateSql(db, isUpdate, database);
    }

    protected static String determineChangeLogFile(String db, boolean isUpdate) throws IOException {
        String baseChangeLogFileName = PREFIX_CHANGELOG + db + "-"; 
        String changeLogFileName = baseChangeLogFileName + cleanVersion(FlowableVersions.CURRENT_VERSION) + ".csv";
        if (isUpdate) {
            String previousChangeLogFileName = baseChangeLogFileName + cleanVersion(FlowableVersions.getPreviousVersion(FlowableVersions.CURRENT_VERSION).getMainVersion()) + ".csv";
            File previousChangeLogFile = new File(previousChangeLogFileName);
            File newChangeLogFile = new File(changeLogFileName);
            if (newChangeLogFile.exists()) {
                newChangeLogFile.delete();
            }
            Files.copy(previousChangeLogFile.toPath(), newChangeLogFile.toPath());
        } else {
            File changeLogFile = new File(changeLogFileName);
            if (changeLogFile.exists()) {
                changeLogFile.delete();
            }
        }
        return changeLogFileName;
    }

    protected static Database determineDatabase(String db, String changeLogFile) throws DatabaseException {
        DatabaseFactory databaseFactory = DatabaseFactory.getInstance();
        DatabaseConnection databaseConnection = databaseFactory.openConnection("offline:" + DATABASES.get(db)
                + "?productName=" + db 
                + "&outputLiquibaseSql=ALL"
                + "&changeLogFile=" + changeLogFile,
                null, null, null, null);
        return DatabaseFactory.getInstance().findCorrectDatabaseImplementation(databaseConnection);
    }

    protected static void generateSql(String db, boolean isUpdate, Database database) throws LiquibaseException, IOException, FileNotFoundException {
        CmmnDbSchemaManager cmmnDbSchemaManager = new CmmnDbSchemaManager();
        Liquibase liquibase = cmmnDbSchemaManager.createLiquibaseInstance(database);
        
        File outputFile = null;
        if (isUpdate) {
            FlowableVersion previousVersion = FlowableVersions.getPreviousVersion(FlowableVersions.CURRENT_VERSION);
            outputFile = new File("src/main/resources/org/flowable/cmmn/db/upgrade/flowable." + db + ".cmmn.upgradestep." 
                    + cleanVersion(previousVersion.getMainVersion()) + ".to." + cleanVersion(FlowableVersions.CURRENT_VERSION) + ".sql");
        } else {
            outputFile = new File("src/main/resources/org/flowable/cmmn/db/create/flowable." + db + ".cmmn.create.sql");
        }
        
        if (outputFile.exists()) {
            outputFile.delete();
        }
        outputFile.createNewFile();
        
        StringWriter stringWriter = new StringWriter();
        liquibase.update("cmmn", stringWriter);
        stringWriter.close();
        String sqlScript = stringWriter.toString();
        
        writeSqlScriptToFile(outputFile, sqlScript);
    }
    
    protected static void writeSqlScriptToFile(File file, String sqlScript) throws FileNotFoundException, IOException {
        PrintWriter printWriter = new PrintWriter(new FileOutputStream(file));
        BufferedReader reader = new BufferedReader(new StringReader(sqlScript));
        String line = reader.readLine();
        int nrOfLinesWritten = 0;
        while (line != null) {
            if (!line.startsWith("--")) {
                printWriter.println(line);
                nrOfLinesWritten++;
            }
            line = reader.readLine();
        }
        printWriter.flush();
        printWriter.close();
        
        // No need to generate empty upgrade script
        if (nrOfLinesWritten == 0) {
            file.delete();
        }
    }

    protected static String cleanVersion(String version) {
        return version.replaceAll("\\.", "");
    }

}
