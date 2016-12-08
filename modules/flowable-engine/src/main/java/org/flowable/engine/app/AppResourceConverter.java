package org.flowable.engine.app;

public interface AppResourceConverter {

  Object convertAppResourceToModel(byte[] appResourceBytes);
}
