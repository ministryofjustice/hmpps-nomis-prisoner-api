create table OFFENDER_SUB_ACCOUNTS
(
    CASELOAD_ID                   VARCHAR2(6 char)                       not null,
    OFFENDER_ID                   NUMBER(10)                             not null,
    TRUST_ACCOUNT_CODE            NUMBER(6)                              not null,
    BALANCE                       NUMBER(11, 2),
    MINIMUM_BALANCE               NUMBER(11, 2),
    HOLD_BALANCE                  NUMBER(11, 2),
    LAST_TXN_ID                   NUMBER(10),
    MODIFY_DATE                   DATE                                   not null,
    MODIFY_USER_ID                VARCHAR2(32 char),
    LIST_SEQ                      NUMBER(6)         default 99,
    IND_DATE                      DATE,
    IND_DAYS                      NUMBER(9),
    CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
    CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
    MODIFY_DATETIME               TIMESTAMP(9),
    AUDIT_TIMESTAMP               TIMESTAMP(9),
    AUDIT_USER_ID                 VARCHAR2(32 char),
    AUDIT_MODULE_NAME             VARCHAR2(65 char),
    AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
    AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
    constraint OFFENDER_SUB_ACCOUNTS_PK
        primary key (CASELOAD_ID, OFFENDER_ID, TRUST_ACCOUNT_CODE),
    constraint OFFENDER_SUB_ACCOUNTS_FK10
        foreign key (OFFENDER_ID) references OFFENDERS,
    constraint OFF_SUBA_OFF_TA_F3
        foreign key (CASELOAD_ID, OFFENDER_ID) references OFFENDER_TRUST_ACCOUNTS
);

comment on column OFFENDER_SUB_ACCOUNTS.CASELOAD_ID is 'The Case Load Identifier';

comment on column OFFENDER_SUB_ACCOUNTS.OFFENDER_ID is 'The Related Offender Identifier';

comment on column OFFENDER_SUB_ACCOUNTS.MODIFY_DATE is 'The data modified date ';

comment on column OFFENDER_SUB_ACCOUNTS.MODIFY_USER_ID is 'The user who modifies the record';

comment on column OFFENDER_SUB_ACCOUNTS.LIST_SEQ is 'The sequence in which the data should be shown';

comment on column OFFENDER_SUB_ACCOUNTS.CREATE_DATETIME is 'The timestamp when the record is created';

comment on column OFFENDER_SUB_ACCOUNTS.CREATE_USER_ID is 'The user who creates the record';

comment on column OFFENDER_SUB_ACCOUNTS.MODIFY_DATETIME is 'The timestamp when the record is modified ';

CREATE UNIQUE INDEX OFFENDER_SUB_ACCOUNTS_PK ON OFFENDER_SUB_ACCOUNTS (CASELOAD_ID, OFFENDER_ID, TRUST_ACCOUNT_CODE);

CREATE INDEX OFFENDER_SUB_ACCOUNTS_NI1 ON OFFENDER_SUB_ACCOUNTS (OFFENDER_ID, TRUST_ACCOUNT_CODE);
