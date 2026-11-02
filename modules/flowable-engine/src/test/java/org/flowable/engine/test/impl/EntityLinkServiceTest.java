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
package org.flowable.engine.test.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkInfo;
import org.flowable.entitylink.api.EntityLinkService;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.HierarchyType;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.entitylink.api.history.HistoricEntityLinkService;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntity;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
public class EntityLinkServiceTest extends PluggableFlowableTestCase {

    @Test
    void testFindEntityLinksWithSameRootWithNullRoot() {
        managementService.executeCommand(commandContext -> {
            EntityLinkServiceConfiguration entityLinkServiceConfiguration = processEngineConfiguration.getEntityLinkServiceConfiguration();
            EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
            HistoricEntityLinkService historicEntityLinkService = entityLinkServiceConfiguration.getHistoricEntityLinkService();
            createEntityLinkWithoutRootScope("1", "execution1", "element1", "2.1", HierarchyType.ROOT, entityLinkService, historicEntityLinkService);
            createEntityLinkWithoutRootScope("1", "execution2", "element2", "2.2", HierarchyType.ROOT, entityLinkService, historicEntityLinkService);
            createEntityLinkWithoutRootScope("1", "execution3", "element1", "3.1", HierarchyType.ROOT, entityLinkService, historicEntityLinkService);
            createEntityLinkWithoutRootScope("1", "execution4", "element3", "3.2", HierarchyType.ROOT, entityLinkService, historicEntityLinkService);
            createEntityLinkWithoutRootScope("2.1", "execution5", "some1", "3.1", HierarchyType.PARENT, entityLinkService, historicEntityLinkService);
            createEntityLinkWithoutRootScope("2.1", "execution6", "some2", "3.2", HierarchyType.PARENT, entityLinkService, historicEntityLinkService);

            return null;
        });

        assertThat(findEntityLinksByScopeId("1"))
                .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getSubScopeId, EntityLinkInfo::getParentElementId,
                        EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                .containsExactlyInAnyOrder(
                        tuple("1", "execution1", "element1", "2.1", HierarchyType.ROOT),
                        tuple("1", "execution2", "element2", "2.2", HierarchyType.ROOT),
                        tuple("1", "execution3", "element1", "3.1", HierarchyType.ROOT),
                        tuple("1", "execution4", "element3", "3.2", HierarchyType.ROOT)
                );

        assertThat(findEntityLinksByScopeId("2.1"))
                .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getSubScopeId, EntityLinkInfo::getParentElementId,
                        EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                .containsExactlyInAnyOrder(
                        tuple("2.1", "execution5", "some1", "3.1", HierarchyType.PARENT),
                        tuple("2.1", "execution6", "some2", "3.2", HierarchyType.PARENT)
                );

        assertThat(findEntityLinksByScopeId("2.2"))
                .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                .isEmpty();

        assertThat(findEntityLinksWithSameRootByScopeId("1"))
                .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                .isEmpty();

        assertThat(findEntityLinksWithSameRootByScopeId("2.1"))
                .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                .isEmpty();

        assertThat(findHistoricEntityLinksByScopeId("1"))
                .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getSubScopeId, EntityLinkInfo::getParentElementId,
                        EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                .containsExactlyInAnyOrder(
                        tuple("1", "execution1", "element1", "2.1", HierarchyType.ROOT),
                        tuple("1", "execution2", "element2", "2.2", HierarchyType.ROOT),
                        tuple("1", "execution3", "element1", "3.1", HierarchyType.ROOT),
                        tuple("1", "execution4", "element3", "3.2", HierarchyType.ROOT)
                );

        assertThat(findHistoricEntityLinksByScopeId("2.1"))
                .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getSubScopeId, EntityLinkInfo::getParentElementId,
                        EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                .containsExactlyInAnyOrder(
                        tuple("2.1", "execution5", "some1", "3.1", HierarchyType.PARENT),
                        tuple("2.1", "execution6", "some2", "3.2", HierarchyType.PARENT)
                );

        assertThat(findHistoricEntityLinksByScopeId("2.2"))
                .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                .isEmpty();

        assertThat(findHistoricEntityLinksWithSameRootByScopeId("1"))
                .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                .isEmpty();

        assertThat(findHistoricEntityLinksWithSameRootByScopeId("2.1"))
                .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                .isEmpty();

        managementService.executeCommand(commandContext -> {
            EntityLinkServiceConfiguration entityLinkServiceConfiguration = processEngineConfiguration.getEntityLinkServiceConfiguration();
            EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
            entityLinkService.deleteEntityLinksByScopeIdAndType("1", ScopeTypes.BPMN);
            entityLinkService.deleteEntityLinksByScopeIdAndType("2.1", ScopeTypes.BPMN);

            HistoricEntityLinkService historicEntityLinkService = entityLinkServiceConfiguration.getHistoricEntityLinkService();
            historicEntityLinkService.deleteHistoricEntityLinksByScopeIdAndScopeType("1", ScopeTypes.BPMN);
            historicEntityLinkService.deleteHistoricEntityLinksByScopeIdAndScopeType("2.1", ScopeTypes.BPMN);
            return null;
        });
    }

