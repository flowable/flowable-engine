package org.flowable.cmmn.rest.service.api.repository;

import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.rest.service.BaseSpringRestTestCase;
import org.flowable.cmmn.rest.service.api.RestUrls;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for all REST-operations related to the Deployment collection.
 * 
 * @author Tijs Rademakers
 */
public class CaseDefinitionCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting case definitions. GET cmmn-repository/case-definitions
     */
    public void testGetCaseDefinitions() throws Exception {

        try {
            CmmnDeployment firstDeployment = repositoryService.createDeployment().name("Deployment 1").addClasspathResource("org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn").deploy();

            CaseDefinition firstOneTaskCase = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").deploymentId(firstDeployment.getId()).singleResult();
            
            CmmnDeployment secondDeployment = repositoryService.createDeployment().name("Deployment 2").addClasspathResource("org/flowable/cmmn/rest/service/api/repository/oneHumanTaskCase.cmmn")
                    .addClasspathResource("org/flowable/cmmn/rest/service/api/repository/simpleCase.cmmn").deploy();
            
            CmmnDeployment thirdDeployment = repositoryService.createDeployment().name("Deployment 3").addClasspathResource("org/flowable/cmmn/rest/service/api/repository/repeatingStage.cmmn")
                            .addClasspathResource("org/flowable/cmmn/rest/service/api/repository/repeatingStage.cmmn").deploy();

            CaseDefinition oneTaskCase = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("oneHumanTaskCase").deploymentId(secondDeployment.getId()).singleResult();
            
            CaseDefinition simpleCaseDef = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("simpleCase").deploymentId(secondDeployment.getId()).singleResult();
            
            CaseDefinition repeatingStageCase = repositoryService.createCaseDefinitionQuery().caseDefinitionKey("testRepeatingStage").deploymentId(thirdDeployment.getId()).singleResult();
            repositoryService.setCaseDefinitionCategory(repeatingStageCase.getId(), "testCategory");

            // Test parameterless call
            String baseUrl = RestUrls.createRelativeResourceUrl(RestUrls.URL_CASE_DEFINITION_COLLECTION);
            assertResultsPresentInDataResponse(baseUrl, firstOneTaskCase.getId(), oneTaskCase.getId(), simpleCaseDef.getId(), repeatingStageCase.getId());

            // Verify ACT-2141 Persistent isGraphicalNotation flag for case definitions
            CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + baseUrl), HttpStatus.SC_OK);
            JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
            closeResponse(response);
            for (int i = 0; i < dataNode.size(); i++) {
                JsonNode caseDefinitionJson = dataNode.get(i);

                String key = caseDefinitionJson.get("key").asText();
                JsonNode graphicalNotationNode = caseDefinitionJson.get("graphicalNotationDefined");
                if (key.equals("testRepeatingStage")) {
                    assertTrue(graphicalNotationNode.asBoolean());
                } else {
                    assertFalse(graphicalNotationNode.asBoolean());
                }

            }

            // Verify

            // Test name filtering
            String url = baseUrl + "?name=" + encode("Repeating stage");
            assertResultsPresentInDataResponse(url, repeatingStageCase.getId());

            // Test nameLike filtering
            url = baseUrl + "?nameLike=" + encode("Repeating%");
            assertResultsPresentInDataResponse(url, repeatingStageCase.getId());

            // Test key filtering
            url = baseUrl + "?key=testRepeatingStage";
            assertResultsPresentInDataResponse(url, repeatingStageCase.getId());

            // Test keyLike filtering
            url = baseUrl + "?keyLike=" + encode("testRepeating%");
            assertResultsPresentInDataResponse(url, repeatingStageCase.getId());

            // Test category filtering
            url = baseUrl + "?category=testCategory";
            assertResultsPresentInDataResponse(url, repeatingStageCase.getId());

            // Test categoryLike filtering
            url = baseUrl + "?categoryLike=" + encode("testCat%");
            assertResultsPresentInDataResponse(url, repeatingStageCase.getId());

            // Test categoryNotEquals filtering
            url = baseUrl + "?categoryNotEquals=testCategory";
            assertResultsPresentInDataResponse(url, firstOneTaskCase.getId(), oneTaskCase.getId(), simpleCaseDef.getId());

            // Test resourceName filtering
            url = baseUrl + "?resourceName=org/flowable/cmmn/rest/service/api/repository/repeatingStage.cmmn";
            assertResultsPresentInDataResponse(url, repeatingStageCase.getId());

            // Test resourceNameLike filtering
            url = baseUrl + "?resourceNameLike=" + encode("%repeatingStage%");
            assertResultsPresentInDataResponse(url, repeatingStageCase.getId());

            // Test version filtering
            url = baseUrl + "?version=2";
            assertResultsPresentInDataResponse(url, oneTaskCase.getId());

            // Test latest filtering
            url = baseUrl + "?latest=true";
            assertResultsPresentInDataResponse(url, oneTaskCase.getId(), simpleCaseDef.getId(), repeatingStageCase.getId());
            url = baseUrl + "?latest=false";
            assertResultsPresentInDataResponse(baseUrl, firstOneTaskCase.getId(), oneTaskCase.getId(), simpleCaseDef.getId(), repeatingStageCase.getId());

            // Test deploymentId
            url = baseUrl + "?deploymentId=" + secondDeployment.getId();
            assertResultsPresentInDataResponse(url, simpleCaseDef.getId(), oneTaskCase.getId());

        } finally {
            // Always cleanup any created deployments, even if the test failed
            List<CmmnDeployment> deployments = repositoryService.createDeploymentQuery().list();
            for (CmmnDeployment deployment : deployments) {
                repositoryService.deleteDeployment(deployment.getId(), true);
            }
        }
    }
}
