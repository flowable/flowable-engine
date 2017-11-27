package org.flowable.app.service.editor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.service.api.ModelService;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.persistence.entity.UserEntityImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import au.com.rds.test.TestContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestContext.class})
public class ModelServiceImplTest
{
  @Autowired
  ModelService modelService;

  @Value("classpath:testBpmn.json")
  private Resource testBpmnJsonResource;

  @Value("classpath:testSfFormDesign.json")
  private Resource testSfFormDesign;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  public void bpmnProvenanceGenerationTest() throws Exception
  {
    User updatedBy = new UserEntityImpl();
    updatedBy.setId("testUserId");

    String editorJson = this.getStringFromResource(testBpmnJsonResource);

    Model model = new Model();
    model.setId("id");
    model.setName("name");
    model.setModelEditorJson(editorJson);
    model.setModelType(AbstractModel.MODEL_TYPE_BPMN);
    this.modelService.saveModel(model, model.getModelEditorJson(), null, false, null, updatedBy);

    this.modelService.saveModel(model, model.getModelEditorJson(), null, false, null, updatedBy);

    JsonNode jsonNode = objectMapper.readTree(model.getModelEditorJson());
    ArrayNode provenancesNode = (ArrayNode) jsonNode.get("properties").get("rds_provenances");
    Assert.assertNotNull(provenancesNode);
    Assert.assertEquals(provenancesNode.size(), 2);

    ObjectNode provenanceNode = (ObjectNode) provenancesNode.get(1);

    Assert.assertTrue(provenanceNode.get("user").textValue().equalsIgnoreCase("testUserId"));

    Assert.assertNotNull(provenanceNode.get("time").asText());
    Assert.assertNotNull(provenanceNode.get("designerVersion").textValue());

  }

  @Test
  public void sfDesignProvenanceGenerationTest() throws Exception
  {
    User updatedBy = new UserEntityImpl();
    updatedBy.setId("testUserId");

    String editorJson = this.getStringFromResource(testSfFormDesign);

    Model model = new Model();
    model.setId("id");
    model.setName("name");
    model.setModelEditorJson(editorJson);
    model.setModelType(AbstractModel.MODEL_TYPE_FORM_RDS);

    this.modelService.saveModel(model, model.getModelEditorJson(), null, false, null, updatedBy);

    this.modelService.saveModel(model, model.getModelEditorJson(), null, false, null, updatedBy);

    JsonNode jsonNode = objectMapper.readTree(model.getModelEditorJson());
    ArrayNode provenancesNode = (ArrayNode) jsonNode.get("metadata").get("provenance");
    Assert.assertNotNull(provenancesNode);
    Assert.assertEquals(provenancesNode.size(), 2);

    ObjectNode provenanceNode = (ObjectNode) provenancesNode.get(1);

    Assert.assertTrue(provenanceNode.get("user").textValue().equalsIgnoreCase("testUserId"));

    Assert.assertNotNull(provenanceNode.get("time").asText());
    Assert.assertNotNull(provenanceNode.get("designerVersion").textValue());

  }


  private String getStringFromResource(Resource rs) throws Exception
  {
    try (InputStream is = rs.getInputStream())
    {
      return IOUtils.toString(is);
    }
  }

}