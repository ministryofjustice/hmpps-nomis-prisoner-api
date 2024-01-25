create table QUESTIONNAIRE_ROLES
(
    QUESTIONNAIRE_ID              NUMBER(10)                             not null
        constraint QUE_ROLE_QUE_FK
        references QUESTIONNAIRES,
    PARTICIPATION_ROLE            VARCHAR2(12 char)                      not null,
    SINGLE_ROLE_FLAG              VARCHAR2(1 char)  default 'N'          not null,
    ACTIVE_FLAG                   VARCHAR2(1 char)  default 'Y'          not null,
    EXPIRY_DATE                   DATE,
    CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
    CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
    MODIFY_DATETIME               TIMESTAMP(9),
    MODIFY_USER_ID                VARCHAR2(32 char),
    LIST_SEQ                      NUMBER(6),
    AUDIT_TIMESTAMP               TIMESTAMP(9),
    AUDIT_USER_ID                 VARCHAR2(32 char),
    AUDIT_MODULE_NAME             VARCHAR2(65 char),
    AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
    AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
    constraint INCIDENT_TYPE_ROLES_PK
        primary key (QUESTIONNAIRE_ID, PARTICIPATION_ROLE)
);

comment on table QUESTIONNAIRE_ROLES is 'The offender party roles on a questionnaire';
comment on column QUESTIONNAIRE_ROLES.QUESTIONNAIRE_ID is 'Reference Code (IR_TYPE)';
comment on column QUESTIONNAIRE_ROLES.PARTICIPATION_ROLE is 'Reference Code (IR_OFF_ROLE) - incorrect on IR_OFF_PART';
comment on column QUESTIONNAIRE_ROLES.SINGLE_ROLE_FLAG is 'If a single party can have this role in the incident case';
comment on column QUESTIONNAIRE_ROLES.ACTIVE_FLAG is 'If the record active';
comment on column QUESTIONNAIRE_ROLES.EXPIRY_DATE is 'The expiry if the record is inactive';
comment on column QUESTIONNAIRE_ROLES.CREATE_DATETIME is 'The timestamp when the record is created';
comment on column QUESTIONNAIRE_ROLES.CREATE_USER_ID is 'The user who creates the record';
comment on column QUESTIONNAIRE_ROLES.MODIFY_DATETIME is 'The timestamp when the record is modified';
comment on column QUESTIONNAIRE_ROLES.MODIFY_USER_ID is 'The user who modifies the record';
comment on column QUESTIONNAIRE_ROLES.LIST_SEQ is 'The listing order';
