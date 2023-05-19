create table AGENCY_INCIDENTS
(
    REPORTED_STAFF_ID             NUMBER(10)                             not null,
    AGENCY_INCIDENT_ID            NUMBER(10)                             not null
        constraint AGENCY_INCIDENTS_PK
            primary key,
    INCIDENT_DATE                 DATE                                   not null,
    INTERNAL_LOCATION_ID          NUMBER(10)                             not null
        constraint AGY_INC_AGY_INTL_F1
            references AGENCY_INTERNAL_LOCATIONS,
    INCIDENT_TIME                 DATE                                   not null,
    INCIDENT_TYPE                 VARCHAR2(12 char)                      not null,
    INCIDENT_STATUS               VARCHAR2(12 char)                      not null,
    CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
    CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
    MODIFY_USER_ID                VARCHAR2(32 char),
    MODIFY_DATETIME               TIMESTAMP(9),
    LOCK_FLAG                     VARCHAR2(1 char)  default 'N'          not null,
    INCIDENT_DETAILS              VARCHAR2(4000 char),
    REPORT_DATE                   DATE                                   not null,
    REPORT_TIME                   DATE                                   not null,
    AGY_LOC_ID                    VARCHAR2(6 char)
        constraint AGY_INC_AGY_LOC_FK
            references AGENCY_LOCATIONS,
    AUDIT_TIMESTAMP               TIMESTAMP(9),
    AUDIT_USER_ID                 VARCHAR2(32 char),
    AUDIT_MODULE_NAME             VARCHAR2(65 char),
    AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
    AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char)
)
;

comment on table AGENCY_INCIDENTS is 'An event which occurred at a prison which may have given rise to one or more breaches of Prison Rule 53 or YOI Rule 55.'
;

comment on column AGENCY_INCIDENTS.REPORTED_STAFF_ID is 'The staff who report the incident'
;

comment on column AGENCY_INCIDENTS.AGENCY_INCIDENT_ID is 'Agency incident ID NOMIS Log number'
;

comment on column AGENCY_INCIDENTS.INCIDENT_DATE is 'Incident Date'
;

comment on column AGENCY_INCIDENTS.INTERNAL_LOCATION_ID is 'Incident Place'
;

comment on column AGENCY_INCIDENTS.INCIDENT_TIME is 'Incident Time'
;

comment on column AGENCY_INCIDENTS.INCIDENT_TYPE is 'Reference Code ( INC_TYPE )'
;

comment on column AGENCY_INCIDENTS.INCIDENT_STATUS is 'Reference Code ( INC_STS )'
;

comment on column AGENCY_INCIDENTS.CREATE_DATETIME is 'The user who creates the record'
;

comment on column AGENCY_INCIDENTS.CREATE_USER_ID is 'The user who modifies the record'
;

comment on column AGENCY_INCIDENTS.MODIFY_USER_ID is 'The timestamp when the record is modified '
;

comment on column AGENCY_INCIDENTS.MODIFY_DATETIME is 'record locked to prevent data changed'
;

comment on column AGENCY_INCIDENTS.LOCK_FLAG is 'The timestamp when the record is created'
;

comment on column AGENCY_INCIDENTS.INCIDENT_DETAILS is 'The Occurence Details'
;

comment on column AGENCY_INCIDENTS.REPORT_DATE is 'The report date of the incident'
;

comment on column AGENCY_INCIDENTS.REPORT_TIME is 'The report time of the incident'
;

comment on column AGENCY_INCIDENTS.AGY_LOC_ID is 'FK to agency locations'
;

