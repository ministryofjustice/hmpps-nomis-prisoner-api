create table CASELOAD_CURRENT_ACCOUNTS_BASE
(
    CASELOAD_ID                   VARCHAR2(6 char)                       not null,
    ACCOUNT_CODE                  NUMBER(6)                              not null,
    ACCOUNT_PERIOD_ID             NUMBER(10)                             not null,
    CURRENT_BALANCE               NUMBER(13, 2)                          not null,
    BANK_ACCOUNT_TYPE             VARCHAR2(12 char),
    BANK_ACCOUNT_NUMBER           VARCHAR2(25 char),
    ACCOUNT_PARTY_TYPE            VARCHAR2(12 char),
    PAYEE_PERSON_ID               NUMBER(10),
    PAYEE_CORPORATE_ID            NUMBER(10),
    MODIFY_USER_ID                VARCHAR2(32 char),
    MODIFY_DATE                   DATE                                   not null,
    ROUTING_NUMBER                NUMBER(9),
    LIST_SEQ                      NUMBER(6),
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
    constraint CASELOAD_CURRENT_ACCTS_BASE_PK
        primary key (CASELOAD_ID, ACCOUNT_CODE),
    constraint CSLD_CAB_AC_CODE_F1
        foreign key (ACCOUNT_CODE) references ACCOUNT_CODES,
    constraint CSLD_CAB_AC_PRD_F1
        foreign key (ACCOUNT_PERIOD_ID) references ACCOUNT_PERIODS,
    constraint CSLD_CAB_CORP_F3
        foreign key (PAYEE_CORPORATE_ID) references CORPORATES
);

comment on table CASELOAD_CURRENT_ACCOUNTS_BASE is 'Maintains the detail of each general ledger account.';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.CASELOAD_ID is ' Unique identifier for a caseload.';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.ACCOUNT_CODE is 'General ledger account code.';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.BANK_ACCOUNT_TYPE is 'Reference Code (BANK_AC_TYPE )';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.BANK_ACCOUNT_NUMBER is 'The number assigned to the account by the bank.';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.ACCOUNT_PARTY_TYPE is 'Reference Code ( PARTY_TYPE )';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.PAYEE_PERSON_ID is 'System generated identifier for a person.';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.PAYEE_CORPORATE_ID is 'System generated identifier for a corporation.';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.MODIFY_USER_ID is 'The user who modifies the record';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.MODIFY_DATE is 'Modify Date';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.ROUTING_NUMBER is 'routing number is the bank banch identify.';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.LIST_SEQ is 'The sequence in which the data should be shown';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.CREATE_DATETIME is 'The timestamp when the record is created';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.CREATE_USER_ID is 'The user who creates the record';

comment on column CASELOAD_CURRENT_ACCOUNTS_BASE.MODIFY_DATETIME is 'The timestamp when the record is modified ';

CREATE INDEX CSLD_CAB_AC_PRD_F1 ON CASELOAD_CURRENT_ACCOUNTS_BASE (ACCOUNT_PERIOD_ID);
CREATE INDEX CSLD_CAB_AC_CODE_F1 ON CASELOAD_CURRENT_ACCOUNTS_BASE (ACCOUNT_CODE);
CREATE INDEX CASELOAD_CURRENT_ACTS_BASE_NI1 ON CASELOAD_CURRENT_ACCOUNTS_BASE (CASELOAD_ID, ACCOUNT_CODE, ACCOUNT_PERIOD_ID);
CREATE INDEX CSLD_CAB_CORP_F3 ON CASELOAD_CURRENT_ACCOUNTS_BASE (PAYEE_CORPORATE_ID);