    @Test
    void testFindEntityLinksWithSameRootWithNullRootInSameContext() {
        managementService.executeCommand(commandContext -> {
            EntityLinkServiceConfiguration entityLinkServiceConfiguration = processEngineConfiguration.getEntityLinkServiceConfiguration();
            EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
            HistoricEntityLinkService historicEntityLinkService = entityLinkServiceConfiguration.getHistoricEntityLinkService();
            createEntityLinkWithoutRootScope("1", "execution1", "element1", "2.1", HierarchyType.ROOT, entityLinkService, historicEntityLinkService);
            createEntityLinkWithoutRootScope("1", "execution2", "element2", "2.2", HierarchyType.ROOT, entityLinkService, historicEntityLinkService);
            createEntityLinkWithoutRootScope("1", "execution3", "element1", "3.1", HierarchyType.ROOT, entityLinkService, historicEntityLinkService);
            createEntityLinkWithoutRootScope("1", "execution4", "element3", "3.2", HierarchyType.ROOT, entityLinkService, historicEntityLinkService);
            createEntityLinkWithoutRootScope("2.1", "execution5", "some1", "3.1", HierarchyType.PARENT, entityLinkService, historicEntityLinkService);
            createEntityLinkWithoutRootScope("2.1", "execution6", "some2", "3.2", HierarchyType.PARENT, entityLinkService, historicEntityLinkService);

            assertThat(findEntityLinksByScopeId("1"))
                    .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getSubScopeId, EntityLinkInfo::getParentElementId,
                            EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                    .containsExactlyInAnyOrder(
                            tuple("1", "execution1", "element1", "2.1", HierarchyType.ROOT),
                            tuple("1", "execution2", "element2", "2.2", HierarchyType.ROOT),
                            tuple("1", "execution3", "element1", "3.1", HierarchyType.ROOT),
                            tuple("1", "execution4", "element3", "3.2", HierarchyType.ROOT)
                    );

            assertThat(findEntityLinksByScopeId("2.1"))
                    .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getSubScopeId, EntityLinkInfo::getParentElementId,
                            EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                    .containsExactlyInAnyOrder(
                            tuple("2.1", "execution5", "some1", "3.1", HierarchyType.PARENT),
                            tuple("2.1", "execution6", "some2", "3.2", HierarchyType.PARENT)
                    );

            assertThat(findEntityLinksByScopeId("2.2"))
                    .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                    .isEmpty();

            assertThat(findEntityLinksWithSameRootByScopeId("1"))
                    .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                    .isEmpty();

            assertThat(findEntityLinksWithSameRootByScopeId("2.1"))
                    .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                    .isEmpty();

            assertThat(findHistoricEntityLinksByScopeId("1"))
                    .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getSubScopeId, EntityLinkInfo::getParentElementId,
                            EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                    .containsExactlyInAnyOrder(
                            tuple("1", "execution1", "element1", "2.1", HierarchyType.ROOT),
                            tuple("1", "execution2", "element2", "2.2", HierarchyType.ROOT),
                            tuple("1", "execution3", "element1", "3.1", HierarchyType.ROOT),
                            tuple("1", "execution4", "element3", "3.2", HierarchyType.ROOT)
                    );

            assertThat(findHistoricEntityLinksByScopeId("2.1"))
                    .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getSubScopeId, EntityLinkInfo::getParentElementId,
                            EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                    .containsExactlyInAnyOrder(
                            tuple("2.1", "execution5", "some1", "3.1", HierarchyType.PARENT),
                            tuple("2.1", "execution6", "some2", "3.2", HierarchyType.PARENT)
                    );

            assertThat(findHistoricEntityLinksByScopeId("2.2"))
                    .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                    .isEmpty();

            assertThat(findHistoricEntityLinksWithSameRootByScopeId("1"))
                    .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                    .isEmpty();

            assertThat(findHistoricEntityLinksWithSameRootByScopeId("2.1"))
                    .extracting(EntityLinkInfo::getScopeId, EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getHierarchyType)
                    .isEmpty();
            return null;
        });


        managementService.executeCommand(commandContext -> {
            EntityLinkServiceConfiguration entityLinkServiceConfiguration = processEngineConfiguration.getEntityLinkServiceConfiguration();
            EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
            entityLinkService.deleteEntityLinksByScopeIdAndType("1", ScopeTypes.BPMN);
            entityLinkService.deleteEntityLinksByScopeIdAndType("2.1", ScopeTypes.BPMN);

            HistoricEntityLinkService historicEntityLinkService = entityLinkServiceConfiguration.getHistoricEntityLinkService();
            historicEntityLinkService.deleteHistoricEntityLinksByScopeIdAndScopeType("1", ScopeTypes.BPMN);
            historicEntityLinkService.deleteHistoricEntityLinksByScopeIdAndScopeType("2.1", ScopeTypes.BPMN);
            return null;
        });
    }

