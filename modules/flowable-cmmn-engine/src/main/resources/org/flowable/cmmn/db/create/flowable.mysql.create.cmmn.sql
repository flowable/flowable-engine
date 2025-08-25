CREATE TABLE ACT_CMMN_DEPLOYMENT
(
    ID_                   VARCHAR(255)            NOT NULL,
    NAME_                 VARCHAR(255)            NULL,
    CATEGORY_             VARCHAR(255)            NULL,
    KEY_                  VARCHAR(255)            NULL,
    DEPLOY_TIME_          datetime(3)             NULL,
    PARENT_DEPLOYMENT_ID_ VARCHAR(255)            NULL,
    TENANT_ID_            VARCHAR(255) DEFAULT '' NULL,
    CONSTRAINT PK_ACT_CMMN_DEPLOYMENT PRIMARY KEY (ID_)
);

CREATE TABLE ACT_CMMN_DEPLOYMENT_RESOURCE
(
    ID_             VARCHAR(255) NOT NULL,
    NAME_           VARCHAR(255) NULL,
    DEPLOYMENT_ID_  VARCHAR(255) NULL,
    RESOURCE_BYTES_ LONGBLOB     NULL,
    GENERATED_ TINYINT NULL,
    CONSTRAINT PK_CMMN_DEPLOYMENT_RESOURCE PRIMARY KEY (ID_)
);

ALTER TABLE ACT_CMMN_DEPLOYMENT_RESOURCE
    ADD CONSTRAINT ACT_FK_CMMN_RSRC_DPL FOREIGN KEY (DEPLOYMENT_ID_) REFERENCES ACT_CMMN_DEPLOYMENT (ID_);

CREATE INDEX ACT_IDX_CMMN_RSRC_DPL ON ACT_CMMN_DEPLOYMENT_RESOURCE (DEPLOYMENT_ID_);

CREATE TABLE ACT_CMMN_CASEDEF
(
    ID_                     VARCHAR(255)            NOT NULL,
    REV_                    INT                     NOT NULL,
    NAME_                   VARCHAR(255)            NULL,
    KEY_                    VARCHAR(255)            NOT NULL,
    VERSION_                INT                     NOT NULL,
    CATEGORY_               VARCHAR(255)            NULL,
    DEPLOYMENT_ID_          VARCHAR(255)            NULL,
    RESOURCE_NAME_          VARCHAR(4000)           NULL,
    DESCRIPTION_            VARCHAR(4000)           NULL,
    HAS_GRAPHICAL_NOTATION_ TINYINT                 NULL,
    DGRM_RESOURCE_NAME_     VARCHAR(4000)           NULL,
    HAS_START_FORM_KEY_     TINYINT                 NULL,
    TENANT_ID_              VARCHAR(255) DEFAULT '' NULL,
    CONSTRAINT PK_ACT_CMMN_CASEDEF PRIMARY KEY (ID_)
);

ALTER TABLE ACT_CMMN_CASEDEF
    ADD CONSTRAINT ACT_FK_CASE_DEF_DPLY FOREIGN KEY (DEPLOYMENT_ID_) REFERENCES ACT_CMMN_DEPLOYMENT (ID_);
CREATE INDEX ACT_IDX_CASE_DEF_DPLY ON ACT_CMMN_CASEDEF (DEPLOYMENT_ID_);

CREATE UNIQUE INDEX ACT_IDX_CASE_DEF_UNIQ ON ACT_CMMN_CASEDEF (KEY_, VERSION_, TENANT_ID_);

CREATE TABLE ACT_CMMN_RU_CASE_INST
(
    ID_                        VARCHAR(255)            NOT NULL,
    REV_                       INT                     NOT NULL,
    BUSINESS_KEY_              VARCHAR(255)            NULL,
    NAME_                      VARCHAR(255)            NULL,
    PARENT_ID_                 VARCHAR(255)            NULL,
    CASE_DEF_ID_               VARCHAR(255)            NULL,
    STATE_                     VARCHAR(255)            NULL,
    START_TIME_                datetime(3)             NULL,
    START_USER_ID_             VARCHAR(255)            NULL,
    CALLBACK_ID_               VARCHAR(255)            NULL,
    CALLBACK_TYPE_             VARCHAR(255)            NULL,
    LOCK_TIME_                 datetime(3)             NULL,
    LOCK_OWNER_                VARCHAR(255)            NULL,
    IS_COMPLETEABLE_           TINYINT                 NULL,
    REFERENCE_ID_              VARCHAR(255)            NULL,
    REFERENCE_TYPE_            VARCHAR(255)            NULL,
    LAST_REACTIVATION_TIME_    datetime(3)             NULL,
    LAST_REACTIVATION_USER_ID_ VARCHAR(255)            NULL,
    BUSINESS_STATUS_           VARCHAR(255)            NULL,
    TENANT_ID_                 VARCHAR(255) DEFAULT '' NULL,
    CONSTRAINT PK_ACT_CMMN_RU_CASE_INST PRIMARY KEY (ID_)
);

