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
package org.flowable.cmmn.api.migration;

import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;

public interface CaseInstanceMigrationCallback {

    /**
     * @deprecated use {@link #caseInstanceMigrated(CaseInstance, CaseDefinition, CaseDefinition, CaseInstanceMigrationDocument)} instead.
     */
    @Deprecated
    void caseInstanceMigrated(CaseInstance caseInstance, CaseDefinition caseDefToMigrateTo, CaseInstanceMigrationDocument document);

    /**
     * Called after a case instance has been migrated. This is the method invoked by the engine;
     * the default implementation delegates to {@link #caseInstanceMigrated(CaseInstance, CaseDefinition, CaseInstanceMigrationDocument)}.
     *
     * @param sourceCaseDefinition the case definition the case instance was on before the migration,
     *          or {@code null} when that definition no longer exists
     */
    default void caseInstanceMigrated(CaseInstance caseInstance, CaseDefinition sourceCaseDefinition, CaseDefinition caseDefToMigrateTo,
            CaseInstanceMigrationDocument document) {
        caseInstanceMigrated(caseInstance, caseDefToMigrateTo, document);
    }

    /**
     * @deprecated use {@link #historicCaseInstanceMigrated(HistoricCaseInstance, CaseDefinition, CaseDefinition, HistoricCaseInstanceMigrationDocument)} instead.
     */
    @Deprecated
    void historicCaseInstanceMigrated(HistoricCaseInstance caseInstance, CaseDefinition caseDefToMigrateTo, HistoricCaseInstanceMigrationDocument document);

    /**
     * Called after a historic case instance has been migrated. This is the method invoked by the engine;
     * the default implementation delegates to {@link #historicCaseInstanceMigrated(HistoricCaseInstance, CaseDefinition, HistoricCaseInstanceMigrationDocument)}.
     *
     * @param sourceCaseDefinition the case definition the historic case instance was on before the migration,
     *          or {@code null} when that definition no longer exists
     */
    default void historicCaseInstanceMigrated(HistoricCaseInstance caseInstance, CaseDefinition sourceCaseDefinition, CaseDefinition caseDefToMigrateTo,
            HistoricCaseInstanceMigrationDocument document) {
        historicCaseInstanceMigrated(caseInstance, caseDefToMigrateTo, document);
    }

}
