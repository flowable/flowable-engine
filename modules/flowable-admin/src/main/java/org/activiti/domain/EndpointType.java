/**
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.activiti.domain;

/**
 * @author Yvo Swillens
 */
public enum EndpointType {

  PROCESS(1),
  DMN(2),
  FORM(3);

  private final int endpointCode;

  EndpointType(int endpointCode) {
    this.endpointCode = endpointCode;
  }

  public int getEndpointCode() {
    return this.endpointCode;
  }

}
