create table OFFENDER_OIC_SANCTIONS
(
  OFFENDER_BOOK_ID              NUMBER(10)                             not null
    constraint OFF_OS_OFF_BKG_F1
      references OFFENDER_BOOKINGS,
  SANCTION_SEQ                  NUMBER(6)                              not null,
  OIC_SANCTION_CODE             VARCHAR2(12 char),
  COMPENSATION_AMOUNT           NUMBER(11, 2),
  SANCTION_MONTHS               NUMBER(3),
  SANCTION_DAYS                 NUMBER(3),
  COMMENT_TEXT                  VARCHAR2(240 char),
  EFFECTIVE_DATE                DATE                                   not null,
  APPEALING_DATE                DATE,
  CONSECUTIVE_OFFENDER_BOOK_ID  NUMBER(10)
    constraint OFFENDER_OIC_SANCTIONS_FK9
      references OFFENDER_BOOKINGS,
  CONSECUTIVE_SANCTION_SEQ      NUMBER(6),
  OIC_HEARING_ID                NUMBER(10),
  STATUS                        VARCHAR2(12 char),
  OFFENDER_ADJUST_ID            NUMBER(10),
  RESULT_SEQ                    NUMBER(6),
  CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
  CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
  MODIFY_DATETIME               TIMESTAMP(9),
  MODIFY_USER_ID                VARCHAR2(32 char),
  STATUS_DATE                   DATE,
  AUDIT_TIMESTAMP               TIMESTAMP(9),
  AUDIT_USER_ID                 VARCHAR2(32 char),
  AUDIT_MODULE_NAME             VARCHAR2(65 char),
  AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
  AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
  AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
  AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
  OIC_INCIDENT_ID               NUMBER(10),
  LIDS_SANCTION_NUMBER          NUMBER(6),
  constraint OFFENDER_OIC_SANCTIONS_PK
    primary key (OFFENDER_BOOK_ID, SANCTION_SEQ),
  constraint OFF_OS_OFF_OS_F1
    foreign key (CONSECUTIVE_OFFENDER_BOOK_ID, CONSECUTIVE_SANCTION_SEQ) references OFFENDER_OIC_SANCTIONS,
  constraint OIC_OS_OIC_HR_FK1
    foreign key (OIC_HEARING_ID, RESULT_SEQ) references OIC_HEARING_RESULTS
)
;

comment on table OFFENDER_OIC_SANCTIONS is 'A punishment imposed upon an offender after having been found guilty of an offence defined in Prison Rule 51 or YOI Rule 55. The touchy-feely New-Labour term for punishment is now award..... A punishment can be made to run consecutively from other punishments imposed as a result of charges arising from unrelated incidents or from the same incident. NOTE : the primary key structure is not logical. It contains the Offender Booking Id of the guilty offender which, in fact, is inherited via parent entities.'
;

comment on column OFFENDER_OIC_SANCTIONS.OFFENDER_BOOK_ID is 'Unique identifer for an offender booking.'
;

comment on column OFFENDER_OIC_SANCTIONS.SANCTION_SEQ is 'Sequence number on sanction forming part of primary key.'
;

comment on column OFFENDER_OIC_SANCTIONS.OIC_SANCTION_CODE is 'Reference Code ( OIC_SANCT )'
;

comment on column OFFENDER_OIC_SANCTIONS.COMPENSATION_AMOUNT is 'Penalty involving compensation amount.'
;

comment on column OFFENDER_OIC_SANCTIONS.SANCTION_MONTHS is 'Penalty months imposed against sentences.'
;

comment on column OFFENDER_OIC_SANCTIONS.SANCTION_DAYS is 'The number of penalty days imposed against sentences.'
;

comment on column OFFENDER_OIC_SANCTIONS.COMMENT_TEXT is 'Pop-up edit window allowing penalty comments.'
;

comment on column OFFENDER_OIC_SANCTIONS.EFFECTIVE_DATE is 'Effective date for penalty.'
;

comment on column OFFENDER_OIC_SANCTIONS.APPEALING_DATE is 'Date of appeal.'
;

comment on column OFFENDER_OIC_SANCTIONS.CONSECUTIVE_OFFENDER_BOOK_ID is 'FK to OIC sanction'
;

comment on column OFFENDER_OIC_SANCTIONS.CONSECUTIVE_SANCTION_SEQ is 'Specification of specific penalty that this may be consecutive to.'
;

comment on column OFFENDER_OIC_SANCTIONS.OIC_HEARING_ID is 'FK to OIC snaction'
;

comment on column OFFENDER_OIC_SANCTIONS.STATUS is 'Referece Code (OIC_SANCT_STS)'
;

comment on column OFFENDER_OIC_SANCTIONS.OFFENDER_ADJUST_ID is 'FK Offender OIC Appeal Penalty'
;

comment on column OFFENDER_OIC_SANCTIONS.RESULT_SEQ is 'Sequential number for hearing results'
;

comment on column OFFENDER_OIC_SANCTIONS.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column OFFENDER_OIC_SANCTIONS.CREATE_USER_ID is 'The user who creates the record'
;

comment on column OFFENDER_OIC_SANCTIONS.MODIFY_DATETIME is 'The timestamp when the record is modified '
;

comment on column OFFENDER_OIC_SANCTIONS.MODIFY_USER_ID is 'The user who modifies the record'
;

comment on column OFFENDER_OIC_SANCTIONS.STATUS_DATE is 'The date when the status changed'
;

