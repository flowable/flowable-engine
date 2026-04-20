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
package org.flowable.test.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import org.flowable.common.engine.impl.FlowableVersions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Filip Hrisafov
 */
class SqlUpgradeValidationTest {

    private static final Collection<String> VERSIONS_WITHOUT_HISTORY_UPGRADE = Set.of(
            "7.1.0.1",
            "7.1.0.2",
            "7.2.0.1",
            "7.2.0.2"
    );

    @ParameterizedTest
    @MethodSource("sqlComponentsWithDatabase")
    void validateUpgradeScripts(String databaseName, SqlComponent sqlComponent) {
        String upgradeResourceFormat = "%s/upgrade/flowable.%s.upgradestep.%s.to.%s.%s.sql";
        // We start with 7.1.0.0 since this is the version where we moved everything to use our own SQL instead of Liquibase for all of our modules
        int initialVersionIndex = FlowableVersions.findMatchingVersionIndex("7.1.0.0");
        if (initialVersionIndex < 0) {
            throw new IllegalStateException("Could not find Flowable version 7.1.0.0");
        }

        for (int i = initialVersionIndex + 1; i < FlowableVersions.FLOWABLE_VERSIONS.size(); i++) {
            String fromVersion = FlowableVersions.FLOWABLE_VERSIONS.get(i - 1).getMainVersion().replace(".", "");
            String toVersion = FlowableVersions.FLOWABLE_VERSIONS.get(i).getMainVersion().replace(".", "");
            String dbSpecificSqlFile = upgradeResourceFormat.formatted(sqlComponent.dbFolder(), databaseName, fromVersion, toVersion, sqlComponent.name());
            ClassPathResource dbSpecificResource = new ClassPathResource(dbSpecificSqlFile);
            if (!dbSpecificResource.exists()) {
                // If there is no DB-specific resource, then there has to be one for all databases
                String allDatabasesSqlFile = upgradeResourceFormat.formatted(sqlComponent.dbFolder(), "all", fromVersion, toVersion, sqlComponent.name());
                ClassPathResource allDatabasesResource = new ClassPathResource(allDatabasesSqlFile);
                assertThat(allDatabasesResource.exists())
                        .withFailMessage(
                                "Neither DB specific or all databases upgrade script found using file " + dbSpecificSqlFile + " or " + allDatabasesSqlFile)
                        .isTrue();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("databases")
    void validateHistoryUpgradeScripts(String databaseName) {
        // This process history DB component is a bit special
        // We don't need to have it always.
        String upgradeResourceFormat = "org/flowable/db/upgrade/flowable.%s.upgradestep.%s.to.%s.history.sql";
        // We start with 7.1.0.0 since this is the version where we moved everything to use our own SQL instead of Liquibase for all of our modules
        int initialVersionIndex = FlowableVersions.findMatchingVersionIndex("7.1.0.0");
        if (initialVersionIndex < 0) {
            throw new IllegalStateException("Could not find Flowable version 7.1.0.0");
        }

        for (int i = initialVersionIndex + 1; i < FlowableVersions.FLOWABLE_VERSIONS.size(); i++) {
            String fromVersion = FlowableVersions.FLOWABLE_VERSIONS.get(i - 1).getMainVersion().replace(".", "");
            String toVersion = FlowableVersions.FLOWABLE_VERSIONS.get(i).getMainVersion();
            if (VERSIONS_WITHOUT_HISTORY_UPGRADE.contains(toVersion)) {
                continue;
            }
            String dbSpecificSqlFile = upgradeResourceFormat.formatted(databaseName, fromVersion, toVersion.replace(".", ""));
            ClassPathResource dbSpecificResource = new ClassPathResource(dbSpecificSqlFile);
            if (!dbSpecificResource.exists()) {
                fail("Missing DB-specific history upgrade script file " + dbSpecificSqlFile);
            }
        }
    }

    static Stream<String> databases() {
        return Stream.of("db2", "h2", "mssql", "mysql", "oracle", "postgres");
    }

    static Stream<Arguments> sqlComponentsWithDatabase() {
        return Stream.concat(
                databases()
                        .flatMap(database -> Stream.of(SqlComponent.values()).map(sqlComponent -> Arguments.of(database, sqlComponent))),
                Stream.of(SqlComponent.engine)
                        .flatMap(sqlComponent -> Stream.of(Arguments.of("cockroachdb", sqlComponent)))
        );
    }

    enum SqlComponent {

        common("org/flowable/common/db"),
        engine("org/flowable/db"),
        app("org/flowable/app/db"),
        cmmn("org/flowable/cmmn/db"),
        dmn("org/flowable/dmn/db"),
        eventregistry("org/flowable/eventregistry/db"),
        identity("org/flowable/idm/db");

        private final String dbFolder;

        SqlComponent(String dbFolder) {
            this.dbFolder = dbFolder;
        }

        public String dbFolder() {
            return dbFolder;
        }

    }

}
