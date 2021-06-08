package org.flowable.content.engine.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.content.api.ContentItem;
import org.junit.Test;

public class ChangeTenantIdContentItemInstanceTest extends AbstractFlowableContentTest {

    private static final String TEST_TENANT_A = "test-tenant-a";
    private static final String TEST_TENANT_B = "test-tenant-b";
    private static final String TEST_TENANT_C = "test-tenant-c";

    @Test
    public void testChangeTenantIdContentItemInstance() {
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

        // The simulation result must match the actual result
        assertThat(simulationResult).as("The simulation result must match the actual result.").isEqualTo(result).as("The simulation result must match the actual result.");

    }

    @Test
    public void check_onlyInstancesFromDefaultTenantDefinitions_set_to_true_throws_UnsupportedOperationException() {
        assertThatThrownBy(() -> contentManagementService.createChangeTenantIdBuilder(TEST_TENANT_A, TEST_TENANT_B)
            .onlyInstancesFromDefaultTenantDefinitions(true)
            .complete())
        .isInstanceOf(UnsupportedOperationException.class);

    }

    private void checkContentItemInTenant(String contentItemId, String expectedTenantId, String moment) {
        ContentItem contentItem = contentService.createContentItemQuery().id(contentItemId).singleResult();
        assertThat(contentItem).isNotNull();
        assertThat(contentItem.getTenantId())
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
