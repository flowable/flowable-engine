package org.activiti.engine.impl.app;

import org.activiti.engine.app.AppModel;
import org.activiti.engine.app.AppResourceConverter;
import org.activiti.engine.common.api.ActivitiException;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AppResourceConverterImpl implements AppResourceConverter {
  
  protected ObjectMapper objectMapper;

  public AppResourceConverterImpl(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }
  
  public Object convertAppResourceToModel(byte[] appResourceBytes) {
    AppModel appModel;
    try {
      appModel = objectMapper.readValue(appResourceBytes, AppModel.class);
    } catch (Exception e) {
      throw new ActivitiException("Error reading app resource", e);
    }
    
    return appModel;
  }
  
}
