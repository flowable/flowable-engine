/**
 *
 */
package org.flowable.form.model;

import java.io.Serializable;

/**
 *
 * @author Simon Yang
 * @version $Id$
 * @since
 */
public class RdsFormModel implements Serializable
{
  private static final long serialVersionUID = 1L;

  protected String key;

  protected String definition = "{\"sfFormProperties\": {\"items\": [],\"type\": \"root\" },\"sfSchemaProperties\": {}}";

  public String getKey()
  {
    return key;
  }

  public void setKey(String key)
  {
    this.key = key;
  }

  public String getDefinition()
  {
    return definition;
  }

  public void setDefinition(String definition)
  {
    this.definition = definition;
  }

}
