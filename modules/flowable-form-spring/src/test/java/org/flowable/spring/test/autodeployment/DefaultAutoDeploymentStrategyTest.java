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

package org.flowable.spring.test.autodeployment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.form.spring.autodeployment.DefaultAutoDeploymentStrategy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.Resource;

/**
 * @author Tiese Barrell
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class DefaultAutoDeploymentStrategyTest extends AbstractAutoDeploymentStrategyTest {

    private DefaultAutoDeploymentStrategy classUnderTest;

    @Before
    @Override
    public void before() throws Exception {
        super.before();
        classUnderTest = new DefaultAutoDeploymentStrategy();
        assertNotNull(classUnderTest);
    }

    @Test
    public void testHandlesMode() {
        assertTrue(classUnderTest.handlesMode(DefaultAutoDeploymentStrategy.DEPLOYMENT_MODE));
        assertFalse(classUnderTest.handlesMode("other-mode"));
        assertFalse(classUnderTest.handlesMode(null));
    }

    @Test
    public void testDeployResources() {
        final Resource[] resources = new Resource[] { resourceMock1, resourceMock2 };
        classUnderTest.deployResources(deploymentNameHint, resources, repositoryServiceMock);

        verify(repositoryServiceMock, times(1)).createDeployment();
        verify(deploymentBuilderMock, times(1)).enableDuplicateFiltering();
        verify(deploymentBuilderMock, times(1)).name(deploymentNameHint);
        verify(deploymentBuilderMock, times(1)).addInputStream(eq(resourceName1), isA(InputStream.class));
        verify(deploymentBuilderMock, times(1)).addInputStream(eq(resourceName2), isA(InputStream.class));
        verify(deploymentBuilderMock, times(1)).deploy();
    }

    @Test
    public void testDeployResourcesNoResources() {
        final Resource[] resources = new Resource[] {};
        classUnderTest.deployResources(deploymentNameHint, resources, repositoryServiceMock);

        verify(repositoryServiceMock, times(1)).createDeployment();
        verify(deploymentBuilderMock, times(1)).enableDuplicateFiltering();
        verify(deploymentBuilderMock, times(1)).name(deploymentNameHint);
        verify(deploymentBuilderMock, never()).addInputStream(isA(String.class), isA(InputStream.class));
        verify(deploymentBuilderMock, never()).addInputStream(eq(resourceName2), isA(InputStream.class));
        verify(deploymentBuilderMock, times(1)).deploy();
    }

    @Test(expected = FlowableException.class)
    public void testDeployResourcesIOExceptionYieldsFlowableException() throws Exception {
        when(resourceMock3.getInputStream()).thenThrow(new IOException());

        final Resource[] resources = new Resource[] { resourceMock3 };
        classUnderTest.deployResources(deploymentNameHint, resources, repositoryServiceMock);

        fail("Expected exception for IOException");
    }

    @Test
    public void testDetermineResourceNameWithExceptionFailsGracefully() throws Exception {
        when(resourceMock3.getFile()).thenThrow(new IOException());
        when(resourceMock3.getFilename()).thenReturn(resourceName3);

        final Resource[] resources = new Resource[] { resourceMock3 };
        classUnderTest.deployResources(deploymentNameHint, resources, repositoryServiceMock);
    }

}