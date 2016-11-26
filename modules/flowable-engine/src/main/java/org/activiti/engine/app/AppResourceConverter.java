package org.activiti.engine.app;

public interface AppResourceConverter {

  Object convertAppResourceToModel(byte[] appResourceBytes);
}
