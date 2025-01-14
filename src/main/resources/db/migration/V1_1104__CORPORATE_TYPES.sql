create table CORPORATE_TYPES
(
    CORPORATE_ID                  NUMBER(10)                             not null
        constraint CORPORATE_TYPES_CORPORATES_FK
            references CORPORATES,
    CORPORATE_TYPE                VARCHAR2(12 char)                      not null,
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
    constraint CORPORATE_TYPES_PK
        primary key (CORPORATE_ID, CORPORATE_TYPE)
);

comment on table CORPORATE_TYPES is 'The table stores the multiple classification of a corporte.  ';

comment on column CORPORATE_TYPES.CORPORATE_ID is 'FK to Corporates';

comment on column CORPORATE_TYPES.CORPORATE_TYPE is 'Reference Code(CORP_TYPE)';

comment on column CORPORATE_TYPES.CREATE_DATETIME is 'The timestamp when the record is created';

comment on column CORPORATE_TYPES.CREATE_USER_ID is 'The user who creates the record';

comment on column CORPORATE_TYPES.MODIFY_DATETIME is 'The timestamp when the record is modified ';

comment on column CORPORATE_TYPES.MODIFY_USER_ID is 'The user who modifies the record';

