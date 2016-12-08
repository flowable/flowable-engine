package org.flowable.engine.impl.app;

import org.flowable.engine.app.AppModel;
import org.flowable.engine.app.AppResourceConverter;
import org.flowable.engine.common.api.FlowableException;

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
      throw new FlowableException("Error reading app resource", e);
    }
    
    return appModel;
  }
  
}