    @Test
    void testFindEntityLinkByScopeAndReferenceScopeAndType() {
        managementService.executeCommand(commandContext -> {
            EntityLinkServiceConfiguration entityLinkServiceConfiguration = processEngineConfiguration.getEntityLinkServiceConfiguration();
            EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
            HistoricEntityLinkService historicEntityLinkService = entityLinkServiceConfiguration.getHistoricEntityLinkService();
            createEntityLinkWithScopeAndRefScopeAndType("1", ScopeTypes.BPMN, "1", ScopeTypes.CMMN, "test", entityLinkServiceConfiguration);
            createEntityLinkWithScopeAndRefScopeAndType("1", ScopeTypes.BPMN, "1", ScopeTypes.CMMN, "other-test", entityLinkServiceConfiguration);
            createEntityLinkWithScopeAndRefScopeAndType("1", ScopeTypes.BPMN, "2", ScopeTypes.CMMN, "test", entityLinkServiceConfiguration);
            createEntityLinkWithScopeAndRefScopeAndType("2", ScopeTypes.BPMN, "1", ScopeTypes.CMMN, "test", entityLinkServiceConfiguration);
            createEntityLinkWithScopeAndRefScopeAndType("1", ScopeTypes.CMMN, "1", ScopeTypes.BPMN, "some-test", entityLinkServiceConfiguration);

            assertThat(entityLinkService.createInternalEntityLinkQuery()
                            .scopeId("1").scopeType(ScopeTypes.BPMN)
                            .referenceScopeId("1").referenceScopeType(ScopeTypes.CMMN)
                            .linkType("test")
                            .singleResult()).isNotNull();
            assertThat(historicEntityLinkService.createInternalHistoricEntityLinkQuery()
                            .scopeId("1").scopeType(ScopeTypes.BPMN)
                            .referenceScopeId("1").referenceScopeType(ScopeTypes.CMMN)
                            .linkType("test")
                            .singleResult()).isNotNull();

            assertThat(entityLinkService.createInternalEntityLinkQuery()
                            .scopeId("1").scopeType(ScopeTypes.BPMN)
                            .referenceScopeId("1").referenceScopeType(ScopeTypes.CMMN)
                            .linkType("dummy")
                            .singleResult()).isNull();
            assertThat(historicEntityLinkService.createInternalHistoricEntityLinkQuery()
                            .scopeId("1").scopeType(ScopeTypes.BPMN)
                            .referenceScopeId("1").referenceScopeType(ScopeTypes.CMMN)
                            .linkType("dummy")
                            .singleResult()).isNull();

            return null;
        });

        managementService.executeCommand(commandContext -> {
            EntityLinkServiceConfiguration entityLinkServiceConfiguration = processEngineConfiguration.getEntityLinkServiceConfiguration();
            EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
            HistoricEntityLinkService historicEntityLinkService = entityLinkServiceConfiguration.getHistoricEntityLinkService();
            assertThat(entityLinkService.createInternalEntityLinkQuery()
                    .scopeId("1").scopeType(ScopeTypes.BPMN)
                    .referenceScopeId("1").referenceScopeType(ScopeTypes.CMMN)
                    .linkType("test")
                    .singleResult()).isNotNull();
            assertThat(historicEntityLinkService.createInternalHistoricEntityLinkQuery()
                    .scopeId("1").scopeType(ScopeTypes.BPMN)
                    .referenceScopeId("1").referenceScopeType(ScopeTypes.CMMN)
                    .linkType("test")
                    .singleResult()).isNotNull();

            assertThat(entityLinkService.createInternalEntityLinkQuery()
                    .scopeId("1").scopeType(ScopeTypes.BPMN)
                    .referenceScopeId("1").referenceScopeType(ScopeTypes.CMMN)
                    .linkType("dummy")
                    .singleResult()).isNull();
            assertThat(historicEntityLinkService.createInternalHistoricEntityLinkQuery()
                    .scopeId("1").scopeType(ScopeTypes.BPMN)
                    .referenceScopeId("1").referenceScopeType(ScopeTypes.CMMN)
                    .linkType("dummy")
                    .singleResult()).isNull();

            return null;
        });

        managementService.executeCommand(commandContext -> {
            EntityLinkServiceConfiguration entityLinkServiceConfiguration = processEngineConfiguration.getEntityLinkServiceConfiguration();
            EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
            entityLinkService.deleteEntityLinksByScopeIdAndType("1", ScopeTypes.BPMN);
            entityLinkService.deleteEntityLinksByScopeIdAndType("2", ScopeTypes.BPMN);
            entityLinkService.deleteEntityLinksByScopeIdAndType("1", ScopeTypes.CMMN);

            HistoricEntityLinkService historicEntityLinkService = entityLinkServiceConfiguration.getHistoricEntityLinkService();
            historicEntityLinkService.deleteHistoricEntityLinksByScopeIdAndScopeType("1", ScopeTypes.BPMN);
            historicEntityLinkService.deleteHistoricEntityLinksByScopeIdAndScopeType("2", ScopeTypes.BPMN);
            historicEntityLinkService.deleteHistoricEntityLinksByScopeIdAndScopeType("1", ScopeTypes.CMMN);
            return null;
        });
    }

