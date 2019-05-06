package org.flowable.engine.impl.util;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.impl.event.EventHandler;
import org.flowable.engine.impl.jobexecutor.ProcessEventJobHandler;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

public class EventSubscriptionUtil {

    public static void eventReceived(EventSubscriptionEntity eventSubscriptionEntity, Object payload, boolean processASync) {
        if (processASync) {
            scheduleEventAsync(eventSubscriptionEntity, payload);
        } else {
            processEventSync(eventSubscriptionEntity, payload);
        }
    }

    protected static void processEventSync(EventSubscriptionEntity eventSubscriptionEntity, Object payload) {

        // A compensate event needs to be deleted before the handlers are called
        if (eventSubscriptionEntity instanceof CompensateEventSubscriptionEntity) {
            CommandContextUtil.getEventSubscriptionService().deleteEventSubscription(eventSubscriptionEntity);
            CountingEntityUtil.handleDeleteEventSubscriptionEntityCount(eventSubscriptionEntity);
        }

        EventHandler eventHandler = CommandContextUtil.getProcessEngineConfiguration().getEventHandler(eventSubscriptionEntity.getEventType());
        if (eventHandler == null) {
            throw new FlowableException("Could not find eventhandler for event of type '" + eventSubscriptionEntity.getEventType() + "'.");
        }
        eventHandler.handleEvent(eventSubscriptionEntity, payload, CommandContextUtil.getCommandContext());
    }

    protected static void scheduleEventAsync(EventSubscriptionEntity eventSubscriptionEntity, Object payload) {
        JobService jobService = CommandContextUtil.getJobService();
        JobEntity message = jobService.createJob();
        message.setJobType(JobEntity.JOB_TYPE_MESSAGE);
        message.setJobHandlerType(ProcessEventJobHandler.TYPE);
        message.setElementId(eventSubscriptionEntity.getActivityId());
        message.setJobHandlerConfiguration(eventSubscriptionEntity.getId());
        message.setTenantId(eventSubscriptionEntity.getTenantId());

        // TODO: support payload
        // if(payload != null) {
        // message.setEventPayload(payload);
        // }

        jobService.scheduleAsyncJob(message);
    }
}
