create table OFFENDER_SENTENCE_STATUSES
(
    OFFENDER_BOOK_ID              NUMBER(10)                             not null
        constraint OFFENDER_SENTENCE_STATUSES_FK9
            references OFFENDER_BOOKINGS,
    SENTENCE_SEQ                  NUMBER(6)                              not null,
    STATUS_UPDATE_REASON          VARCHAR2(12 char)                      not null
        constraint OFF_SENT_STS_LGL_UPD_RSN_FK
            references LEGAL_UPDATE_REASONS,
    STATUS_UPDATE_COMMENT         VARCHAR2(400 char),
    STATUS_UPDATE_DATE            DATE              default sysdate      not null,
    STATUS_UPDATE_STAFF_ID        NUMBER(10)                             not null,
    OFFENDER_SENTENCE_STATUS_ID   NUMBER(10)                             not null
        constraint OFFENDER_SENTENCE_STATUSES_PK
            primary key,
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
    constraint OFF_SENT_STS_OFF_SENT_FK
        foreign key (OFFENDER_BOOK_ID, SENTENCE_SEQ) references OFFENDER_SENTENCES
)
;

comment on column OFFENDER_SENTENCE_STATUSES.OFFENDER_BOOK_ID is 'The offender Book ID'
;

comment on column OFFENDER_SENTENCE_STATUSES.SENTENCE_SEQ is 'The sentence seq'
;

comment on column OFFENDER_SENTENCE_STATUSES.STATUS_UPDATE_REASON is 'The reason of the status update'
;

comment on column OFFENDER_SENTENCE_STATUSES.STATUS_UPDATE_COMMENT is 'The comment of the status update'
;

comment on column OFFENDER_SENTENCE_STATUSES.STATUS_UPDATE_DATE is 'The date of the status date'
;

comment on column OFFENDER_SENTENCE_STATUSES.STATUS_UPDATE_STAFF_ID is 'The staff who update the status'
;

comment on column OFFENDER_SENTENCE_STATUSES.OFFENDER_SENTENCE_STATUS_ID is 'The PK of the status update reason'