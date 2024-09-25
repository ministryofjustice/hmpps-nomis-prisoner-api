create table CORPORATES
(
    CORPORATE_ID                  NUMBER(10)                             not null
        constraint CORPORATES_PK
            primary key,
    CORPORATE_NAME                VARCHAR2(40 char),
    CASELOAD_ID                   VARCHAR2(6 char),
    CONTACT_PERSON_NAME           VARCHAR2(40 char),
    CREATED_DATE                  DATE                                   not null,
    UPDATED_DATE                  DATE,
    USER_ID                       VARCHAR2(32 char),
    COMMENT_TEXT                  VARCHAR2(240 char),
    START_DATE                    DATE,
    ACCOUNT_TERM_CODE             VARCHAR2(60 char),
    SHIPPING_TERM_CODE            VARCHAR2(60 char),
    MINIMUM_PURCHASE_AMOUNT       NUMBER(9, 2),
    MAXIMUM_PURCHASE_AMOUNT       NUMBER(9, 2),
    MEMO_TEXT                     VARCHAR2(40 char),
    SUSPENDED_FLAG                VARCHAR2(1 char)                       not null,
    SUSPENDED_DATE                DATE,
    FEI_NUMBER                    VARCHAR2(40 char),
    ACTIVE_FLAG                   VARCHAR2(1 char)  default 'Y'          not null,
    EXPIRY_DATE                   DATE,
    TAX_NO                        VARCHAR2(12 char),
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

comment on table CORPORATES is 'An organisation not managed or owned by the National Offender Management Service - typically, these will be Program Providers. NOTE: There are two clear exceptional cases to the aforementioned definition providing workarounds for TAG system functional gaps: - 1. Corporate instances representing Administrative Caseloads can be created and subsequently assigned as Corporate Beneficiaries to enable repayments of Prisoner Advances (Prisoner Finances - Deductions); 2. Corporate instances representing Institutional Receptions can be created and subsequently utilised as a target for trust account funds transfers (e.g. when an offender is relocated).';

comment on column CORPORATES.CORPORATE_ID is ' - Column already exists';

comment on column CORPORATES.CORPORATE_NAME is ' - Column already exists';

comment on column CORPORATES.CASELOAD_ID is ' - Column already exists';

comment on column CORPORATES.CONTACT_PERSON_NAME is ' - Column already exists';

comment on column CORPORATES.CREATED_DATE is ' - Column already exists';

comment on column CORPORATES.UPDATED_DATE is ' - Column already exists';

comment on column CORPORATES.USER_ID is ' - Column already exists';

comment on column CORPORATES.COMMENT_TEXT is ' - Column already exists';

comment on column CORPORATES.ACTIVE_FLAG is 'Active data indicator';

comment on column CORPORATES.EXPIRY_DATE is 'Expiry date for the data';

comment on column CORPORATES.CREATE_DATETIME is 'The timestamp when the record is created';

comment on column CORPORATES.CREATE_USER_ID is 'The user who creates the record';

comment on column CORPORATES.MODIFY_DATETIME is 'The timestamp when the record is modified ';

comment on column CORPORATES.MODIFY_USER_ID is 'The user who modifies the record';

