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
package org.flowable.app.engine.impl.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flowable.common.engine.impl.util.ReflectUtil;
import org.junit.jupiter.api.Test;

class SpannerAppDdlTest {

    private static final String CREATE_APP_RESOURCE = "org/flowable/app/db/create/flowable.spanner.create.app.sql";
    private static final String DROP_APP_RESOURCE = "org/flowable/app/db/drop/flowable.spanner.drop.app.sql";

    private static final List<String> FORBIDDEN_LEGACY_TYPES = Arrays.asList(
            "varchar", "bytea", "longblob", "longvarbinary", "tinyint", "double precision", "datetime"
    );

    private static final List<String> EXPECTED_APP_TABLES = Arrays.asList(
            "ACT_APP_DEPLOYMENT", "ACT_APP_DEPLOYMENT_RESOURCE", "ACT_APP_APPDEF"
    );

    @Test
    void testResourcesExist() {
        assertResourceExists(CREATE_APP_RESOURCE);
        assertResourceExists(DROP_APP_RESOURCE);
    }

    @Test
    void testCreateAppScriptSyntaxAndTypes() throws IOException {
        String content = readResource(CREATE_APP_RESOURCE);
        List<String> statements = parseStatements(content);
        assertThat(statements).isNotEmpty();

        for (String type : FORBIDDEN_LEGACY_TYPES) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(type) + "\\b", Pattern.CASE_INSENSITIVE);
            assertThat(pattern.matcher(content).find())
                    .as("GoogleSQL App DDL should not contain legacy type '%s'", type)
                    .isFalse();
        }

        List<String> createdTables = extractCreatedTables(statements);
        for (String expectedTable : EXPECTED_APP_TABLES) {
            assertThat(createdTables).as("Expected App table %s to be created", expectedTable).contains(expectedTable);
        }

        assertThat(content).containsIgnoringCase("CREATE UNIQUE NULL_FILTERED INDEX");
        assertThat(content).contains("app.schema.version");
        assertThat(content).contains("8.1.0.1");
    }

    @Test
    void testDropAppScriptSyntax() throws IOException {
        String createContent = readResource(CREATE_APP_RESOURCE);
        String dropContent = readResource(DROP_APP_RESOURCE);
        List<String> statements = parseStatements(dropContent);
        assertThat(statements).isNotEmpty();

        Pattern illegalDropConstraintPattern = Pattern.compile(
                "DROP\\s+CONSTRAINT\\s+IF\\s+EXISTS",
                Pattern.CASE_INSENSITIVE);
        assertThat(illegalDropConstraintPattern.matcher(dropContent).find())
                .as("GoogleSQL does not support 'DROP CONSTRAINT IF EXISTS' in App drop script")
                .isFalse();

        Pattern fkPattern = Pattern.compile(
                "ALTER\\s+TABLE\\s+([A-Za-z0-9_]+)\\s+ADD\\s+CONSTRAINT\\s+([A-Za-z0-9_]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher fkMatcher = fkPattern.matcher(createContent);
        List<String> expectedFks = new ArrayList<>();
        while (fkMatcher.find()) {
            String tableName = fkMatcher.group(1).toUpperCase(Locale.ROOT);
            String constraintName = fkMatcher.group(2).toUpperCase(Locale.ROOT);
            expectedFks.add(constraintName);
            Pattern dropFkPattern = Pattern.compile(
                    "ALTER\\s+TABLE\\s+" + Pattern.quote(tableName) + "\\s+DROP\\s+CONSTRAINT\\s+" + Pattern.quote(constraintName),
                    Pattern.CASE_INSENSITIVE);
            assertThat(dropFkPattern.matcher(dropContent).find())
                    .as("App drop script must drop foreign key constraint %s on %s", constraintName, tableName)
                    .isTrue();
        }
        assertThat(expectedFks).as("App create script must have foreign key constraints").isNotEmpty();

        Pattern indexPattern = Pattern.compile(
                "CREATE\\s+(?:UNIQUE\\s+)?(?:NULL_FILTERED\\s+)?INDEX\\s+([A-Za-z0-9_]+)",
                Pattern.CASE_INSENSITIVE);
        Matcher idxMatcher = indexPattern.matcher(createContent);
        List<String> expectedIndexes = new ArrayList<>();
        while (idxMatcher.find()) {
            String indexName = idxMatcher.group(1).toUpperCase(Locale.ROOT);
            expectedIndexes.add(indexName);
            Pattern dropIdxPattern = Pattern.compile(
                    "DROP\\s+INDEX\\s+IF\\s+EXISTS\\s+" + Pattern.quote(indexName),
                    Pattern.CASE_INSENSITIVE);
            assertThat(dropIdxPattern.matcher(dropContent).find())
                    .as("App drop script must drop index %s using DROP INDEX IF EXISTS", indexName)
                    .isTrue();
        }
        assertThat(expectedIndexes).as("App create script must have indexes").isNotEmpty();

        for (String expectedTable : EXPECTED_APP_TABLES) {
            assertThat(dropContent)
                    .as("Drop App script should drop table %s", expectedTable)
                    .containsIgnoringCase(expectedTable);
        }
    }

    @Test
    void testDistroAllCreateScript() throws IOException {
        Path distroPath = Paths.get("../../distro/sql/create/all/flowable.spanner.all.create.sql");
        if (!Files.exists(distroPath)) {
            distroPath = Paths.get("distro/sql/create/all/flowable.spanner.all.create.sql");
        }
        assertThat(Files.exists(distroPath)).as("Unified Spanner create script must exist at %s", distroPath).isTrue();

        String content = Files.readString(distroPath, StandardCharsets.UTF_8);
        List<String> statements = parseStatements(content);
        assertThat(statements).isNotEmpty();

        for (String type : FORBIDDEN_LEGACY_TYPES) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(type) + "\\b", Pattern.CASE_INSENSITIVE);
            assertThat(pattern.matcher(content).find())
                    .as("GoogleSQL unified distro DDL should not contain legacy type '%s'", type)
                    .isFalse();
        }

        List<String> createdTables = extractCreatedTables(statements);
        List<String> keyTablesFromEachSubsystem = Arrays.asList(
                "ACT_GE_PROPERTY", "ACT_GE_BYTEARRAY",
                "ACT_RE_PROCDEF", "ACT_RU_EXECUTION",
                "ACT_HI_PROCINST",
                "ACT_APP_APPDEF",
                "ACT_CMMN_CASEDEF",
                "ACT_DMN_DECISION",
                "FLW_EVENT_DEFINITION",
                "ACT_ID_USER"
        );
        for (String table : keyTablesFromEachSubsystem) {
            assertThat(createdTables).as("Unified distro script must create %s", table).contains(table);
        }
    }

    private void assertResourceExists(String resourceName) {
        try (InputStream stream = ReflectUtil.getResourceAsStream(resourceName)) {
            assertThat(stream).as("DDL resource must exist: %s", resourceName).isNotNull();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> extractCreatedTables(List<String> statements) {
        List<String> createdTables = new ArrayList<>();
        Pattern createTablePattern = Pattern.compile("CREATE\\s+TABLE\\s+([A-Z0-9_]+)", Pattern.CASE_INSENSITIVE);
        for (String stmt : statements) {
            Matcher m = createTablePattern.matcher(stmt);
            if (m.find()) {
                String tableName = m.group(1).toUpperCase(Locale.ROOT);
                createdTables.add(tableName);
                assertThat(stmt)
                        .as("Table %s must define PRIMARY KEY in GoogleSQL syntax", tableName)
                        .containsIgnoringCase("PRIMARY KEY");
            }
        }
        return createdTables;
    }

    private String readResource(String resourceName) throws IOException {
        try (InputStream inputStream = ReflectUtil.getResourceAsStream(resourceName)) {
            assertThat(inputStream).as("Resource %s must exist", resourceName).isNotNull();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            return sb.toString();
        }
    }

    private List<String> parseStatements(String ddl) throws IOException {
        List<String> statements = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(ddl))) {
            String line;
            StringBuilder currentStmt = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("#")) {
                    continue;
                }
                currentStmt.append(trimmed).append(" ");
                if (trimmed.endsWith(";")) {
                    statements.add(currentStmt.toString().trim());
                    currentStmt.setLength(0);
                }
            }
        }
        return statements;
    }
}
