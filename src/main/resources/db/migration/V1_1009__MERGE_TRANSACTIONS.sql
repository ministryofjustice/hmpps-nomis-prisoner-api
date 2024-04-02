create table MERGE_TRANSACTIONS
(
    MERGE_TRANSACTION_ID          NUMBER(10)                             not null
        constraint MERGE_TRANSACTIONS_PK
            primary key,
    REQUEST_DATE                  DATE              default SYSDATE      not null,
    REQUEST_STATUS_CODE           VARCHAR2(12 char) default 'PENDING'    not null,
    QUEUE_MESSAGE_ID              VARCHAR2(64 char),
    TRANSACTION_SOURCE            VARCHAR2(12 char)                      not null,
    OFFENDER_BOOK_ID_1            NUMBER(10),
    ROOT_OFFENDER_ID_1            NUMBER(10),
    OFFENDER_ID_1                 NUMBER(10),
    OFFENDER_ID_DISPLAY_1         VARCHAR2(10 char),
    LAST_NAME_1                   VARCHAR2(35 char),
    FIRST_NAME_1                  VARCHAR2(35 char),
    OFFENDER_BOOK_ID_2            NUMBER(10),
    ROOT_OFFENDER_ID_2            NUMBER(10),
    OFFENDER_ID_2                 NUMBER(10),
    OFFENDER_ID_DISPLAY_2         VARCHAR2(10 char),
    LAST_NAME_2                   VARCHAR2(35 char),
    FIRST_NAME_2                  VARCHAR2(35 char),
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
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char)
);
