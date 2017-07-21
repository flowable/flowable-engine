package org.flowable.app.rest.editor;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.model.editor.form.FormRepresentation;
import org.flowable.app.repository.editor.ModelRepository;
import org.flowable.app.service.editor.FlowableFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import au.com.rds.schemaformbuilder.form.FormDesign;
import au.com.rds.schemaformbuilder.formdesignjson.FormDesignJsonService;
import au.com.rds.schemaformbuilder.util.JsonUtils;

@RestController
@RequestMapping("/sf/forms")
public class RdsFormResource
{

  @Autowired
  protected FlowableFormService formService;
  
  @Autowired
  FormDesignJsonService formDesignJsonService;
  
  @Autowired
  ModelRepository modelRepo;

  ObjectMapper objectMapper = new ObjectMapper();
  
  @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ArrayNode> listForms()
  {
    ArrayNode responseData = JsonUtils.arrayNode();
    
    List<Model> models =  modelRepo.findByModelType(AbstractModel.MODEL_TYPE_FORM_RDS, null);    

    for (Model form : models)
    {
      String name = form.getName();
      responseData.addObject()
              .put("key", form.getKey())
              .put("name", !StringUtils.isBlank(name) ? name : form.getKey());
    }

    return new ResponseEntity<ArrayNode>(responseData, HttpStatus.OK);

  }

  @RequestMapping(value = "/{formKey}", method = RequestMethod.GET, produces = "application/json")
  public @ResponseBody ResponseEntity<JsonNode> getForm(@PathVariable String formKey)
  {
    ObjectNode responseData = formDesignJsonService.findByKeyWithReferencedBy(formKey);
    HttpStatus responseStatus;
    if (responseData == null)
    {
      responseData = JsonUtils.objectNode();
      ObjectNode error = JsonUtils.objectNode();
      responseData.set("error", error);
      error.set("message", JsonUtils.textNode("Form with key " + formKey + " was not found."));
      error.set("type", JsonUtils.textNode("form_not_found"));
      responseStatus = HttpStatus.NOT_FOUND;
    } else {
      responseStatus = HttpStatus.OK;
    }
    return new ResponseEntity<JsonNode>(responseData, responseStatus);
  }

  @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
  public @ResponseBody ResponseEntity<JsonNode> saveForm(@RequestBody String json)
  {
    ObjectNode responseData = JsonUtils.objectNode();
    FormRepresentation formRep = formService.saveForm(json);
    long lastUpdated = formRep.getLastUpdated().getTime();
    responseData.set("lastUpdated", JsonUtils.numberNode(lastUpdated));
    return new ResponseEntity<JsonNode>(responseData, HttpStatus.OK);
  }
}
