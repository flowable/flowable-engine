ALTER TABLE FLW_CHANNEL_DEFINITION ADD TYPE_ VARCHAR(255) NULL;

ALTER TABLE FLW_CHANNEL_DEFINITION ADD IMPLEMENTATION_ VARCHAR(255) NULL;

execute java org.flowable.eventregistry.impl.cmd.UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmd
