create table CASELOAD_PAYMENT_PROFILES
(
    CASELOAD_PAYMENT_PROFILE_ID   NUMBER(10)                             not null
        constraint CASELOAD_PAYMENT_PROFILES_PK
        primary key,
    CASELOAD_ID                   VARCHAR2(6 char)                       not null
        constraint CSL_PAY_PRFL_CSL_FK
        references CASELOADS,
    TXN_TYPE                      VARCHAR2(12 char)                      not null,
    PAYMENT_MODE                  VARCHAR2(12 char)                      not null,
    ADVANCE_AMOUNT                NUMBER(11, 2),
    PAYMENT_AMOUNT                NUMBER(11, 2),
    WEEK_DAY                      VARCHAR2(12 char)                      not null,
    ACTIVE_FLAG                   VARCHAR2(1 char)  default 'Y'          not null,
    EXPIRY_DATE                   DATE,
    RECORD_USER_ID                VARCHAR2(30 char) default USER         not null,
    CREATE_DATETIME               TIMESTAMP(9) default systimestamp not null,
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
comment on table CASELOAD_PAYMENT_PROFILES is 'The caseload payment schedules';
comment on column CASELOAD_PAYMENT_PROFILES.CASELOAD_ID is 'The ID of the caseload';
comment on column CASELOAD_PAYMENT_PROFILES.TXN_TYPE is 'The transaction type';
comment on column CASELOAD_PAYMENT_PROFILES.PAYMENT_MODE is '(Advance|Schedule)';
comment on column CASELOAD_PAYMENT_PROFILES.ADVANCE_AMOUNT is 'The amount of advance payment';
comment on column CASELOAD_PAYMENT_PROFILES.PAYMENT_AMOUNT is 'The amount of payment';
comment on column CASELOAD_PAYMENT_PROFILES.WEEK_DAY is 'The Week Day of the payment.  Reference Code(Week_DAY)';
comment on column CASELOAD_PAYMENT_PROFILES.ACTIVE_FLAG is '?If the payment active';
comment on column CASELOAD_PAYMENT_PROFILES.EXPIRY_DATE is 'Expiry date of the payment schedules';
comment on column CASELOAD_PAYMENT_PROFILES.RECORD_USER_ID is 'User who inputs the records';
comment on column CASELOAD_PAYMENT_PROFILES.CREATE_DATETIME is 'The timestamp when the record is created';
comment on column CASELOAD_PAYMENT_PROFILES.CREATE_USER_ID is 'The user who creates the record';
comment on column CASELOAD_PAYMENT_PROFILES.MODIFY_DATETIME is 'The timestamp when the record is modified ';
comment on column CASELOAD_PAYMENT_PROFILES.MODIFY_USER_ID is 'The user who modifies the record';
