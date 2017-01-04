package org.flowable.engine.impl.persistence;

public interface CountingTaskEntity {

  boolean isCountEnabled();

  void setCountEnabled(boolean isCountEnabled);

  void setVariableCount(int variableCount);

  int getVariableCount();

  void setIdentityLinkCount(int identityLinkCount);

  int getIdentityLinkCount();
}
