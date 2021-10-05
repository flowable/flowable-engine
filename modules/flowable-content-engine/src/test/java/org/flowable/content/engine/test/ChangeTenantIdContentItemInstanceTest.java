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
package org.flowable.content.engine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.content.api.ContentChangeTenantIdEntityTypes.CONTENT_ITEM_INSTANCES;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.content.api.ContentItem;
import org.junit.Test;

public class ChangeTenantIdContentItemInstanceTest extends AbstractFlowableContentTest {

    private static final String TEST_TENANT_A = "test-tenant-a";
    private static final String TEST_TENANT_B = "test-tenant-b";
    private static final String TEST_TENANT_C = "test-tenant-c";

    @Test
    public void changeTenantIdContentItemInstance() {
        // Creating content Items
        String contentItemIdA = createContentItem(TEST_TENANT_A);
        String contentItemIdB = createContentItem(TEST_TENANT_B);
        String contentItemIdC = createContentItem(TEST_TENANT_C);

        checkContentItemInTenant(contentItemIdA, TEST_TENANT_A, "prior to changing to " + TEST_TENANT_B);
        checkContentItemInTenant(contentItemIdB, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkContentItemInTenant(contentItemIdC, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        ChangeTenantIdResult simulationResult = contentManagementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkContentItemInTenant(contentItemIdA, TEST_TENANT_A, "after simulating the change to " + TEST_TENANT_B);
        checkContentItemInTenant(contentItemIdB, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkContentItemInTenant(contentItemIdC, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        ChangeTenantIdResult result = contentManagementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B).complete();

        // All the A instances should now be assigned to the tenant B
        checkContentItemInTenant(contentItemIdA, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        // The instances from B remain in B and the ones from C in C
        checkContentItemInTenant(contentItemIdB, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkContentItemInTenant(contentItemIdC, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        deleteContentItem(contentItemIdA);
        deleteContentItem(contentItemIdB);
        deleteContentItem(contentItemIdC);

        //Expected results map
        Map<String, Long> resultMap = Collections.singletonMap(CONTENT_ITEM_INSTANCES, 1L);

        //Check that all the entities are returned
        assertThat(simulationResult.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());
        assertThat(result.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());

        resultMap.forEach((key, value) -> {
            //Check simulation result content
            assertThat(simulationResult.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);

            //Check result content
            assertThat(result.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);
        });
    }

    @Test
    public void changeTenantIdContentItemInstanceFromEmptyTenant() {
        // Creating content Items
        String contentItemIdNoTenant = createContentItem("");
        String contentItemIdB = createContentItem(TEST_TENANT_B);
        String contentItemIdC = createContentItem(TEST_TENANT_C);

        checkContentItemInTenant(contentItemIdNoTenant, "", "prior to changing to " + TEST_TENANT_B);
        checkContentItemInTenant(contentItemIdB, TEST_TENANT_B, "prior to changing to " + TEST_TENANT_B);
        checkContentItemInTenant(contentItemIdC, TEST_TENANT_C, "prior to changing to " + TEST_TENANT_B);

        ChangeTenantIdResult simulationResult = contentManagementService.createChangeTenantIdBuilder("", TEST_TENANT_B).simulate();

        // All the instances should stay in the original tenant after the simulation
        checkContentItemInTenant(contentItemIdNoTenant, "", "after simulating the change to " + TEST_TENANT_B);
        checkContentItemInTenant(contentItemIdB, TEST_TENANT_B, "after simulating the change to " + TEST_TENANT_B);
        checkContentItemInTenant(contentItemIdC, TEST_TENANT_C, "after simulating the change to " + TEST_TENANT_B);

        ChangeTenantIdResult result = contentManagementService.createChangeTenantIdBuilder("", TEST_TENANT_B).complete();

        // All the A instances should now be assigned to the tenant B
        checkContentItemInTenant(contentItemIdNoTenant, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        // The instances from B remain in B and the ones from C in C
        checkContentItemInTenant(contentItemIdB, TEST_TENANT_B, "after the change to " + TEST_TENANT_B);
        checkContentItemInTenant(contentItemIdC, TEST_TENANT_C, "after the change to " + TEST_TENANT_B);

        deleteContentItem(contentItemIdNoTenant);
        deleteContentItem(contentItemIdB);
        deleteContentItem(contentItemIdC);

        //Expected results map
        Map<String, Long> resultMap = Collections.singletonMap(CONTENT_ITEM_INSTANCES, 1L);

        //Check that all the entities are returned
        assertThat(simulationResult.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());
        assertThat(result.getChangedEntityTypes())
                .containsExactlyInAnyOrderElementsOf(resultMap.keySet());

        resultMap.forEach((key, value) -> {
            //Check simulation result content
            assertThat(simulationResult.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);

            //Check result content
            assertThat(result.getChangedInstances(key))
                    .as(key)
                    .isEqualTo(value);
        });
    }

    @Test
    public void changeTenantIdWhenSourceAndTargetAreEqual() {
        assertThatThrownBy(() -> contentManagementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_A).simulate())
                .isInstanceOf(FlowableIllegalArgumentException.class);
        assertThatThrownBy(() -> contentManagementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_A).complete())
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    private void checkContentItemInTenant(String contentItemId, String expectedTenantId, String moment) {
        ContentItem contentItem = contentService.createContentItemQuery().id(contentItemId).singleResult();
        assertThat(contentItem).isNotNull();
        assertThat(StringUtils.defaultIfEmpty(contentItem.getTenantId(), ""))
                .as("ContentItem instance %s belongs to tenant %s instead of %s %s", contentItem.getId(), contentItem.getTenantId(), expectedTenantId, moment)
                .isEqualTo(expectedTenantId);
    }

    private String createContentItem(String tenantId) {
        ContentItem contentItem = contentService.newContentItem();
        contentItem.setName("testItem");
        contentItem.setMimeType("application/pdf");
        contentItem.setProcessInstanceId("123456");
        contentItem.setTenantId(tenantId);
        contentService.saveContentItem(contentItem);
        return contentItem.getId();
    }

    private void deleteContentItem(String contentItemId) {
        contentService.deleteContentItem(contentItemId);
    }

}