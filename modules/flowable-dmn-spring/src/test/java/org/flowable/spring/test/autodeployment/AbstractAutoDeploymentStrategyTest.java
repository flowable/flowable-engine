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

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;

import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnDeploymentBuilder;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;

/**
 * @author Tiese Barrell
 */
public class AbstractAutoDeploymentStrategyTest {

    @Mock
    protected DmnEngine dmnEngineMock;

    @Mock
    protected DmnRepositoryService repositoryServiceMock;

    @Mock
    protected DmnDeploymentBuilder deploymentBuilderMock;

    @Mock
    protected ContextResource resourceMock1;

    @Mock
    protected ByteArrayResource resourceMock2;

    @Mock
    protected Resource resourceMock3;

    @Mock
    protected File fileMock1;

    @Mock
    protected File fileMock2;

    @Mock
    protected File fileMock3;

    @Mock
    private InputStream inputStreamMock;

    @Mock
    private DmnDeployment deploymentMock;

    protected final String deploymentNameHint = "nameHint";

    protected final String resourceName1 = "resourceName1.form";
    protected final String resourceName2 = "resourceName2.form";
    protected final String resourceName3 = "resourceName2.test";

    @BeforeEach
    public void before() throws Exception {

        when(dmnEngineMock.getDmnRepositoryService()).thenReturn(repositoryServiceMock);

        when(resourceMock1.getPathWithinContext()).thenReturn(resourceName1);
        when(resourceMock1.getFile()).thenReturn(fileMock1);

        when(resourceMock2.getDescription()).thenReturn(resourceName2);
        when(resourceMock2.getFile()).thenReturn(fileMock2);

        when(resourceMock3.getFile()).thenReturn(fileMock3);
        when(fileMock3.getAbsolutePath()).thenReturn(resourceName3);

        when(resourceMock1.getInputStream()).thenReturn(inputStreamMock);
        when(resourceMock2.getInputStream()).thenReturn(inputStreamMock);
        when(resourceMock3.getInputStream()).thenReturn(inputStreamMock);

        when(repositoryServiceMock.createDeployment()).thenReturn(deploymentBuilderMock);
        when(deploymentBuilderMock.enableDuplicateFiltering()).thenReturn(deploymentBuilderMock);
        when(deploymentBuilderMock.name(isA(String.class))).thenReturn(deploymentBuilderMock);

        when(deploymentBuilderMock.deploy()).thenReturn(deploymentMock);
    }

}