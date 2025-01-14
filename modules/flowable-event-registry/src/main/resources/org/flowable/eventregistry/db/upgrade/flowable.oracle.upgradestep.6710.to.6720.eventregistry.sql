ALTER TABLE FLW_CHANNEL_DEFINITION ADD TYPE_ VARCHAR2(255);

ALTER TABLE FLW_CHANNEL_DEFINITION ADD IMPLEMENTATION_ VARCHAR2(255);

execute java org.flowable.eventregistry.impl.cmd.UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmd
