/**
 * Copyright 2005-2015 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.activiti.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public enum EndpointType {

  PROCESS(1),
  DMN(2),
  FORM(3);

  private static Map<Integer, EndpointType> map = new HashMap<>();

  static {
    for (EndpointType endpointType : EndpointType.values()) {
      map.put(endpointType.endpointCode, endpointType);
    }
  }

  private final int endpointCode;

  EndpointType(int endpointCode) {
    this.endpointCode = endpointCode;
  }

  public int getEndpointCode() {
    return this.endpointCode;
  }

  public static EndpointType valueOf(int endpointCode) {
    return map.get(endpointCode);
  }

}