ALTER TABLE ACT_CMMN_RU_CASE_INST
    ADD CONSTRAINT ACT_FK_CASE_INST_CASE_DEF FOREIGN KEY (CASE_DEF_ID_) REFERENCES ACT_CMMN_CASEDEF (ID_);

CREATE INDEX ACT_IDX_CASE_INST_CASE_DEF ON ACT_CMMN_RU_CASE_INST (CASE_DEF_ID_);
CREATE INDEX ACT_IDX_CASE_INST_PARENT ON ACT_CMMN_RU_CASE_INST (PARENT_ID_);
CREATE INDEX ACT_IDX_CASE_INST_REF_ID_ ON ACT_CMMN_RU_CASE_INST (REFERENCE_ID_);

CREATE TABLE ACT_CMMN_RU_PLAN_ITEM_INST
(
    ID_                     VARCHAR(255)            NOT NULL,
    REV_                    INT                     NOT NULL,
    CASE_DEF_ID_            VARCHAR(255)            NULL,
    CASE_INST_ID_           VARCHAR(255)            NULL,
    STAGE_INST_ID_          VARCHAR(255)            NULL,
    IS_STAGE_               TINYINT                 NULL,
    ELEMENT_ID_             VARCHAR(255)            NULL,
    NAME_                   VARCHAR(255)            NULL,
    STATE_                  VARCHAR(255)            NULL,
    CREATE_TIME_            datetime(3)             NULL,
    START_USER_ID_          VARCHAR(255)            NULL,
    ASSIGNEE_               VARCHAR(255)            NULL,
    COMPLETED_BY_           VARCHAR(255)            NULL,
    REFERENCE_ID_           VARCHAR(255)            NULL,
    REFERENCE_TYPE_         VARCHAR(255)            NULL,
    ITEM_DEFINITION_ID_     VARCHAR(255)            NULL,
    ITEM_DEFINITION_TYPE_   VARCHAR(255)            NULL,
    IS_COMPLETEABLE_        TINYINT                 NULL,
    IS_COUNT_ENABLED_       TINYINT                 NULL,
    VAR_COUNT_              INT                     NULL,
    SENTRY_PART_INST_COUNT_ INT                     NULL,
    LAST_AVAILABLE_TIME_    datetime(3)             NULL,
    LAST_ENABLED_TIME_      datetime(3)             NULL,
    LAST_DISABLED_TIME_     datetime(3)             NULL,
    LAST_STARTED_TIME_      datetime(3)             NULL,
    LAST_SUSPENDED_TIME_    datetime(3)             NULL,
    COMPLETED_TIME_         datetime(3)             NULL,
    OCCURRED_TIME_          datetime(3)             NULL,
    TERMINATED_TIME_        datetime(3)             NULL,
    EXIT_TIME_              datetime(3)             NULL,
    ENDED_TIME_             datetime(3)             NULL,
    ENTRY_CRITERION_ID_     VARCHAR(255)            NULL,
    EXIT_CRITERION_ID_      VARCHAR(255)            NULL,
    EXTRA_VALUE_            VARCHAR(255)            NULL,
    DERIVED_CASE_DEF_ID_    VARCHAR(255)            NULL,
    LAST_UNAVAILABLE_TIME_  datetime(3)             NULL,
    TENANT_ID_              VARCHAR(255) DEFAULT '' NULL,
    CONSTRAINT PK_CMMN_PLAN_ITEM_INST PRIMARY KEY (ID_)
);

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST
    ADD CONSTRAINT ACT_FK_PLAN_ITEM_CASE_DEF FOREIGN KEY (CASE_DEF_ID_) REFERENCES ACT_CMMN_CASEDEF (ID_);
CREATE INDEX ACT_IDX_PLAN_ITEM_CASE_DEF ON ACT_CMMN_RU_PLAN_ITEM_INST (CASE_DEF_ID_);

ALTER TABLE ACT_CMMN_RU_PLAN_ITEM_INST
    ADD CONSTRAINT ACT_FK_PLAN_ITEM_CASE_INST FOREIGN KEY (CASE_INST_ID_) REFERENCES ACT_CMMN_RU_CASE_INST (ID_);
