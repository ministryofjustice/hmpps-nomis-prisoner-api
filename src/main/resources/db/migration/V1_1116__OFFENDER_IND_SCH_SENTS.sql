create table OFFENDER_IND_SCH_SENTS
(
    EVENT_ID                      NUMBER(10)                             not null
        constraint OFF_IND_SS_OFF_IND_SCH_FK
            references OFFENDER_IND_SCHEDULES,
    OFFENDER_BOOK_ID              NUMBER(10)                             not null
        constraint OFFENDER_IND_SCH_SENTS_FK9
            references OFFENDER_BOOKINGS,
    SENTENCE_SEQ                  NUMBER(6)                              not null,
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
    constraint OFFENDE_IND_SCH_SENTS_PK
        primary key (EVENT_ID, OFFENDER_BOOK_ID, SENTENCE_SEQ),
    constraint OFF_IND_SS_OFF_SENT_FK
        foreign key (OFFENDER_BOOK_ID, SENTENCE_SEQ) references OFFENDER_SENTENCES
);

comment on column OFFENDER_IND_SCH_SENTS.OFFENDER_BOOK_ID is 'The Related Offender Book Identifier';
comment on column OFFENDER_IND_SCH_SENTS.CREATE_DATETIME is 'The timestamp when the record is created';
comment on column OFFENDER_IND_SCH_SENTS.CREATE_USER_ID is 'The user who creates the record';
comment on column OFFENDER_IND_SCH_SENTS.MODIFY_DATETIME is 'The timestamp when the record is modified ';
comment on column OFFENDER_IND_SCH_SENTS.MODIFY_USER_ID is 'The user who modifies the record';