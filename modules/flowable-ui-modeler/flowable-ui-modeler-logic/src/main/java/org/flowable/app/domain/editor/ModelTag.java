/**
 * 
 */
package org.flowable.app.domain.editor;

import java.util.Date;

/**
 *
 * @author Simon Yang
 * @version $Id$
 * @since
 */
public class ModelTag
{
  private String id;
  private String name;  
  private Date created;
  private String createdBy;
  private Date updated;
  private String updatedBy;
  public String getId()
  {
    return id;
  }
  public void setId(String id)
  {
    this.id = id;
  }
  public String getName()
  {
    return name;
  }
  public void setName(String name)
  {
    this.name = name;
  }
  public Date getCreated()
  {
    return created;
  }
  public void setCreated(Date created)
  {
    this.created = created;
  }
  public String getCreatedBy()
  {
    return createdBy;
  }
  public void setCreatedBy(String createdBy)
  {
    this.createdBy = createdBy;
  }
  public Date getUpdated()
  {
    return updated;
  }
  public void setUpdated(Date updated)
  {
    this.updated = updated;
  }
  public String getUpdatedBy()
  {
    return updatedBy;
  }
  public void setUpdatedBy(String updatedBy)
  {
    this.updatedBy = updatedBy;
  } 
  
  
}
