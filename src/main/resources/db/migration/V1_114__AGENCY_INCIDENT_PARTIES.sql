create table AGENCY_INCIDENT_PARTIES
(
    AGENCY_INCIDENT_ID            NUMBER(10)                             not null
        constraint AGY_INCP_AGY_INC_F1
            references AGENCY_INCIDENTS,
    PARTY_SEQ                     NUMBER(6)         default 1            not null,
    INCIDENT_ROLE                 VARCHAR2(12 char)                      not null,
    OFFENDER_BOOK_ID              NUMBER(10)
        constraint AGY_INC_PTY_OFF_BKG_FK
            references OFFENDER_BOOKINGS,
    STAFF_ID                      NUMBER(10)
        constraint AGY_INCP_STAFF_F1
            references STAFF_MEMBERS,
    PERSON_ID                     NUMBER(10)
        constraint AGY_INC_PTY_PER_FK
            references PERSONS,
    DISPOSITION_TYPE              VARCHAR2(12 char),
    DISPOSITION_DATE              DATE,
    OIC_INCIDENT_ID               NUMBER(10)
        constraint AGENCY_INCIDENT_PARTIES_UK1
            unique,
    COMMENT_TEXT                  VARCHAR2(240 char),
    CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
    CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
    ACTION_CODE                   VARCHAR2(12 char),
    PARTY_ADDED_DATE              DATE                                   not null,
    MODIFY_DATETIME               TIMESTAMP(9),
    MODIFY_USER_ID                VARCHAR2(32 char),
    AUDIT_TIMESTAMP               TIMESTAMP(9),
    AUDIT_USER_ID                 VARCHAR2(32 char),
    AUDIT_MODULE_NAME             VARCHAR2(65 char),
    AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
    AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
    constraint AGENCY_INCIDENT_PARTIES_PK
        primary key (AGENCY_INCIDENT_ID, PARTY_SEQ)
)
;

comment on table AGENCY_INCIDENT_PARTIES is 'A Staff Member or Offender who was involved in an event which may have given rise to one or more breaches of Prison Rule 51 or YOI Rule 55.'
;

comment on column AGENCY_INCIDENT_PARTIES.AGENCY_INCIDENT_ID is 'Incident ID'
;

comment on column AGENCY_INCIDENT_PARTIES.PARTY_SEQ is 'Sequence'
;

comment on column AGENCY_INCIDENT_PARTIES.INCIDENT_ROLE is 'Reference Code ( INC_ROLE )'
;

comment on column AGENCY_INCIDENT_PARTIES.OFFENDER_BOOK_ID is 'FK : Offender Bookings'
;

comment on column AGENCY_INCIDENT_PARTIES.STAFF_ID is 'FK : Staff Members'
;

comment on column AGENCY_INCIDENT_PARTIES.PERSON_ID is 'FK : Persons'
;

comment on column AGENCY_INCIDENT_PARTIES.DISPOSITION_TYPE is 'Reference Code (INC_DECISION)'
;

comment on column AGENCY_INCIDENT_PARTIES.DISPOSITION_DATE is 'The date of the disposition decision'
;

comment on column AGENCY_INCIDENT_PARTIES.OIC_INCIDENT_ID is 'The Adjudication No'
;

comment on column AGENCY_INCIDENT_PARTIES.COMMENT_TEXT is 'The general comment text'
;

comment on column AGENCY_INCIDENT_PARTIES.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column AGENCY_INCIDENT_PARTIES.CREATE_USER_ID is 'The user who creates the record'
;

comment on column AGENCY_INCIDENT_PARTIES.ACTION_CODE is 'Reference Code (INC_DECISION)'
;

comment on column AGENCY_INCIDENT_PARTIES.MODIFY_DATETIME is 'The timestamp when the record is modified '
;

comment on column AGENCY_INCIDENT_PARTIES.MODIFY_USER_ID is 'The user who modifies the record'
;

