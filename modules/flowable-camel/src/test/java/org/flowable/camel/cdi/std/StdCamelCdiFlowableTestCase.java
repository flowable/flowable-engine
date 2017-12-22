package org.flowable.camel.cdi.std;

import org.flowable.cdi.test.BaseCdiFlowableTestCase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test base class is for supporting the default camel context which is not a named bean in a CDI environment.
 * 
 * @author Zach Visagie
 *
 */
public abstract class StdCamelCdiFlowableTestCase extends BaseCdiFlowableTestCase {
    protected static final Logger LOGGER = LoggerFactory.getLogger(StdCamelCdiFlowableTestCase.class);

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class).addPackages(true, "org.flowable.cdi").addPackages(true, "org.flowable.camel.cdi.std").addAsManifestResource("META-INF/beans.xml", "beans.xml");
    }

}
