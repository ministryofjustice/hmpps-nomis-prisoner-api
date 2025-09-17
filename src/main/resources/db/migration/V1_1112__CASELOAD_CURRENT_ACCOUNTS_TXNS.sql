create table CASELOAD_CURRENT_ACCOUNTS_TXNS
(
    CASELOAD_CURRENT_ACCOUNT_ID   NUMBER(10)                             not null,
    CASELOAD_ID                   VARCHAR2(6 char)                       not null,
    ACCOUNT_CODE                  NUMBER(6)                              not null,
    ACCOUNT_PERIOD_ID             NUMBER(10)                             not null,
    CURRENT_BALANCE               NUMBER(13, 2)                          not null,
    MODIFY_USER_ID                VARCHAR2(32 char),
    MODIFY_DATE                   DATE                                   not null,
    LIST_SEQ                      NUMBER(3)         default 99,
    CONSOLIDATION_DATE            DATE,
    TXN_ID                        NUMBER(10),
    TXN_ENTRY_SEQ                 NUMBER(6),
    GL_ENTRY_SEQ                  NUMBER(6),
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
    constraint CASELOAD_CURRENT_ACCT_TXNS_PK
        primary key (CASELOAD_CURRENT_ACCOUNT_ID),
    constraint CSLD_CAT_AC_CODE_F1
        foreign key (ACCOUNT_CODE) references ACCOUNT_CODES,
    constraint CSLD_CAT_AC_PRD_F2
        foreign key (ACCOUNT_PERIOD_ID) references ACCOUNT_PERIODS
);

comment on table CASELOAD_CURRENT_ACCOUNTS_TXNS is 'Maintains the detail of each general ledger account.';

comment on column CASELOAD_CURRENT_ACCOUNTS_TXNS.CASELOAD_ID is ' Unique identifier for a caseload.';

comment on column CASELOAD_CURRENT_ACCOUNTS_TXNS.ACCOUNT_CODE is 'General ledger account code.';

comment on column CASELOAD_CURRENT_ACCOUNTS_TXNS.ACCOUNT_PERIOD_ID is 'Pointing to the current accounting period.';

comment on column CASELOAD_CURRENT_ACCOUNTS_TXNS.CURRENT_BALANCE is 'The current balance in the general ledger account.';

comment on column CASELOAD_CURRENT_ACCOUNTS_TXNS.MODIFY_USER_ID is 'The user who modifies the record';

comment on column CASELOAD_CURRENT_ACCOUNTS_TXNS.MODIFY_DATE is 'Modify Date';

comment on column CASELOAD_CURRENT_ACCOUNTS_TXNS.LIST_SEQ is 'The sequence in which the data should be shown';

comment on column CASELOAD_CURRENT_ACCOUNTS_TXNS.TXN_ID is 'Transaction Identifier';

comment on column CASELOAD_CURRENT_ACCOUNTS_TXNS.CREATE_DATETIME is 'The timestamp when the record is created';

comment on column CASELOAD_CURRENT_ACCOUNTS_TXNS.CREATE_USER_ID is 'The user who creates the record';

comment on column CASELOAD_CURRENT_ACCOUNTS_TXNS.MODIFY_DATETIME is 'The timestamp when the record is modified ';

CREATE UNIQUE INDEX CASELOAD_CURRENT_ACCT_TXNS_PK ON CASELOAD_CURRENT_ACCOUNTS_TXNS (CASELOAD_CURRENT_ACCOUNT_ID);
CREATE INDEX CSLD_CAT_AC_PRD_F2 ON CASELOAD_CURRENT_ACCOUNTS_TXNS (ACCOUNT_PERIOD_ID);
CREATE INDEX CASELOAD_CURRENT_ACTS_TXNS_NI1 ON CASELOAD_CURRENT_ACCOUNTS_TXNS (CASELOAD_ID, ACCOUNT_CODE, ACCOUNT_PERIOD_ID);
CREATE INDEX CSLD_CAT_AC_CODE_F1 ON CASELOAD_CURRENT_ACCOUNTS_TXNS (ACCOUNT_CODE);

create sequence CASELOAD_CURRENT_ACCOUNT_ID start with 1;
