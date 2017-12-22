package org.flowable.camel.cdi.named;

import org.flowable.cdi.test.BaseCdiFlowableTestCase;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test base class is for supporting named contexts. The default camel context in a CDI environment is not named. A factory class is provided to name and construct the context.
 * 
 * @author Zach Visagie
 *
 */
public abstract class NamedCamelCdiFlowableTestCase extends BaseCdiFlowableTestCase {
    protected static final Logger LOGGER = LoggerFactory.getLogger(NamedCamelCdiFlowableTestCase.class);
    
    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class).addPackages(true, "org.flowable.cdi").addPackages(true, "org.flowable.camel.cdi.named").addAsManifestResource("META-INF/beans.xml", "beans.xml");
    }

}
