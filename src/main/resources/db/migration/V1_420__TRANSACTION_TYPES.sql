create table TRANSACTION_TYPES
(
    TXN_TYPE                      VARCHAR2(6 char)                       not null
        constraint TRANSACTION_TYPES_PK
            primary key,
    DESCRIPTION                   VARCHAR2(40 char)                      not null,
    ACTIVE_FLAG                   VARCHAR2(1 char)  default 'Y'          not null,
    TXN_USAGE                     VARCHAR2(12 char)                      not null,
    ALL_CASELOAD_FLAG             VARCHAR2(1 char)  default 'Y'          not null,
    EXPIRY_DATE                   DATE,
    UPDATE_ALLOWED_FLAG           VARCHAR2(1 char)  default 'Y'          not null,
    MANUAL_INVOICE_FLAG           VARCHAR2(1 char)  default 'Y'          not null,
    CREDIT_OBLIGATION_TYPE        VARCHAR2(6 char),
    MODIFY_USER_ID                VARCHAR2(32 char),
    MODIFY_DATE                   DATE                                   not null,
    LIST_SEQ                      NUMBER(6)         default 99,
    GROSS_NET_FLAG                VARCHAR2(1 char)  default 'N',
    CASELOAD_TYPE                 VARCHAR2(12 char),
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
