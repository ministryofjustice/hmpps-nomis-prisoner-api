create table IMPRISON_STATUS_MAPPINGS
(
    IMPRISON_STATUS_MAPPING_ID    NUMBER(10)                             not null
        constraint IMPRISON_STATUS_MAPPINGS_PK
            primary key,
    IMPRISONMENT_STATUS_ID        NUMBER(10)                             not null
        constraint IMPRISON_STATUS_MAPPINGS_FK1
            references IMPRISONMENT_STATUSES,
    SENTENCE_CATEGORY             VARCHAR2(12 char),
    SENTENCE_CALC_TYPE            VARCHAR2(12 char),
    OFFENCE_RESULT_CODE           VARCHAR2(12 char)
        constraint IMPRISON_STATUS_MAPPINGS_FK3
            references OFFENCE_RESULT_CODES,
    ACTIVE_FLAG                   VARCHAR2(1 char)  default 'Y'          not null,
    EXPIRY_DATE                   DATE,
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
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
    constraint IMPRISON_STATUS_MAPPINGS_UK1
        unique (SENTENCE_CATEGORY, SENTENCE_CALC_TYPE, OFFENCE_RESULT_CODE, ACTIVE_FLAG, EXPIRY_DATE),
    constraint IMPRISON_STATUS_MAPPINGS_FK2
        foreign key (SENTENCE_CATEGORY, SENTENCE_CALC_TYPE) references SENTENCE_CALC_TYPES,
    constraint IMPRISON_STATUS_MAPPINGS_CK1
        check ((active_flag = 'Y' AND expiry_date IS NULL)
            OR active_flag = 'N' and expiry_date IS NOT NULL),
    constraint IMPRISON_STATUS_MAPPINGS_CK2
        check ((sentence_category IS NOT NULL AND sentence_calc_type IS NOT NULL) OR offence_result_code IS NOT NULL)
)
;

comment on table IMPRISON_STATUS_MAPPINGS is 'Maps the imprisonment statuses with sentence calc types;offence result codes'
;

comment on column IMPRISON_STATUS_MAPPINGS.IMPRISON_STATUS_MAPPING_ID is 'Primary key generated from sequence imprison_status_mapping_id '
;

comment on column IMPRISON_STATUS_MAPPINGS.IMPRISONMENT_STATUS_ID is 'Foreign key to imprisonment_statuses table'
;

comment on column IMPRISON_STATUS_MAPPINGS.SENTENCE_CATEGORY is 'First part of Foreign key to sentence_calc_types table'
;

comment on column IMPRISON_STATUS_MAPPINGS.SENTENCE_CALC_TYPE is 'Second part of Foreign key to sentence_calc_types table'
;

comment on column IMPRISON_STATUS_MAPPINGS.OFFENCE_RESULT_CODE is 'Foreign key to offence_result_codes table'
;

comment on column IMPRISON_STATUS_MAPPINGS.ACTIVE_FLAG is 'Indicates whether the row is active or not (Y;N)'
;

comment on column IMPRISON_STATUS_MAPPINGS.EXPIRY_DATE is 'Date the row was made inactive'
;

