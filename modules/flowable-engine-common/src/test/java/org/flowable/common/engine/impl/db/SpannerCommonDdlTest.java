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
package org.flowable.common.engine.impl.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flowable.common.engine.impl.util.ReflectUtil;
import org.junit.jupiter.api.Test;

class SpannerCommonDdlTest {

    private static final String CREATE_RESOURCE = "org/flowable/common/db/create/flowable.spanner.create.common.sql";
    private static final String DROP_RESOURCE = "org/flowable/common/db/drop/flowable.spanner.drop.common.sql";

    private static final List<String> FORBIDDEN_LEGACY_TYPES = Arrays.asList(
            "varchar", "bytea", "longblob", "longvarbinary", "tinyint", "double precision", "datetime"
    );

    private static final List<String> EXPECTED_TABLES = Arrays.asList(
            "ACT_GE_PROPERTY", "ACT_GE_BYTEARRAY", "FLW_RU_BATCH", "FLW_RU_BATCH_PART",
            "ACT_RU_ENTITYLINK", "ACT_HI_ENTITYLINK", "ACT_RU_IDENTITYLINK", "ACT_HI_IDENTITYLINK",
            "ACT_RU_JOB", "ACT_RU_TIMER_JOB", "ACT_RU_SUSPENDED_JOB", "ACT_RU_DEADLETTER_JOB",
            "ACT_RU_HISTORY_JOB", "ACT_RU_EXTERNAL_JOB", "ACT_RU_TASK", "ACT_HI_TASKINST",
            "ACT_HI_TSK_LOG", "ACT_RU_VARIABLE", "ACT_HI_VARINST", "ACT_RU_EVENT_SUBSCR"
    );

    @Test
    void testResourcesExist() {
        try (InputStream createStream = ReflectUtil.getResourceAsStream(CREATE_RESOURCE)) {
            assertThat(createStream).as("Create common DDL resource must exist: %s", CREATE_RESOURCE).isNotNull();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (InputStream dropStream = ReflectUtil.getResourceAsStream(DROP_RESOURCE)) {
            assertThat(dropStream).as("Drop common DDL resource must exist: %s", DROP_RESOURCE).isNotNull();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCreateScriptSyntaxAndTypes() throws IOException {
        String content = readResource(CREATE_RESOURCE);
        List<String> statements = parseStatements(content);
        assertThat(statements).isNotEmpty();

        for (String type : FORBIDDEN_LEGACY_TYPES) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(type) + "\\b", Pattern.CASE_INSENSITIVE);
            assertThat(pattern.matcher(content).find())
                    .as("GoogleSQL DDL should not contain legacy type '%s'", type)
                    .isFalse();
        }

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

        for (String expectedTable : EXPECTED_TABLES) {
            assertThat(createdTables).as("Expected table %s to be created", expectedTable).contains(expectedTable);
        }

        // ACT_RU_DEADLETTER_JOB must not have RETRIES_ column
        Pattern deadletterPattern = Pattern.compile(
                "CREATE\\s+TABLE\\s+ACT_RU_DEADLETTER_JOB\\s*\\((.*?)\\)\\s*PRIMARY\\s+KEY",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher deadletterMatcher = deadletterPattern.matcher(content);
        assertThat(deadletterMatcher.find()).as("ACT_RU_DEADLETTER_JOB table definition must be found").isTrue();
        assertThat(deadletterMatcher.group(1))
                .as("ACT_RU_DEADLETTER_JOB must not contain RETRIES_ column")
                .doesNotContainIgnoringCase("RETRIES_");

        assertThat(content).contains("common.schema.version");
        assertThat(content).contains("8.1.0.1");
        assertThat(content).contains("next.dbid");
    }

    @Test
    void testDropScriptSyntax() throws IOException {
        String createContent = readResource(CREATE_RESOURCE);
        String dropContent = readResource(DROP_RESOURCE);
        List<String> statements = parseStatements(dropContent);
        assertThat(statements).isNotEmpty();

        // 1. Spanner does not support DROP CONSTRAINT IF EXISTS
        Pattern illegalDropConstraintPattern = Pattern.compile(
                "DROP\\s+CONSTRAINT\\s+IF\\s+EXISTS",
                Pattern.CASE_INSENSITIVE);
        assertThat(illegalDropConstraintPattern.matcher(dropContent).find())
                .as("GoogleSQL does not support 'DROP CONSTRAINT IF EXISTS' in drop script")
                .isFalse();

        // 2. All foreign key constraints created in CREATE script must be dropped
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
                    .as("Drop script must drop foreign key constraint %s on %s", constraintName, tableName)
                    .isTrue();
        }
        assertThat(expectedFks).as("Common create script must have foreign key constraints").isNotEmpty();

        // 3. All secondary & unique indexes created in CREATE script must be dropped with DROP INDEX IF EXISTS
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
                    .as("Drop script must drop index %s using DROP INDEX IF EXISTS", indexName)
                    .isTrue();
        }
        assertThat(expectedIndexes).as("Common create script must have indexes").isNotEmpty();

        // 4. All expected tables must be dropped
        for (String expectedTable : EXPECTED_TABLES) {
            assertThat(dropContent)
                    .as("Drop script should drop table %s", expectedTable)
                    .containsIgnoringCase(expectedTable);
        }
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
