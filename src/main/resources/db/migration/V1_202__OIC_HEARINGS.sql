create table OIC_HEARINGS
(
    OIC_HEARING_ID                NUMBER(10)                             not null
        constraint OIC_HEARINGS_PK
            primary key,
    OIC_HEARING_TYPE              VARCHAR2(12 char),
    OIC_INCIDENT_ID               NUMBER(10)                             not null
        constraint OIC_HR_AGY_INC_PTY_FK
            references AGENCY_INCIDENT_PARTIES (OIC_INCIDENT_ID),
    SCHEDULE_DATE                 DATE,
    SCHEDULE_TIME                 DATE,
    HEARING_DATE                  DATE,
    HEARING_TIME                  DATE,
    HEARING_STAFF_ID              NUMBER(10),
    VISIT_JUSTICE_TEXT            VARCHAR2(40 char),
    COMMENT_TEXT                  VARCHAR2(240 char),
    TAPE_NUMBER                   VARCHAR2(12 char),
    CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
    CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
    MODIFY_DATETIME               TIMESTAMP(9),
    MODIFY_USER_ID                VARCHAR2(32 char),
    INTERNAL_LOCATION_ID          NUMBER(10)
        constraint OIC_HEAR_AGY_INT_LOC_FK
            references AGENCY_INTERNAL_LOCATIONS,
    REPRESENTATIVE_TEXT           VARCHAR2(240 char),
    EVENT_ID                      NUMBER(10),
    AUDIT_TIMESTAMP               TIMESTAMP(9),
    AUDIT_USER_ID                 VARCHAR2(32 char),
    AUDIT_MODULE_NAME             VARCHAR2(65 char),
    AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
    AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
    EVENT_STATUS                  VARCHAR2(12 char) default 'SCH'
)
;

comment on table OIC_HEARINGS is 'An formal meeting presided over by an individual (the adjudicator) empowered to inquire into the report of an offenders alleged involvement in an Agency Incident, to decide whether a breach of Prison Rule 51 or YOI Rule 55 has been established beyond reasonable doubt, and, if the offender is found guilty, to impose punishment. NOTE1 : there can be more than one hearing per adjudication in circumstances where, for instance, a hearing is opened and then immediately adjourned (eg. to await further information or to pass to an independent adjudicator). In such cases there will be no related OIC Hearing Result. NOTE2 : for increment one an occurrence of Offender Schedules is not created. That will change at some point during Release 0. When the change is implemented it will be incorporated into this diagram.'
;

comment on column OIC_HEARINGS.OIC_HEARING_ID is 'The Hearing ID of OIC Hearing'
;

comment on column OIC_HEARINGS.OIC_HEARING_TYPE is 'Reference Code [ OIC_HEAR]'
;

comment on column OIC_HEARINGS.OIC_INCIDENT_ID is 'FK to agency incident parties'
;

comment on column OIC_HEARINGS.SCHEDULE_DATE is 'The date scheduled for the OIC hearing.'
;

comment on column OIC_HEARINGS.SCHEDULE_TIME is 'Schedule Time for hearing.'
;

comment on column OIC_HEARINGS.HEARING_DATE is 'Hearing Date.'
;

comment on column OIC_HEARINGS.HEARING_TIME is 'Hearing time.'
;

comment on column OIC_HEARINGS.HEARING_STAFF_ID is 'Hearing Staff ID.'
;

comment on column OIC_HEARINGS.VISIT_JUSTICE_TEXT is 'Visiting Justice hearing case - if applicable.'
;

comment on column OIC_HEARINGS.COMMENT_TEXT is 'Comment of the OIC Hearing'
;

comment on column OIC_HEARINGS.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column OIC_HEARINGS.CREATE_USER_ID is 'The user who creates the record'
;

comment on column OIC_HEARINGS.MODIFY_DATETIME is 'The timestamp when the record is modified '
;

comment on column OIC_HEARINGS.MODIFY_USER_ID is 'The user who modifies the record'
;

comment on column OIC_HEARINGS.INTERNAL_LOCATION_ID is 'FK to agency internal locations.'
;

comment on column OIC_HEARINGS.REPRESENTATIVE_TEXT is 'other represnetative text of the offender'
;

