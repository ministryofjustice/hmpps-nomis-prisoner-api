create table OFFENDER_TRUST_ACCOUNTS
(
    CASELOAD_ID                   VARCHAR2(6 char)                       not null,
    OFFENDER_ID                   NUMBER(10)                             not null,
    ACCOUNT_CLOSED_FLAG           VARCHAR2(1 char)  default 'Y'          not null,
    HOLD_BALANCE                  NUMBER(11, 2),
    CURRENT_BALANCE               NUMBER(11, 2),
    MODIFY_DATE                   DATE                                   not null,
    MODIFY_USER_ID                VARCHAR2(32 char),
    LIST_SEQ                      NUMBER(6)         default 99,
    NOTIFY_DATE                   DATE,
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
    constraint OFFENDER_TRUST_ACCOUNT_PK
        primary key (CASELOAD_ID, OFFENDER_ID),
    constraint OFF_TA_OFF_NAME_F2
        foreign key (OFFENDER_ID) references OFFENDERS
);

comment on column OFFENDER_TRUST_ACCOUNTS.CASELOAD_ID is 'The Case Load Identifier';

comment on column OFFENDER_TRUST_ACCOUNTS.OFFENDER_ID is 'The Related Offender Identifier';

comment on column OFFENDER_TRUST_ACCOUNTS.MODIFY_DATE is 'Modify Date';

comment on column OFFENDER_TRUST_ACCOUNTS.MODIFY_USER_ID is 'The user who modifies the record';

comment on column OFFENDER_TRUST_ACCOUNTS.LIST_SEQ is 'The sequence in which the data should be shown';

comment on column OFFENDER_TRUST_ACCOUNTS.CREATE_DATETIME is 'The timestamp when the record is created';

comment on column OFFENDER_TRUST_ACCOUNTS.CREATE_USER_ID is 'The user who creates the record';

comment on column OFFENDER_TRUST_ACCOUNTS.MODIFY_DATETIME is 'The timestamp when the record is modified ';

CREATE UNIQUE INDEX OFFENDER_TRUST_ACCOUNT_PK ON OFFENDER_TRUST_ACCOUNTS (CASELOAD_ID, OFFENDER_ID);

CREATE INDEX OFFENDER_TRUST_ACCOUNTS_NI1 ON OFFENDER_TRUST_ACCOUNTS (OFFENDER_ID)
