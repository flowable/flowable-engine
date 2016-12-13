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
package org.flowable.app.service.editor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.AppDefinition;
import org.flowable.app.domain.editor.AppModelDefinition;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.repository.editor.ModelRepository;
import org.flowable.app.service.api.AppDefinitionService;
import org.flowable.app.service.api.ModelService;
import org.flowable.app.service.exception.BadRequestException;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.bpmn.model.UserTask;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.xml.converter.DmnXMLConverter;
import org.flowable.editor.dmn.converter.DmnJsonConverter;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Can't merge this with {@link AppDefinitionService}, as it doesn't have visibility of domain models needed to do the publication.
 * 
 * @author jbarrez
 */
@Service
@Transactional
public class AppDefinitionPublishService {

  private static final Logger logger = LoggerFactory.getLogger(AppDefinitionPublishService.class);

  @Autowired
  protected ModelService modelService;

  @Autowired
  protected ModelRepository modelRepository;
  
  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected Environment environment;
  
  protected DmnJsonConverter dmnJsonConverter = new DmnJsonConverter();
  protected DmnXMLConverter dmnXMLConverter = new DmnXMLConverter();

  public void publishAppDefinition(String comment, Model appDefinitionModel, User user) {

    // Create new version of the app model
    modelService.createNewModelVersion(appDefinitionModel, comment, user);

    AppDefinition appDefinition = resolveAppDefinition(appDefinitionModel);

    if (CollectionUtils.isNotEmpty(appDefinition.getModels())) {

      String deployableZipName = appDefinitionModel.getKey() + ".zip";
      Map<String, byte[]> deployableAssets = new HashMap<>();

      String appDefinitionJson = getAppDefinitionJson(appDefinition);
      byte[] appDefinitionJsonBytes = appDefinitionJson.getBytes(StandardCharsets.UTF_8);

      deployableAssets.put(appDefinitionModel.getKey() + ".app", appDefinitionJsonBytes);
      
      Map<String, Model> formMap = new HashMap<String, Model>();
      Map<String, Model> decisionTableMap = new HashMap<String, Model>();

      for (AppModelDefinition appModelDef : appDefinition.getModels()) {

        AbstractModel processModel = modelService.getModel(appModelDef.getId());
        if (processModel == null) {
          logger.error("Model {} for app definition {} could not be found", appModelDef.getId(), appDefinitionModel.getId());
          throw new BadRequestException("Model for app definition could not be found");
        }
        
        List<Model> referencedModels = modelRepository.findByParentModelId(processModel.getId());
        for (Model childModel : referencedModels) {
          if (Model.MODEL_TYPE_FORM == childModel.getModelType()) {
            formMap.put(childModel.getId(), childModel);
            
          } else if (Model.MODEL_TYPE_DECISION_TABLE == childModel.getModelType()) {
            decisionTableMap.put(childModel.getId(), childModel);
          }
        }

        BpmnModel bpmnModel = modelService.getBpmnModel(processModel, formMap, decisionTableMap);
        Map<String, StartEvent> startEventMap = processNoneStartEvents(bpmnModel);
        
        for (Process process : bpmnModel.getProcesses()) {
          processUserTasks(process.getFlowElements(), process, startEventMap);
        }
        
        byte[] modelXML = modelService.getBpmnXML(bpmnModel);
        deployableAssets.put(processModel.getKey().replaceAll(" ", "") + ".bpmn", modelXML);
      }
      
      if (formMap.size() > 0) {
        for (String formId : formMap.keySet()) {
          Model formInfo = formMap.get(formId);
          String formModelEditorJson = formInfo.getModelEditorJson();
          byte[] formModelEditorJsonBytes = formModelEditorJson.getBytes(StandardCharsets.UTF_8);
          deployableAssets.put("form-" + formInfo.getKey() + ".form", formModelEditorJsonBytes);
        }
      }
      
      if (decisionTableMap.size() > 0) {
        for (String decisionTableId : decisionTableMap.keySet()) {
          Model decisionTableInfo = decisionTableMap.get(decisionTableId);
          try {
            JsonNode decisionTableNode = objectMapper.readTree(decisionTableInfo.getModelEditorJson());
            DmnDefinition dmnDefinition = dmnJsonConverter.convertToDmn(decisionTableNode, decisionTableInfo.getId(),
                decisionTableInfo.getVersion(), decisionTableInfo.getLastUpdated());
            byte[] dmnXMLBytes = dmnXMLConverter.convertToXML(dmnDefinition);
            deployableAssets.put("dmn-" + decisionTableInfo.getKey() + ".dmn", dmnXMLBytes);
          } catch (Exception e) {
            logger.error("Error converting decision table to XML " + decisionTableInfo.getName(), e);
            throw new InternalServerErrorException("Error converting decision table to XML " + decisionTableInfo.getName());
          }
        }
      }

      byte[] deployZipArtifact = createDeployZipArtifact(deployableAssets);
      if (deployZipArtifact != null) {
        deployZipArtifact(deployableZipName, deployZipArtifact);
      }
    }
  }
  
