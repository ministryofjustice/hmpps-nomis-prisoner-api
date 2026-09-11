create table OFFENDER_TEST_SELECTIONS
(
    OFFENDER_BOOK_ID              NUMBER(10)                             not null
        constraint OFFENDER_TEST_SELECTIONS_FK9 references OFFENDER_BOOKINGS,
    RTP_ID                        NUMBER(10)                             not null
        constraint OFF_TEST_SEL_RTP_F1 references RANDOM_TESTING_PROGRAMS on delete cascade,
    TEST_SELECTION_TYPE           VARCHAR2(1 char)                       not null
        constraint OFF_TEST_SEL_TYPE_CHK check (test_selection_type IN ('M', 'R')),
    TEST_SELECTION_NO             NUMBER(5)                              not null,
    TESTED_FLAG                   VARCHAR2(1 char)  default NULL
        constraint OFF_TEST_SEL_TESTED_CHK check (tested_flag IN ('Y', 'N')),
    REASON_NOT_TESTED             VARCHAR2(12 char),
    NOTES                         VARCHAR2(240 char),
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
    constraint OFF_TEST_SEL_PK primary key (OFFENDER_BOOK_ID, RTP_ID)
);

comment on column OFFENDER_TEST_SELECTIONS.OFFENDER_BOOK_ID is 'Offender Book Id';
comment on column OFFENDER_TEST_SELECTIONS.RTP_ID is 'Id of random testing programme';
comment on column OFFENDER_TEST_SELECTIONS.TEST_SELECTION_TYPE is 'Type of selection: (M)ain or (R)eserve';
comment on column OFFENDER_TEST_SELECTIONS.TEST_SELECTION_NO is 'No of selection';
comment on column OFFENDER_TEST_SELECTIONS.TESTED_FLAG is 'Tested Flag, Y or N';
comment on column OFFENDER_TEST_SELECTIONS.REASON_NOT_TESTED is 'Reason not tested';
comment on column OFFENDER_TEST_SELECTIONS.NOTES is 'Notes';
comment on column OFFENDER_TEST_SELECTIONS.CREATE_DATETIME is 'The timestamp when the record is created';
comment on column OFFENDER_TEST_SELECTIONS.CREATE_USER_ID is 'The user who creates the record';
comment on column OFFENDER_TEST_SELECTIONS.MODIFY_DATETIME is 'The timestamp when the record is modified ';
comment on column OFFENDER_TEST_SELECTIONS.MODIFY_USER_ID is 'The user who modifies the record';

CREATE INDEX OFFENDER_TEST_SELECTIONS_NI1 ON OFFENDER_TEST_SELECTIONS (RTP_ID);
