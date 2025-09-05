create table LEGAL_UPDATE_REASONS
(
    UPDATE_REASON_CODE            VARCHAR2(12 char)                      not null
        constraint LEGAL_UPDATE_REASONS_PK
            primary key,
    DESCRIPTION                   VARCHAR2(80 char)                      not null,
    EFFECTIVE_DATE                DATE,
    REASON_CATEGORY               VARCHAR2(12 char)                      not null,
    ACTIVE_TYPE                   VARCHAR2(12 char)                      not null,
    LIST_SEQ                      NUMBER(6),
    EXPIRY_DATE                   DATE,
    ACTIVE_FLAG                   VARCHAR2(1 char)  default 'Y'          not null,
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
comment on column LEGAL_UPDATE_REASONS.UPDATE_REASON_CODE is 'The update reason code';
comment on column LEGAL_UPDATE_REASONS.DESCRIPTION is 'The description';
comment on column LEGAL_UPDATE_REASONS.EFFECTIVE_DATE is 'The effective date';
comment on column LEGAL_UPDATE_REASONS.REASON_CATEGORY is 'The category of the reason code.  Reference Codes(LGL_RSN_CAT)';
comment on column LEGAL_UPDATE_REASONS.ACTIVE_TYPE is 'The active type.  Reference Codes(ACTIVE_TYPE)';
comment on column LEGAL_UPDATE_REASONS.LIST_SEQ is 'The list seq';
comment on column LEGAL_UPDATE_REASONS.EXPIRY_DATE is 'The expiry date of the reaons';
comment on column LEGAL_UPDATE_REASONS.ACTIVE_FLAG is '? if the record active';