  protected Map<String, StartEvent> processNoneStartEvents(BpmnModel bpmnModel) {
    Map<String, StartEvent> startEventMap = new HashMap<String, StartEvent>();
    for (Process process : bpmnModel.getProcesses()) {
      for (FlowElement flowElement : process.getFlowElements()) {
        if (flowElement instanceof StartEvent) {
          StartEvent startEvent = (StartEvent) flowElement;
          if (CollectionUtils.isEmpty(startEvent.getEventDefinitions())) {
            if (StringUtils.isEmpty(startEvent.getInitiator())) {
              startEvent.setInitiator("initiator");
            }
            startEventMap.put(process.getId(), startEvent);
            break;
          }
        }
      }
    }
    return startEventMap;
  }
  
  protected void processUserTasks(Collection<FlowElement> flowElements, Process process, Map<String, StartEvent> startEventMap) {

    for (FlowElement flowElement : flowElements) {
      if (flowElement instanceof UserTask) {
        UserTask userTask = (UserTask) flowElement;
        if ("$INITIATOR".equals(userTask.getAssignee())) {
          if (startEventMap.get(process.getId()) != null) {
            userTask.setAssignee("${" + startEventMap.get(process.getId()).getInitiator() + "}");
          }
        }

      } else if (flowElement instanceof SubProcess) {
        processUserTasks(((SubProcess) flowElement).getFlowElements(), process, startEventMap);
      }
    }
  }
  
  protected AppDefinition resolveAppDefinition(Model appDefinitionModel) {
    try {
      AppDefinition appDefinition = objectMapper.readValue(appDefinitionModel.getModelEditorJson(), AppDefinition.class);
      return appDefinition;
      
    } catch (Exception e) {
      logger.error("Error deserializing app " + appDefinitionModel.getId(), e);
      throw new InternalServerErrorException("Could not deserialize app definition");
    }
  }

  protected String getAppDefinitionJson(AppDefinition appDefinition) {
    ObjectNode appDefinitionNode = objectMapper.createObjectNode();
    appDefinitionNode.put("theme", appDefinition.getTheme());
    appDefinitionNode.put("icon", appDefinition.getIcon());
    return appDefinitionNode.toString();
  }

  protected byte[] createDeployZipArtifact(Map<String, byte[]> deployableAssets) {
    ByteArrayOutputStream baos = null;
    ZipOutputStream zos = null;
    byte[] deployZipArtifact = null;
    try {
      baos = new ByteArrayOutputStream();
      zos = new ZipOutputStream(baos);

      for (Map.Entry<String, byte[]> entry : deployableAssets.entrySet()) {
        ZipEntry zipEntry = new ZipEntry(entry.getKey());
        zipEntry.setSize(entry.getValue().length);
        zos.putNextEntry(zipEntry);
        zos.write(entry.getValue());
        zos.closeEntry();
      }

      // this is the zip file as byte[]
      deployZipArtifact = baos.toByteArray();
    } catch (IOException ioe) {
      logger.error("Error adding deploy zip entry", ioe);
      throw new InternalServerErrorException("Could not create deploy zip artifact");
    }

    return deployZipArtifact;
  }

  protected void deployZipArtifact(String artifactName, byte[] zipArtifact) {
    String deployApiUrl = environment.getRequiredProperty("deployment.api.url");
    String basicAuthUser = environment.getRequiredProperty("idm.admin.user");
    String basicAuthPassword = environment.getRequiredProperty("idm.admin.password");

    if (deployApiUrl.endsWith("/") == false) {
      deployApiUrl = deployApiUrl.concat("/");
    }
    deployApiUrl = deployApiUrl.concat("repository/deployments");

    HttpPost httpPost = new HttpPost(deployApiUrl);
    httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(
        Base64.encodeBase64((basicAuthUser + ":" + basicAuthPassword).getBytes(Charset.forName("UTF-8")))));

    MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
    entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
    entityBuilder.addBinaryBody("artifact", zipArtifact, ContentType.DEFAULT_BINARY, artifactName);

    HttpEntity entity = entityBuilder.build();
    httpPost.setEntity(entity);

    HttpClientBuilder clientBuilder = HttpClientBuilder.create();
    SSLConnectionSocketFactory sslsf = null;
    try {
      SSLContextBuilder builder = new SSLContextBuilder();
      builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
      sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      clientBuilder.setSSLSocketFactory(sslsf);
    } catch (Exception e) {
      logger.error("Could not configure SSL for http client", e);
      throw new InternalServerErrorException("Could not configure SSL for http client", e);
    }

    CloseableHttpClient client = clientBuilder.build();

    try {
      HttpResponse response = client.execute(httpPost);
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
        return;
      } else {
        logger.error("Invalid deploy result code: {}", response.getStatusLine());
        throw new InternalServerErrorException("Invalid deploy result code: " + response.getStatusLine());
      }
    } catch (IOException ioe) {
      logger.error("Error calling deploy endpoint", ioe);
      throw new InternalServerErrorException("Error calling deploy endpoint: " + ioe.getMessage());
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (IOException e) {
          logger.warn("Exception while closing http client", e);
        }
      }
    }
  }
}
