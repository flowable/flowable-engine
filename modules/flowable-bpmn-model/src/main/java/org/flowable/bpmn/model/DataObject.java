package org.flowable.bpmn.model;

/**
 * @author Lori Small
 */
public class DataObject extends FlowElement {

  protected ItemDefinition itemSubjectRef;

  public ItemDefinition getItemSubjectRef() {
    return itemSubjectRef;
  }

  public void setItemSubjectRef(ItemDefinition itemSubjectRef) {
    this.itemSubjectRef = itemSubjectRef;
  }

  @Override
  public DataObject clone() {
    DataObject clone = new DataObject();
    clone.setValues(this);
    return clone;
  }

  public void setValues(DataObject otherElement) {
    super.setValues(otherElement);

    setId(otherElement.getId());
    setName(otherElement.getName());
    setItemSubjectRef(otherElement.getItemSubjectRef());
  }
}