    @Test
    void testFindEntityLinksByScopeIds() {
        managementService.executeCommand(commandContext -> {
            EntityLinkServiceConfiguration entityLinkServiceConfiguration = processEngineConfiguration.getEntityLinkServiceConfiguration();
            EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
            HistoricEntityLinkService historicEntityLinkService = entityLinkServiceConfiguration.getHistoricEntityLinkService();
            createEntityLinkWithScopeAndRefScopeAndType("1", ScopeTypes.BPMN, "1", ScopeTypes.CMMN, "test", entityLinkServiceConfiguration);
            createEntityLinkWithScopeAndRefScopeAndType("1", ScopeTypes.BPMN, "1", ScopeTypes.CMMN, "other-test", entityLinkServiceConfiguration);
            createEntityLinkWithScopeAndRefScopeAndType("1", ScopeTypes.BPMN, "2", ScopeTypes.CMMN, "test", entityLinkServiceConfiguration);
            createEntityLinkWithScopeAndRefScopeAndType("2", ScopeTypes.BPMN, "1", ScopeTypes.CMMN, "test", entityLinkServiceConfiguration);
            createEntityLinkWithScopeAndRefScopeAndType("1", ScopeTypes.CMMN, "1", ScopeTypes.BPMN, "some-test", entityLinkServiceConfiguration);
            createEntityLinkWithScopeAndRefScopeAndType("3", ScopeTypes.BPMN, "2", ScopeTypes.CMMN, "other-test", entityLinkServiceConfiguration);

            assertThat(entityLinkService.createInternalEntityLinkQuery()
                            .scopeIds(List.of("1", "2")).scopeType(ScopeTypes.BPMN)
                            .list())
                    .extracting(EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getReferenceScopeType, EntityLinkInfo::getLinkType)
                    .containsExactlyInAnyOrder(
                            tuple("1", ScopeTypes.CMMN, "test"),
                            tuple("1", ScopeTypes.CMMN, "other-test"),
                            tuple("2", ScopeTypes.CMMN, "test"),
                            tuple("1", ScopeTypes.CMMN, "test")
                    );
            assertThat(historicEntityLinkService.createInternalHistoricEntityLinkQuery()
                    .scopeIds(List.of("1", "2")).scopeType(ScopeTypes.BPMN)
                    .list())
                    .extracting(EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getReferenceScopeType, EntityLinkInfo::getLinkType)
                    .containsExactlyInAnyOrder(
                            tuple("1", ScopeTypes.CMMN, "test"),
                            tuple("1", ScopeTypes.CMMN, "other-test"),
                            tuple("2", ScopeTypes.CMMN, "test"),
                            tuple("1", ScopeTypes.CMMN, "test")
                    );
            return null;
        });

        managementService.executeCommand(commandContext -> {
            EntityLinkServiceConfiguration entityLinkServiceConfiguration = processEngineConfiguration.getEntityLinkServiceConfiguration();
            EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
            HistoricEntityLinkService historicEntityLinkService = entityLinkServiceConfiguration.getHistoricEntityLinkService();
            assertThat(entityLinkService.createInternalEntityLinkQuery()
                    .scopeIds(List.of("1", "2")).scopeType(ScopeTypes.BPMN)
                    .list())
                    .extracting(EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getReferenceScopeType, EntityLinkInfo::getLinkType)
                    .containsExactlyInAnyOrder(
                            tuple("1", ScopeTypes.CMMN, "test"),
                            tuple("1", ScopeTypes.CMMN, "other-test"),
                            tuple("2", ScopeTypes.CMMN, "test"),
                            tuple("1", ScopeTypes.CMMN, "test")
                    );
            assertThat(historicEntityLinkService.createInternalHistoricEntityLinkQuery()
                    .scopeIds(List.of("1", "2")).scopeType(ScopeTypes.BPMN)
                    .list())
                    .extracting(EntityLinkInfo::getReferenceScopeId, EntityLinkInfo::getReferenceScopeType, EntityLinkInfo::getLinkType)
                    .containsExactlyInAnyOrder(
                            tuple("1", ScopeTypes.CMMN, "test"),
                            tuple("1", ScopeTypes.CMMN, "other-test"),
                            tuple("2", ScopeTypes.CMMN, "test"),
                            tuple("1", ScopeTypes.CMMN, "test")
                    );

            return null;
        });

        managementService.executeCommand(commandContext -> {
            EntityLinkServiceConfiguration entityLinkServiceConfiguration = processEngineConfiguration.getEntityLinkServiceConfiguration();
            EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
            entityLinkService.deleteEntityLinksByScopeIdAndType("1", ScopeTypes.BPMN);
            entityLinkService.deleteEntityLinksByScopeIdAndType("2", ScopeTypes.BPMN);
            entityLinkService.deleteEntityLinksByScopeIdAndType("1", ScopeTypes.CMMN);
            entityLinkService.deleteEntityLinksByScopeIdAndType("3", ScopeTypes.BPMN);

            HistoricEntityLinkService historicEntityLinkService = entityLinkServiceConfiguration.getHistoricEntityLinkService();
            historicEntityLinkService.deleteHistoricEntityLinksByScopeIdAndScopeType("1", ScopeTypes.BPMN);
            historicEntityLinkService.deleteHistoricEntityLinksByScopeIdAndScopeType("2", ScopeTypes.BPMN);
            historicEntityLinkService.deleteHistoricEntityLinksByScopeIdAndScopeType("1", ScopeTypes.CMMN);
            historicEntityLinkService.deleteHistoricEntityLinksByScopeIdAndScopeType("3", ScopeTypes.BPMN);
            return null;
        });
    }


