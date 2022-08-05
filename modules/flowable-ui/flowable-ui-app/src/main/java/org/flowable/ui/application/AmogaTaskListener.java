package org.flowable.ui.application;

import org.apache.kafka.clients.producer.ProducerConfig;
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


public class AmogaTaskListener implements TaskListener {

    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                "143.110.178.147:9092");
        configProps.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        configProps.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    private void sendEmail(DelegateTask delegateTask){
        RestTemplate restTemplate = new RestTemplate();
        Map<String, String> body = new HashMap<String, String>();
        body.put("email_address", delegateTask.getAssignee());
        body.put("subject", "New Task Assigned");
        body.put("body", String.format("New Task %s with ID %s assigned.",
                delegateTask.getTaskDefinitionKey(), delegateTask.getId()));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> entity = new HttpEntity<Map<String, String>>(body,headers);
        restTemplate.exchange("https://dev.amoga.io/api/v1/core/sendmail", HttpMethod.POST, entity, String.class).getBody();
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            if ("complete".equalsIgnoreCase(delegateTask.getEventName())) {
                for (VariableInstance variable : delegateTask.getVariableInstances().values()) {
                    delegateTask.setVariableLocal("amoTaskLocal_" + variable
                            .getName(), variable.getValue());
                }
            }
            if ("create".equalsIgnoreCase(delegateTask.getEventName())) {
                delegateTask.setVariable(delegateTask.getCategory() + "__status", "toDo");
            }
            if ("assignment".equalsIgnoreCase(delegateTask.getEventName())) {
                sendEmail(delegateTask);
            }
            if ("delete".equalsIgnoreCase(delegateTask.getEventName())) {
                delegateTask.setVariable(delegateTask.getCategory() + "__status", "task_completed");
            }
            String event = "{\n" +
                    "    \"task\":\"" + delegateTask.getTaskDefinitionKey() + "\",\n" +
                    "    \"task_id\":\"" + delegateTask.getId() + "\",\n" +
                    "    \"process\":\"" + delegateTask.getProcessDefinitionId() + "\",\n" +
                    "    \"processId\":\"" + delegateTask.getProcessInstanceId() + "\",\n" +
                    "    \"assignee\":\"" + delegateTask.getAssignee() + "\",\n" +
                    "    \"event\":\"" + delegateTask.getEventName() + "\"\n" +
                    "}";

            kafkaTemplate().send("amoga-task-assignment-topic", event);
        } catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
