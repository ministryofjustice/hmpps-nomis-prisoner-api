create table AGENCY_INCIDENT_REPAIRS
(
    AGENCY_INCIDENT_ID            NUMBER(10)                             not null
        constraint AGY_INC_RPR_AGY_INC_FK
            references AGENCY_INCIDENTS,
    REPAIR_SEQ                    NUMBER(6)         default 1            not null,
    REPAIR_TYPE                   VARCHAR2(12 char)                      not null,
    COMMENT_TEXT                  VARCHAR2(4000 char),
    MODIFY_USER_ID                VARCHAR2(32 char),
    MODIFY_DATETIME               TIMESTAMP(9),
    REPAIR_COST                   NUMBER(12, 2),
    CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
    CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
    AUDIT_TIMESTAMP               TIMESTAMP(9),
    AUDIT_USER_ID                 VARCHAR2(32 char),
    AUDIT_MODULE_NAME             VARCHAR2(65 char),
    AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
    AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
    constraint AGENCY_INCIDENT_REPAIRS_PK
        primary key (AGENCY_INCIDENT_ID, REPAIR_SEQ)
)
;

comment on table AGENCY_INCIDENT_REPAIRS is 'A repair to prison property necessary as a result of damage caused during an Agency Incident.'
;

comment on column AGENCY_INCIDENT_REPAIRS.AGENCY_INCIDENT_ID is 'Agency incident ID'
;

comment on column AGENCY_INCIDENT_REPAIRS.REPAIR_SEQ is 'The repair seq as part of the PK'
;

comment on column AGENCY_INCIDENT_REPAIRS.REPAIR_TYPE is 'Reference Code (REPAIR)'
;

comment on column AGENCY_INCIDENT_REPAIRS.COMMENT_TEXT is 'The dollar cost of the repair'
;

comment on column AGENCY_INCIDENT_REPAIRS.MODIFY_USER_ID is 'The user who modifies the record'
;

comment on column AGENCY_INCIDENT_REPAIRS.MODIFY_DATETIME is 'The timestamp when the record is modified '
;

comment on column AGENCY_INCIDENT_REPAIRS.REPAIR_COST is 'The timestamp when the record is modified '
;

comment on column AGENCY_INCIDENT_REPAIRS.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column AGENCY_INCIDENT_REPAIRS.CREATE_USER_ID is 'The user who creates the record'
;