    protected List<EntityLink> findEntityLinksByScopeId(String scopeId) {
        return managementService.executeCommand(commandContext -> processEngineConfiguration.getEntityLinkServiceConfiguration().getEntityLinkService()
                .findEntityLinksByScopeIdAndType(scopeId, ScopeTypes.BPMN, EntityLinkType.CHILD));
    }

    protected List<EntityLink> findEntityLinksWithSameRootByScopeId(String scopeId) {
        return managementService.executeCommand(commandContext -> processEngineConfiguration.getEntityLinkServiceConfiguration().getEntityLinkService()
                .findEntityLinksWithSameRootScopeForScopeIdAndScopeType(scopeId, ScopeTypes.BPMN, EntityLinkType.CHILD));
    }

    protected List<HistoricEntityLink> findHistoricEntityLinksByScopeId(String scopeId) {
        return managementService.executeCommand(commandContext -> processEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService()
                .findHistoricEntityLinksByScopeIdAndScopeType(scopeId, ScopeTypes.BPMN, EntityLinkType.CHILD));
    }

    protected List<HistoricEntityLink> findHistoricEntityLinksWithSameRootByScopeId(String scopeId) {
        return managementService.executeCommand(commandContext -> processEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService()
                .findHistoricEntityLinksWithSameRootScopeForScopeIdAndScopeType(scopeId, ScopeTypes.BPMN, EntityLinkType.CHILD));
    }

