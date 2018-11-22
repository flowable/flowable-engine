package org.flowable.idm.engine.test.api.identity;

import org.flowable.idm.engine.test.ResourceFlowableIdmTestCase;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.Assert.assertNotNull;


public class GetPropertiesTest extends ResourceFlowableIdmTestCase {

    public GetPropertiesTest() {
        super("flowable.idm.cfg.xml");
    }
    @Test
    public void testIdmManagementServiceGetProperties() {
        Map<String, String> properties = idmManagementService.getProperties();
        assertNotNull(properties);
    }

}
