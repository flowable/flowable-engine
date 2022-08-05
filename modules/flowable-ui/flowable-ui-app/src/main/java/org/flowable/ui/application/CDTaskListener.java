package org.flowable.ui.application;

import org.apache.kafka.common.serialization.StringSerializer;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class CDTaskListener
  implements TaskListener
{
  public ProducerFactory<String, String> producerFactory()
  {
    Map<String, Object> configProps = new HashMap();
    configProps.put("bootstrap.servers", "143.110.178.147:9092");
    
    configProps.put("key.serializer", StringSerializer.class);
    
    configProps.put("value.serializer", StringSerializer.class);
    
    return new DefaultKafkaProducerFactory(configProps);
  }
  
  public KafkaTemplate<String, String> kafkaTemplate()
  {
    return new KafkaTemplate(producerFactory());
  }
  
  private void sendEmail(DelegateTask delegateTask)
  {
    RestTemplate restTemplate = new RestTemplate();
    Map<String, String> body = new HashMap();
    body.put("email_address", delegateTask.getAssignee());
    body.put("subject", "New Task Assigned");
    body.put("body", String.format("New Task %s with ID %s assigned.",
          delegateTask.getTaskDefinitionKey(), delegateTask.getId()));
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, String>> entity = new HttpEntity(body, headers);
    String response = (String)restTemplate.exchange("https://dev.amoga.io/api/v1/core/sendmail", HttpMethod.POST, entity, String.class, new Object[0]).getBody();
    System.out.println(response);
  }
  
  public void notify(DelegateTask delegateTask)
  {
    try
    {
      String category;
      if (delegateTask.getCategory() == null) {
        category = delegateTask.getTaskDefinitionKey();
      } else {
        category = delegateTask.getCategory();
      }
      if ("complete".equalsIgnoreCase(delegateTask.getEventName()))
      {
        for (VariableInstance variable : delegateTask.getVariableInstances().values()) {
          if (variable.getName().equals("_status")) {
            delegateTask.setVariableLocal("amoTaskLocal_" + category + "__status", variable
              .getValue());
          } else {
            delegateTask.setVariableLocal("amoTaskLocal_" + variable
              .getName(), variable.getValue());
          }
        }
      }
      if ("create".equalsIgnoreCase(delegateTask.getEventName())) {
        delegateTask.setVariable("_status", "toDo");
      }
      if ("assignment".equalsIgnoreCase(delegateTask.getEventName())) {
        sendEmail(delegateTask);
      }
      if ("delete".equalsIgnoreCase(delegateTask.getEventName())) {
        delegateTask.setVariable(category + "__status", "task_completed");
      }
      String event = "{\n\"task\":\"" + delegateTask.getTaskDefinitionKey() + "\",\n    \"task_id\":\"" + delegateTask.getId() + "\",\n    \"process\":\"" + delegateTask.getProcessDefinitionId() + "\",\n    \"processId\":\"" + delegateTask.getProcessInstanceId() + "\",\n    \"assignee\":\"" + delegateTask.getAssignee() + "\",\n    \"event\":\"" + delegateTask.getEventName() + "\"\n}";
      
      kafkaTemplate().send("amoga-task-assignment-topic", event);
    }
    catch (Exception e)
    {
      System.out.println(e.getMessage());
    }
  }
}