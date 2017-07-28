/**
 * 
 */
package org.flowable.app.rest.editor;

import java.util.List;

import org.flowable.app.domain.editor.ModelTag;
import org.flowable.app.service.api.ModelTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import au.com.rds.schemaformbuilder.util.JsonUtils;

/**
 *
 * @author Simon Yang
 * @version $Id$
 * @since
 */

@RestController
public class ModelTagResource
{
  @Autowired
  ModelTagService modelTagService;
  
  @RequestMapping(value = "/rest/modeltags", method = RequestMethod.GET, produces = "application/json")
  public List<ModelTag> getModelTagByModelType() {
    return modelTagService.findAll();
  }
  
  @RequestMapping(value = "/rest/modeltags", method = RequestMethod.POST, produces = "application/json")
  public void createModelTag(@RequestBody String body) {
    JsonNode jsonNode = JsonUtils.fromJson(body);
    Assert.notNull(jsonNode.get("name"), "Name cannot be empty!");     
    
    String name = jsonNode.get("name").asText();
    Assert.hasText(name, "Name cannot be empty!");
    modelTagService.createModelTag(name);
  }
  
  @RequestMapping(value = "/rest/modeltags/{tagId}", method = RequestMethod.DELETE, produces = "application/json")
  public boolean deleteModelTag(@PathVariable String tagId) {
    return modelTagService.deleteModelTag(tagId);
    
  }
  
  @RequestMapping(value = "/rest/modeltags/{tagId}", method = RequestMethod.PUT, produces = "application/json")
  public void updateModelTag(@PathVariable String tagId, @RequestBody String body) {
    JsonNode jsonNode = JsonUtils.fromJson(body);
    Assert.notNull(jsonNode.get("name"), "Name cannot be empty!");     
    
    String name = jsonNode.get("name").asText();
    Assert.hasText(name, "Name cannot be empty!");
    modelTagService.updateModelTag(tagId, name);
  }
}
