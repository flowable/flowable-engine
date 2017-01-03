package org.flowable.stencil;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class StencilReader {
  
  private static final Logger logger = LoggerFactory.getLogger(StencilReader.class);

  protected ObjectMapper objectMapper = new ObjectMapper();
  
  public void readStencil() {
    ObjectNode stencilsetNode = null;
    try {
      stencilsetNode = (ObjectNode) objectMapper.readTree(this.getClass().getClassLoader().getResourceAsStream("stencilset_bpmn.json"));
    } catch (Exception e) {
      logger.error("Error reading stencil", e);
    }
    
    Map<String, JsonNode> propertyMap = new HashMap<>();
    ArrayNode propertyPackageArrayNode = (ArrayNode) stencilsetNode.get("propertyPackages");
    for (JsonNode propertyPackageNode : propertyPackageArrayNode) {
      System.out.println("propertyPackageNode " + propertyPackageNode.get("name").asText());
      propertyMap.put(propertyPackageNode.get("name").asText(), propertyPackageNode);
    }
    
    ObjectNode multiInstancePropertyNode = (ObjectNode) propertyMap.get("multiinstance_typepackage").get("properties").get(0);
    multiInstancePropertyNode.put("title", "flowable-multiinstance");
    
    ObjectNode completionConditionNode = createPropertiesNode("completioncondition", "String", "Completion condition", "true", 
        "The completion condition for the adhoc sub process", null);
    propertyPackageArrayNode.add(completionConditionNode);
    
    ObjectNode orderingNode = createPropertiesNode("ordering", "flowable-ordering", "Ordering", "Parallel", 
        "The ordering for the adhoc sub process", null);
    propertyPackageArrayNode.add(orderingNode);
    
    ObjectNode cancelRemainingInstancesNode = createPropertiesNode("cancelremaininginstances", "Boolean", "Cancel remaining instances", "true", 
        "Cancel the remaining instances for the adhoc sub process?", null);
    propertyPackageArrayNode.add(cancelRemainingInstancesNode);
    
    Map<String, JsonNode> stencilMap = new HashMap<>();
    ArrayNode stencilArrayNode = (ArrayNode) stencilsetNode.get("stencils");
    for (JsonNode stencilNode : stencilArrayNode) {
      System.out.println("stencilNode " + stencilNode.get("id").asText());
      stencilMap.put(stencilNode.get("id").asText(), stencilNode);
    }
    
    try {
      ObjectNode adhocItemNode = createStencilItemNode("AdhocSubProcess", "Adhoc sub process", "An adhoc sub process", "adhoc_subprocess.svg");
      stencilArrayNode.add(adhocItemNode);
      
      objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
      objectMapper.writeValue(new File("stencilset_bpmn_modified.json"), stencilsetNode);
    } catch (Exception e) {
      logger.error("Error writing stencil", e);
    }
  }
  
  protected ObjectNode createStencilItemNode(String id, String title, String description, String svgFileName) throws Exception {
    ObjectNode stencilItemNode = objectMapper.createObjectNode();
    stencilItemNode.put("type", "node");
    stencilItemNode.put("id", id);
    stencilItemNode.put("title", title);
    stencilItemNode.put("description", description);
    stencilItemNode.put("view", IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(svgFileName)));
    stencilItemNode.put("icon", "activity/adhoc.subprocess.png");
    ArrayNode groupsArray = stencilItemNode.putArray("groups");
    groupsArray.add("Structural");
    
    ArrayNode propertyArray = stencilItemNode.putArray("propertyPackages");
    propertyArray.add("overrideidpackage");
    propertyArray.add("namepackage");
    propertyArray.add("documentationpackage");
    propertyArray.add("completionconditionpackage");
    propertyArray.add("orderingpackage");
    propertyArray.add("cancelremaininginstancespackage");
    
    stencilItemNode.putArray("hiddenPropertyPackages");
    
    ArrayNode rolesArray = stencilItemNode.putArray("roles");
    rolesArray.add("Activity");
    rolesArray.add("sequence_start");
    rolesArray.add("sequence_end");
    rolesArray.add("all");
    
    return stencilItemNode;
  }
  
  protected ObjectNode createPropertiesNode(String id, String type, String title, String value, String description, List<String> refToViews) {
    ObjectNode propertyRootNode = objectMapper.createObjectNode();
    propertyRootNode.put("name", id + "package");
    ArrayNode propertiesNode = propertyRootNode.putArray("properties");
    ObjectNode propertyNode = propertiesNode.objectNode();
    propertyNode.put("id", id);
    propertyNode.put("type", type);
    propertyNode.put("title", title);
    propertyNode.put("value", value);
    propertyNode.put("description", description);
    propertyNode.put("popular", true);
    propertiesNode.add(propertyNode);
    
    if (refToViews != null && refToViews.size() > 0) {
      ArrayNode refArrayNode = propertyNode.putArray("refToView");
      for (String refToView : refToViews) {
        refArrayNode.add(refToView);
      }
    }
    
    return propertyRootNode;
  }
  
  protected void addPropertyRef(ObjectNode elementNode, String propertyRef) {
    ArrayNode propertyRefArray = (ArrayNode) elementNode.get("propertyPackages");
    propertyRefArray.add(propertyRef);
  }
}
