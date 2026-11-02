ALTER TABLE FLW_CHANNEL_DEFINITION ADD TYPE_ varchar(255);

ALTER TABLE FLW_CHANNEL_DEFINITION ADD IMPLEMENTATION_ varchar(255);

execute java org.flowable.eventregistry.impl.cmd.UpdateChannelDefinitionTypeAndImplementationForAllChannelDefinitionsCmd
