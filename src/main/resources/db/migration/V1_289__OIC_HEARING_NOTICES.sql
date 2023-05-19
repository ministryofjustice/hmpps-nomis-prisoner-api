create table OIC_HEARING_NOTICES
(
 OIC_HEARING_ID                NUMBER(10)                             not null
  constraint OIC_HN_OIC_HR_FK
   references OIC_HEARINGS,
 OIC_NOTICE_SEQ                NUMBER(7)                              not null,
 DELIVERY_DATE                 DATE,
 DELIVERY_TIME                 DATE,
 DELIVERY_STAFF_ID             NUMBER(10)
  constraint OIC_HN_OIC_STAFF_FK
   references STAFF_MEMBERS,
 CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
 CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
 MODIFY_DATETIME               TIMESTAMP(9),
 MODIFY_USER_ID                VARCHAR2(32 char),
 COMMENT_TEXT                  VARCHAR2(240 char),
 AUDIT_TIMESTAMP               TIMESTAMP(9),
 AUDIT_USER_ID                 VARCHAR2(32 char),
 AUDIT_MODULE_NAME             VARCHAR2(65 char),
 AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
 AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
 AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
 AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
 constraint OIC_HEARING_NOTICES_PK
  primary key (OIC_HEARING_ID, OIC_NOTICE_SEQ)
)
;

comment on table OIC_HEARING_NOTICES is 'The delivery to an offender of the Notice (s) of Report concerning the charge(s) laid against him;her which are subject to adjudication. Also referred to as Notifications.'
;

comment on column OIC_HEARING_NOTICES.OIC_HEARING_ID is 'FK to OIC Hearings'
;

comment on column OIC_HEARING_NOTICES.OIC_NOTICE_SEQ is 'The notice seq as part of the PK'
;

comment on column OIC_HEARING_NOTICES.DELIVERY_DATE is 'The date of delivery'
;

comment on column OIC_HEARING_NOTICES.DELIVERY_TIME is 'The time of delivery'
;

comment on column OIC_HEARING_NOTICES.DELIVERY_STAFF_ID is 'FK to Staff_Members. The staff who delivers the notices'
;

comment on column OIC_HEARING_NOTICES.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column OIC_HEARING_NOTICES.CREATE_USER_ID is 'The user who creates the record'
;

comment on column OIC_HEARING_NOTICES.MODIFY_DATETIME is 'The timestamp when the record is modified '
;

comment on column OIC_HEARING_NOTICES.MODIFY_USER_ID is 'The user who modifies the record'
;

comment on column OIC_HEARING_NOTICES.COMMENT_TEXT is 'The comment text'
;

