create table OIC_HEARING_RESULTS
(
  OIC_HEARING_ID                NUMBER(10)                             not null
    constraint OIC_HR_OIC_HEAR_F1
      references OIC_HEARINGS,
  RESULT_SEQ                    NUMBER(6)                              not null,
  AGENCY_INCIDENT_ID            NUMBER(10)                             not null,
  CHARGE_SEQ                    NUMBER(6)                              not null,
  PLEA_FINDING_CODE             VARCHAR2(12 char)                      not null,
  FINDING_CODE                  VARCHAR2(12 char)                      not null,
  CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
  CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
  MODIFY_DATETIME               TIMESTAMP(9),
  MODIFY_USER_ID                VARCHAR2(32 char),
  AUDIT_TIMESTAMP               TIMESTAMP(9),
  AUDIT_USER_ID                 VARCHAR2(32 char),
  AUDIT_MODULE_NAME             VARCHAR2(65 char),
  AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
  AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
  AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
  AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
  OIC_OFFENCE_ID                NUMBER(10)                             not null
    constraint OIC_HEARING_RESULTS_FK1
      references OIC_OFFENCES,
  constraint OIC_HEARING_RESULTS_PK
    primary key (OIC_HEARING_ID, RESULT_SEQ),
  constraint OIC_HEARING_RESULTS_UK
    unique (OIC_HEARING_ID, AGENCY_INCIDENT_ID, CHARGE_SEQ),
  constraint OIC_HR_OIC_AGY_INC_CHG_FK
    foreign key (AGENCY_INCIDENT_ID, CHARGE_SEQ) references AGENCY_INCIDENT_CHARGES
)
;

comment on table OIC_HEARING_RESULTS is 'The outcome of an adjudication hearing in respect of a specific charge. NOTE1 : there cannot be more than one result per charge. If a result is quashed on appeal then the status of the result is changed to quashed. NOTE2 : it follows from Note1 above that (a) attributes Plea Finding Code & Finding Code & OIC Hearing Id belong logically within the Agency Incident Charge entity (ie. they are dependent on the Charge - not on both Charge & Hearing). In other words, this entity is logically redundant. Hence, it is represented here with a 1:1 relationship with Agency incident Charge. NOTE3 : as per the comment on parent entity Agency Incident Charge, the FK inherited from that parent is a natural key rather than the physical primary key of the parent'
;

comment on column OIC_HEARING_RESULTS.OIC_HEARING_ID is 'System generated primary key for hearing.'
;

comment on column OIC_HEARING_RESULTS.RESULT_SEQ is 'Sequential number for hearing results.'
;

comment on column OIC_HEARING_RESULTS.AGENCY_INCIDENT_ID is 'System generated seqential log number for the incident.'
;

comment on column OIC_HEARING_RESULTS.CHARGE_SEQ is 'Sequential number for charge.'
;

comment on column OIC_HEARING_RESULTS.PLEA_FINDING_CODE is 'Reference Code ( FINDING ). The offender"s plea on this charge.'
;

comment on column OIC_HEARING_RESULTS.FINDING_CODE is 'Reference Code ( FINDING )'
;

comment on column OIC_HEARING_RESULTS.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column OIC_HEARING_RESULTS.CREATE_USER_ID is 'The user who creates the record'
;

comment on column OIC_HEARING_RESULTS.MODIFY_DATETIME is 'The timestamp when the record is modified '
;

comment on column OIC_HEARING_RESULTS.MODIFY_USER_ID is 'The user who modifies the record'
;

