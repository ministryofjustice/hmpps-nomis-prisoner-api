create table OFFENDER_LICENCE_SENTENCES
(
    OFFENDER_BOOK_ID              NUMBER(10)                             not null
        constraint OFFENDER_LICENCE_SENTENCES_FK9
            references OFFENDER_BOOKINGS,
    SENTENCE_SEQ                  NUMBER(6)                              not null,
    CASE_ID                       NUMBER(10)                             not null
        constraint OFFENDER_LICENCE_SENTENCES_FK1
            references OFFENDER_CASES,
    LICENCE_SENTENCE_SEQ          NUMBER(6)                              not null,
    CREATION_DATE                 DATE,
    CREATION_USER                 VARCHAR2(32 char),
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
    constraint OFFENDER_LICENCE_SENTENCES_FK2
        foreign key (OFFENDER_BOOK_ID, SENTENCE_SEQ) references OFFENDER_SENTENCES,
    constraint OFFENDER_LICENCE_SENTENCES_FK3
        foreign key (OFFENDER_BOOK_ID, LICENCE_SENTENCE_SEQ) references OFFENDER_SENTENCES
);

comment on table OFFENDER_LICENCE_SENTENCES is 'This table holds the sentence to licence link';
