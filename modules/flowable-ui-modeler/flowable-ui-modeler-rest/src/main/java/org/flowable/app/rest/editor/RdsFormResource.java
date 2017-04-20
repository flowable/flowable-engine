package org.flowable.app.rest.editor;

import org.flowable.app.model.editor.form.FormRepresentation;
import org.flowable.app.service.editor.FlowableFormService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/sf/forms")
public class RdsFormResource
{

  @Autowired
  protected FlowableFormService formService;

  ObjectMapper objectMapper = new ObjectMapper();

  @RequestMapping(value = "/{formKey}", method = RequestMethod.GET, produces = "application/json")
  public String getForm(@PathVariable String formKey)
  {
    return formService.getRdsForm(formKey);
  }

  @RequestMapping(value = "", method = RequestMethod.POST, produces = "application/json")
  public FormRepresentation saveForm(@RequestBody String json)
  {
    return formService.saveForm(json);
  }
}
