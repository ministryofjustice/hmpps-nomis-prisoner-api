create table OFFENDER_FIXED_TERM_RECALLS
(
    OFFENDER_BOOK_ID              NUMBER(10)                             not null
        constraint OFF_FIXED_TERM_RECALLS_PK
            primary key
        constraint OFF_FIXED_TERM_RECALLS_FK
            references OFFENDER_BOOKINGS,
    RETURN_TO_CUSTODY_DATE        DATE                                   not null,
    STAFF_ID                      NUMBER(10)                             not null
        constraint OFF_FIXED_TERM_RECALLS_FK1
            references STAFF_MEMBERS,
    COMMENT_TEXT                  VARCHAR2(4000 char),
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
    RECALL_LENGTH                 NUMBER(12)        default 28           not null
);

comment on table OFFENDER_FIXED_TERM_RECALLS is 'Offender Fixed Term Recalls';

comment on column OFFENDER_FIXED_TERM_RECALLS.OFFENDER_BOOK_ID is 'Offender Booking ID';

comment on column OFFENDER_FIXED_TERM_RECALLS.RETURN_TO_CUSTODY_DATE is 'Offender retrun to custody date';

comment on column OFFENDER_FIXED_TERM_RECALLS.STAFF_ID is 'Offender retrun to custody date confirmed by';

comment on column OFFENDER_FIXED_TERM_RECALLS.COMMENT_TEXT is 'Any comments entered by user';

comment on column OFFENDER_FIXED_TERM_RECALLS.RECALL_LENGTH is 'Fixed term recall sentence length in days';

