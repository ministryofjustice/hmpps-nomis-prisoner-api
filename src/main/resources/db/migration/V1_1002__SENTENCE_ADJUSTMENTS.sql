create table SENTENCE_ADJUSTMENTS
(
    SENTENCE_ADJUST_CODE          VARCHAR2(12 char)                      not null
        constraint SA_PK
            primary key,
    DESCRIPTION                   VARCHAR2(80 char)                      not null,
    DEBIT_CREDIT_CODE             VARCHAR2(12 char),
    USAGE_CODE                    VARCHAR2(12 char),
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

comment on table SENTENCE_ADJUSTMENTS is 'This Table stores the details of the Sentence Adjustment Types';

comment on column SENTENCE_ADJUSTMENTS.SENTENCE_ADJUST_CODE is 'Adjustment Code';

comment on column SENTENCE_ADJUSTMENTS.DESCRIPTION is 'Description of the Adjustment code';

comment on column SENTENCE_ADJUSTMENTS.DEBIT_CREDIT_CODE is 'Either CR or DR';

comment on column SENTENCE_ADJUSTMENTS.USAGE_CODE is 'The source code for the adjustment Refence Domain : [ADJ_SRC]';

comment on column SENTENCE_ADJUSTMENTS.CREATE_DATETIME is 'The timestamp when the record is created';

comment on column SENTENCE_ADJUSTMENTS.CREATE_USER_ID is 'The user who creates the record';

comment on column SENTENCE_ADJUSTMENTS.MODIFY_DATETIME is 'The timestamp when the record is modified ';

comment on column SENTENCE_ADJUSTMENTS.MODIFY_USER_ID is 'The user who modifies the record';