CREATE INDEX ACT_IDX_PLAN_ITEM_CASE_INST ON ACT_CMMN_RU_PLAN_ITEM_INST (CASE_INST_ID_);

CREATE INDEX ACT_IDX_PLAN_ITEM_STAGE_INST ON ACT_CMMN_RU_PLAN_ITEM_INST (STAGE_INST_ID_);

CREATE TABLE ACT_CMMN_RU_SENTRY_PART_INST
(
    ID_                VARCHAR(255) NOT NULL,
    REV_               INT          NOT NULL,
    CASE_DEF_ID_       VARCHAR(255) NULL,
    CASE_INST_ID_      VARCHAR(255) NULL,
    PLAN_ITEM_INST_ID_ VARCHAR(255) NULL,
    ON_PART_ID_        VARCHAR(255) NULL,
    IF_PART_ID_        VARCHAR(255) NULL,
    TIME_STAMP_        datetime(3)  NULL,
    CONSTRAINT PK_CMMN_SENTRY_PART_INST PRIMARY KEY (ID_)
);

ALTER TABLE ACT_CMMN_RU_SENTRY_PART_INST
    ADD CONSTRAINT ACT_FK_SENTRY_CASE_DEF FOREIGN KEY (CASE_DEF_ID_) REFERENCES ACT_CMMN_CASEDEF (ID_);
CREATE INDEX ACT_IDX_SENTRY_CASE_DEF ON ACT_CMMN_RU_SENTRY_PART_INST (CASE_DEF_ID_);

ALTER TABLE ACT_CMMN_RU_SENTRY_PART_INST
    ADD CONSTRAINT ACT_FK_SENTRY_CASE_INST FOREIGN KEY (CASE_INST_ID_) REFERENCES ACT_CMMN_RU_CASE_INST (ID_);
CREATE INDEX ACT_IDX_SENTRY_CASE_INST ON ACT_CMMN_RU_SENTRY_PART_INST (CASE_INST_ID_);

ALTER TABLE ACT_CMMN_RU_SENTRY_PART_INST
    ADD CONSTRAINT ACT_FK_SENTRY_PLAN_ITEM FOREIGN KEY (PLAN_ITEM_INST_ID_) REFERENCES ACT_CMMN_RU_PLAN_ITEM_INST (ID_);
CREATE INDEX ACT_IDX_SENTRY_PLAN_ITEM ON ACT_CMMN_RU_SENTRY_PART_INST (PLAN_ITEM_INST_ID_);

CREATE TABLE ACT_CMMN_RU_MIL_INST
(
    ID_           VARCHAR(255)            NOT NULL,
    NAME_         VARCHAR(255)            NOT NULL,
    TIME_STAMP_   datetime(3)             NOT NULL,
    CASE_INST_ID_ VARCHAR(255)            NOT NULL,
    CASE_DEF_ID_  VARCHAR(255)            NOT NULL,
    ELEMENT_ID_   VARCHAR(255)            NOT NULL,
    TENANT_ID_    VARCHAR(255) DEFAULT '' NULL,
    CONSTRAINT PK_ACT_CMMN_RU_MIL_INST PRIMARY KEY (ID_)
);

ALTER TABLE ACT_CMMN_RU_MIL_INST
    ADD CONSTRAINT ACT_FK_MIL_CASE_DEF FOREIGN KEY (CASE_DEF_ID_) REFERENCES ACT_CMMN_CASEDEF (ID_);
CREATE INDEX ACT_IDX_MIL_CASE_DEF ON ACT_CMMN_RU_MIL_INST (CASE_DEF_ID_);

ALTER TABLE ACT_CMMN_RU_MIL_INST
    ADD CONSTRAINT ACT_FK_MIL_CASE_INST FOREIGN KEY (CASE_INST_ID_) REFERENCES ACT_CMMN_RU_CASE_INST (ID_);
CREATE INDEX ACT_IDX_MIL_CASE_INST ON ACT_CMMN_RU_MIL_INST (CASE_INST_ID_);

