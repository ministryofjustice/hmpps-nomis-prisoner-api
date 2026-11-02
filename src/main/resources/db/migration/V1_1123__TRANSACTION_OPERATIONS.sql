create table TRANSACTION_OPERATIONS
(
    MODULE_NAME                   VARCHAR2(20 char)                      not null,
    TXN_TYPE                      VARCHAR2(6 char)                       not null,
    TXN_OPERATION_SEQ             NUMBER(6)         default 99           not null,
    DR_ACCOUNT_CODE               NUMBER(6),
    CR_ACCOUNT_CODE               NUMBER(6),
    BANK_DR_ACCOUNT_CODE          NUMBER(6),
    BANK_CR_ACCOUNT_CODE          NUMBER(6),
    INVALID_ACCOUNTS_FLAG         VARCHAR2(1 char)  default 'N',
    CHEQUE_PRODUCTION_FLAG        VARCHAR2(1 char)  default 'Y'          not null,
    RECEIPT_PRODUCTION_FLAG       VARCHAR2(1 char)  default 'Y'          not null,
    CHEQUE_PAYEE_TYPE             VARCHAR2(12 char),
    UPDATE_ALLOWED_FLAG           VARCHAR2(1 char)  default 'Y'          not null,
    TXN_OPERATION_TYPE            VARCHAR2(12 char),
    TXN_ITEM_CODE                 VARCHAR2(12 char),
    MODIFY_DATE                   DATE                                   not null,
    MODIFY_USER_ID                VARCHAR2(32 char),
    LIST_SEQ                      NUMBER(6)         default 99,
    CASELOAD_ID                   VARCHAR2(12 char)                      not null,
    CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
    CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
    MODIFY_DATETIME               TIMESTAMP(9),
    AUDIT_TIMESTAMP               TIMESTAMP(9),
    AUDIT_USER_ID                 VARCHAR2(32 char),
    AUDIT_MODULE_NAME             VARCHAR2(65 char),
    AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
    AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char)
);

comment on table TRANSACTION_OPERATIONS is 'Specifies the transactions associated with each module as well as posting logic.';
comment on column TRANSACTION_OPERATIONS.MODULE_NAME is 'Short name for module.';
comment on column TRANSACTION_OPERATIONS.TXN_TYPE is 'Trust accounting transaction type.';
comment on column TRANSACTION_OPERATIONS.TXN_OPERATION_SEQ is 'Controls the sequencing of multiple posting transactions.';
comment on column TRANSACTION_OPERATIONS.DR_ACCOUNT_CODE is 'Debit account for specified transaction.';
comment on column TRANSACTION_OPERATIONS.CR_ACCOUNT_CODE is 'Credit for specified transaction.';
comment on column TRANSACTION_OPERATIONS.BANK_DR_ACCOUNT_CODE is 'Transaction may involve movement of funds between banks.';
comment on column TRANSACTION_OPERATIONS.BANK_CR_ACCOUNT_CODE is 'Transaction may involve movement of funds between banks.';
comment on column TRANSACTION_OPERATIONS.CHEQUE_PRODUCTION_FLAG is 'Flag signalling check production.';
comment on column TRANSACTION_OPERATIONS.RECEIPT_PRODUCTION_FLAG is 'Flag signalling receipt production for transaction.';
comment on column TRANSACTION_OPERATIONS.CHEQUE_PAYEE_TYPE is 'Reference Code ( PAYEE_TYPE )';
comment on column TRANSACTION_OPERATIONS.UPDATE_ALLOWED_FLAG is 'Update allowed flag.';
comment on column TRANSACTION_OPERATIONS.TXN_ITEM_CODE is 'Reference Code ( TXN_ITEM )';
comment on column TRANSACTION_OPERATIONS.MODIFY_DATE is 'Modify Date';
comment on column TRANSACTION_OPERATIONS.MODIFY_USER_ID is 'The user who modifies the record';
comment on column TRANSACTION_OPERATIONS.LIST_SEQ is 'The sequence in which the data should be shown';
comment on column TRANSACTION_OPERATIONS.CASELOAD_ID is 'The Case Load Identifier';
comment on column TRANSACTION_OPERATIONS.CREATE_DATETIME is 'The timestamp when the record is created';
comment on column TRANSACTION_OPERATIONS.CREATE_USER_ID is 'The user who creates the record';
comment on column TRANSACTION_OPERATIONS.MODIFY_DATETIME is 'The timestamp when the record is modified ';