    protected void createEntityLinkWithoutRootScope(String scopeId, String subScopeId, String parentElementId,
            String referenceId, String hierarchyType, EntityLinkService entityLinkService,
            HistoricEntityLinkService historicEntityLinkService) {

        EntityLinkEntity entityLink = (EntityLinkEntity) entityLinkService.createEntityLink();
        entityLink.setScopeId(scopeId);
        entityLink.setSubScopeId(subScopeId);
        entityLink.setScopeType(ScopeTypes.BPMN);
        entityLink.setLinkType(EntityLinkType.CHILD);
        entityLink.setParentElementId(parentElementId);
        entityLink.setReferenceScopeId(referenceId);
        entityLink.setReferenceScopeType(ScopeTypes.BPMN);
        entityLink.setHierarchyType(hierarchyType);
        entityLinkService.insertEntityLink(entityLink);

        HistoricEntityLinkEntity historicEntityLink = (HistoricEntityLinkEntity) historicEntityLinkService.createHistoricEntityLink();
        historicEntityLink.setScopeId(scopeId);
        historicEntityLink.setSubScopeId(subScopeId);
        historicEntityLink.setScopeType(ScopeTypes.BPMN);
        historicEntityLink.setLinkType(EntityLinkType.CHILD);
        historicEntityLink.setParentElementId(parentElementId);
        historicEntityLink.setReferenceScopeId(referenceId);
        historicEntityLink.setReferenceScopeType(ScopeTypes.BPMN);
        historicEntityLink.setHierarchyType(hierarchyType);
        historicEntityLinkService.insertHistoricEntityLink(historicEntityLink, false);
    }

    protected void createEntityLinkWithScopeAndRefScopeAndType(String scopeId, String scopeType, String referenceScopeId, String referenceScopeType,
            String linkType, EntityLinkServiceConfiguration entityLinkServiceConfiguration) {

        EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
        EntityLinkEntity entityLink = (EntityLinkEntity) entityLinkService.createEntityLink();
        entityLink.setScopeId(scopeId);
        entityLink.setScopeType(scopeType);
        entityLink.setReferenceScopeId(referenceScopeId);
        entityLink.setReferenceScopeType(referenceScopeType);
        entityLink.setLinkType(linkType);
        entityLinkService.insertEntityLink(entityLink);

        HistoricEntityLinkService historicEntityLinkService = entityLinkServiceConfiguration.getHistoricEntityLinkService();
        HistoricEntityLinkEntity historicEntityLink = (HistoricEntityLinkEntity) historicEntityLinkService.createHistoricEntityLink();
        historicEntityLink.setScopeId(scopeId);
        historicEntityLink.setScopeType(scopeType);
        historicEntityLink.setReferenceScopeId(referenceScopeId);
        historicEntityLink.setReferenceScopeType(referenceScopeType);
        historicEntityLink.setLinkType(linkType);
        historicEntityLinkService.insertHistoricEntityLink(historicEntityLink, false);
    }

}