CREATE TABLE ACT_CMMN_HI_CASE_INST
(
    ID_                        VARCHAR(255)            NOT NULL,
    REV_                       INT                     NOT NULL,
    BUSINESS_KEY_              VARCHAR(255)            NULL,
    NAME_                      VARCHAR(255)            NULL,
    PARENT_ID_                 VARCHAR(255)            NULL,
    CASE_DEF_ID_               VARCHAR(255)            NULL,
    STATE_                     VARCHAR(255)            NULL,
    START_TIME_                datetime(3)             NULL,
    END_TIME_                  datetime(3)             NULL,
    START_USER_ID_             VARCHAR(255)            NULL,
    CALLBACK_ID_               VARCHAR(255)            NULL,
    CALLBACK_TYPE_             VARCHAR(255)            NULL,
    REFERENCE_ID_              VARCHAR(255)            NULL,
    REFERENCE_TYPE_            VARCHAR(255)            NULL,
    LAST_REACTIVATION_TIME_    datetime(3)             NULL,
    LAST_REACTIVATION_USER_ID_ VARCHAR(255)            NULL,
    BUSINESS_STATUS_           VARCHAR(255)            NULL,
    TENANT_ID_                 VARCHAR(255) DEFAULT '' NULL,
    END_USER_ID_               VARCHAR(255)            NULL,
    CONSTRAINT PK_ACT_CMMN_HI_CASE_INST PRIMARY KEY (ID_)
);

CREATE INDEX ACT_IDX_HI_CASE_INST_END ON ACT_CMMN_HI_CASE_INST (END_TIME_);

CREATE TABLE ACT_CMMN_HI_MIL_INST
(
    ID_           VARCHAR(255)            NOT NULL,
    REV_          INT                     NOT NULL,
    NAME_         VARCHAR(255)            NOT NULL,
    TIME_STAMP_   datetime(3)             NOT NULL,
    CASE_INST_ID_ VARCHAR(255)            NOT NULL,
    CASE_DEF_ID_  VARCHAR(255)            NOT NULL,
    ELEMENT_ID_   VARCHAR(255)            NOT NULL,
    TENANT_ID_    VARCHAR(255) DEFAULT '' NULL,
    CONSTRAINT PK_ACT_CMMN_HI_MIL_INST PRIMARY KEY (ID_)
);

CREATE TABLE ACT_CMMN_HI_PLAN_ITEM_INST
(
    ID_                    VARCHAR(255)            NOT NULL,
    REV_                   INT                     NOT NULL,
    NAME_                  VARCHAR(255)            NULL,
    STATE_                 VARCHAR(255)            NULL,
    CASE_DEF_ID_           VARCHAR(255)            NULL,
    CASE_INST_ID_          VARCHAR(255)            NULL,
    STAGE_INST_ID_         VARCHAR(255)            NULL,
    IS_STAGE_              TINYINT                 NULL,
    ELEMENT_ID_            VARCHAR(255)            NULL,
    ITEM_DEFINITION_ID_    VARCHAR(255)            NULL,
    ITEM_DEFINITION_TYPE_  VARCHAR(255)            NULL,
    CREATE_TIME_           datetime(3)             NULL,
    LAST_AVAILABLE_TIME_   datetime(3)             NULL,
    LAST_ENABLED_TIME_     datetime(3)             NULL,
    LAST_DISABLED_TIME_    datetime(3)             NULL,
    LAST_STARTED_TIME_     datetime(3)             NULL,
    LAST_SUSPENDED_TIME_   datetime(3)             NULL,
    COMPLETED_TIME_        datetime(3)             NULL,
    OCCURRED_TIME_         datetime(3)             NULL,
    TERMINATED_TIME_       datetime(3)             NULL,
    EXIT_TIME_             datetime(3)             NULL,
    ENDED_TIME_            datetime(3)             NULL,
    LAST_UPDATED_TIME_     datetime(3)             NULL,
    START_USER_ID_         VARCHAR(255)            NULL,
    ASSIGNEE_              VARCHAR(255)            NULL,
    COMPLETED_BY_          VARCHAR(255)            NULL,
    REFERENCE_ID_          VARCHAR(255)            NULL,
    REFERENCE_TYPE_        VARCHAR(255)            NULL,
    ENTRY_CRITERION_ID_    VARCHAR(255)            NULL,
    EXIT_CRITERION_ID_     VARCHAR(255)            NULL,
    SHOW_IN_OVERVIEW_      TINYINT                 NULL,
    EXTRA_VALUE_           VARCHAR(255)            NULL,
    DERIVED_CASE_DEF_ID_   VARCHAR(255)            NULL,
    LAST_UNAVAILABLE_TIME_ datetime(3)             NULL,
    TENANT_ID_             VARCHAR(255) DEFAULT '' NULL,
    CONSTRAINT PK_ACT_CMMN_HI_PLAN_ITEM_INST PRIMARY KEY (ID_)
);

CREATE INDEX ACT_IDX_HI_PLAN_ITEM_INST_CASE ON ACT_CMMN_HI_PLAN_ITEM_INST (CASE_INST_ID_);

insert into ACT_GE_PROPERTY
values ('cmmn.schema.version', '8.0.0.0', 1);
