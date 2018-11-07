package org.flowable.spring.impl.test;

import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * Removes all deployments at the end of a complete test class.
 * <p>
 * Use this as follows in a Spring test:
 * 
 * @author jbarrez @RunWith(SpringJUnit4ClassRunner.class) @TestExecutionListeners(CleanTestExecutionListener.class) @ContextConfiguration("...")
 */
public class CleanTestExecutionListener extends AbstractTestExecutionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CleanTestExecutionListener.class);

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        RepositoryService repositoryService = testContext.getApplicationContext().getBean(RepositoryService.class);
        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            try {
                repositoryService.deleteDeployment(deployment.getId(), true);
            } catch (FlowableOptimisticLockingException flowableOptimisticLockingException) {
                LOGGER.warn("Caught exception, retrying", flowableOptimisticLockingException);
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }

}
