---
id: ch06-EventRegistry-Introduction
title: Event Registry Introduction
---

## What is the Event Registry?

The Event Registry engine allows for sending and receiving events from any source (with out-of-the-box support for JMS, Kafka, RabbitMQ and receiving events over HTTP) and using these
events in CMMN and BPMN models. For example, it's possible to receive an event from a JMS queue and start a new process instance of a process definition that has a start event defined
that listens to this event type. Or receive an event from a Kafka topic and trigger a boundary event in a specific process instance that matches the correlation definition of the incoming event.
The same possibilities are there for CMMN, with starting a new case instance when a specific event type is received or triggering an event registry listener of a specific case instance.
There are two types of definitions that need to be configured, e.g. Event and Channel definitions.

## What is an Event definition

An Event definition defines the event payload of an incoming or outgoing event in addition to the default key and name properties that identify the event type.
The event payload is defined with an array of name and type objects, with string, integer, double and boolean types supported.
Another part of the event definition is the correlation parameters property, for which an array of parameter objects with a name and type can be defined.
These correlation parameter values are used to match an incoming event to an event subscription of a case or process instance.
An example of an Event definition is the following JSON definition:

    {
      "key": "myEvent",
      "name": "My event",
      "correlationParameters": [
          {
            "name": "customerId",
            "type": "string"
          }
      ],
      "payload": [
          {
            "name": "customerName",
            "type": "string"
          },
          {
            "name": "amount",
            "type": "integer"
          }
      ]
    }

## What is a Channel definition

A Channel definition defines the destination of an incoming or outgoing event in addition to the default key and name properties that identify the channel type.
The destination name is defined together with a type value that specifies the adapter type to be used. Out-of-the-box the JMS, Kafka and RabbitMQ adapter types are supported, but
other adapters can be added as well. For a Channel definition the channel type can be either inbound (incoming events) or outbound (outgoing events).

For an inbound channel definition the event key detection needs to be defined, which configures how the event key can be determined for the incoming event.
This can be a fixed event type value, or the event key can be determined based on a JSON field value or JSON pointer expression.
In a similar way the tenant id detection can be defined for multi tenant Flowable environments. Here also a static tenant id value can be defined, or a JSON field value or JSON pointer expression.
Another part that can be defined for an inbound channel definition is the way the incoming event can be deserialized.
By default JSON and XML are the out-of-the-box supported deserialization options.

For an outbound channel definition the serializer type can be defined, with the supported JSON and XML values.
An example inbound channel JSON definition is:

    {
      "key": "testChannel",
      "category": "channel",
      "name": "Test channel",
      "channelType": "inbound",
      "type": "jms",
      "destination": "test-customer",
      "deserializerType": "json",
      "channelEventKeyDetection": {
          "jsonField": "eventKeyValue"
      }
    }

This example inbound channel definition connects to a JMS queue named **test-customer** and incoming events will be deserialized as JSON for the event payload and the
event key will be determined based on the **eventKeyValue** JSON property in the incoming event payload.

An example outbound channel JSON definition is:

    {
      "key": "outboundCustomer",
      "category": "channel",
      "name": "Test channel",
      "channelType": "outbound",
      "type": "jms",
      "destination": "outbound-customer",
      "serializerType": "json"
    }
    
This example outbound channel definition connects to a JMS queue named **outbound-customer** and outgoing events will be serialized as a JSON event payload.
