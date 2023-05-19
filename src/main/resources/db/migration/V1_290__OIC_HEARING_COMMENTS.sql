create table OIC_HEARING_COMMENTS
(
 OIC_HEARING_ID                NUMBER(10)                             not null
  constraint OIC_HC_OIC_HEAR_FK1
   references OIC_HEARINGS,
 HEARING_COMMENT_SEQ           NUMBER(6)         default 1            not null,
 COMMENT_TEXT                  VARCHAR2(4000 char)                    not null,
 MODIFY_USER_ID                VARCHAR2(32 char) default USER,
 MODIFY_DATETIME               TIMESTAMP(9)      default SYSDATE,
 CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
 CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
 AUDIT_TIMESTAMP               TIMESTAMP(9),
 AUDIT_USER_ID                 VARCHAR2(32 char),
 AUDIT_MODULE_NAME             VARCHAR2(65 char),
 AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
 AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
 AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
 AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
 constraint OIC_HEARING_COMMENTS_PK
  primary key (OIC_HEARING_ID, HEARING_COMMENT_SEQ)
)
;

comment on table OIC_HEARING_COMMENTS is 'The comment of an OIC Hearing'
;

comment on column OIC_HEARING_COMMENTS.OIC_HEARING_ID is 'FK to OIC Hearings'
;

comment on column OIC_HEARING_COMMENTS.HEARING_COMMENT_SEQ is 'The comment seq as part of the PK'
;

comment on column OIC_HEARING_COMMENTS.COMMENT_TEXT is 'The timestamp when the record is modified '
;

comment on column OIC_HEARING_COMMENTS.MODIFY_USER_ID is 'The user who modifies the record'
;

comment on column OIC_HEARING_COMMENTS.MODIFY_DATETIME is 'The comment'
;

comment on column OIC_HEARING_COMMENTS.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column OIC_HEARING_COMMENTS.CREATE_USER_ID is 'The user who creates the record'
;

