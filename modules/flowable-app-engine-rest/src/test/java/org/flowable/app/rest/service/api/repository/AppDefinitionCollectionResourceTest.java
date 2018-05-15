package org.flowable.app.rest.service.api.repository;

import java.util.List;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.rest.AppRestUrls;
import org.flowable.app.rest.service.BaseSpringRestTestCase;

/**
 * Test for all REST-operations related to the Deployment collection.
 * 
 * @author Tijs Rademakers
 */
public class AppDefinitionCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting case definitions. GET app-repository/app-definitions
     */
    public void testGetAppDefinitions() throws Exception {

        try {
            AppDeployment firstDeployment = repositoryService.createDeployment().name("Deployment 1").addClasspathResource("org/flowable/app/rest/service/api/repository/oneApp.app").deploy();

            AppDefinition firstOneApp = repositoryService.createAppDefinitionQuery().appDefinitionKey("oneApp").deploymentId(firstDeployment.getId()).singleResult();
            
            AppDeployment secondDeployment = repositoryService.createDeployment().name("Deployment 2").addClasspathResource("org/flowable/app/rest/service/api/repository/oneApp.app").deploy();
            
            AppDeployment thirdDeployment = repositoryService.createDeployment().name("Deployment 3").addClasspathResource("org/flowable/app/rest/service/api/repository/secondApp.app").deploy();

            AppDefinition oneApp = repositoryService.createAppDefinitionQuery().appDefinitionKey("oneApp").deploymentId(secondDeployment.getId()).singleResult();
            
            AppDefinition secondApp = repositoryService.createAppDefinitionQuery().appDefinitionKey("secondApp").deploymentId(thirdDeployment.getId()).singleResult();
            repositoryService.setAppDefinitionCategory(secondApp.getId(), "testCategory");

            // Test parameterless call
            String baseUrl = AppRestUrls.createRelativeResourceUrl(AppRestUrls.URL_APP_DEFINITION_COLLECTION);
            assertResultsPresentInDataResponse(baseUrl, firstOneApp.getId(), oneApp.getId(), secondApp.getId());

            // Verify

            // Test name filtering
            String url = baseUrl + "?name=" + encode("Second app");
            assertResultsPresentInDataResponse(url, secondApp.getId());

            // Test nameLike filtering
            url = baseUrl + "?nameLike=" + encode("Second%");
            assertResultsPresentInDataResponse(url, secondApp.getId());

            // Test key filtering
            url = baseUrl + "?key=secondApp";
            assertResultsPresentInDataResponse(url, secondApp.getId());

            // Test keyLike filtering
            url = baseUrl + "?keyLike=" + encode("second%");
            assertResultsPresentInDataResponse(url, secondApp.getId());

            // Test category filtering
            url = baseUrl + "?category=testCategory";
            assertResultsPresentInDataResponse(url, secondApp.getId());

            // Test categoryLike filtering
            url = baseUrl + "?categoryLike=" + encode("testCat%");
            assertResultsPresentInDataResponse(url, secondApp.getId());

            // Test categoryNotEquals filtering
            url = baseUrl + "?categoryNotEquals=testCategory";
            assertResultsPresentInDataResponse(url, firstOneApp.getId(), oneApp.getId());

            // Test resourceName filtering
            url = baseUrl + "?resourceName=org/flowable/app/rest/service/api/repository/secondApp.app";
            assertResultsPresentInDataResponse(url, secondApp.getId());

            // Test resourceNameLike filtering
            url = baseUrl + "?resourceNameLike=" + encode("%secondApp%");
            assertResultsPresentInDataResponse(url, secondApp.getId());

            // Test version filtering
            url = baseUrl + "?version=2";
            assertResultsPresentInDataResponse(url, oneApp.getId());

            // Test latest filtering
            url = baseUrl + "?latest=true";
            assertResultsPresentInDataResponse(url, oneApp.getId(), secondApp.getId());
            url = baseUrl + "?latest=false";
            assertResultsPresentInDataResponse(baseUrl, firstOneApp.getId(), oneApp.getId(), secondApp.getId());

            // Test deploymentId
            url = baseUrl + "?deploymentId=" + secondDeployment.getId();
            assertResultsPresentInDataResponse(url, oneApp.getId());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<AppDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (AppDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }
}
