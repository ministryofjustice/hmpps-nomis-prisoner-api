create table PERSON_EMPLOYMENTS
(
    PERSON_ID                     NUMBER(10)                             not null
        constraint PERSON_EMPLOYMENTS_PERSONS_FK
            references PERSONS,
    EMPLOYMENT_SEQ                NUMBER(6)         default 1            not null,
    OCCUPATION_CODE               VARCHAR2(12 char),
    EMPLOYER_NAME                 VARCHAR2(60 char),
    HOURS_WEEK                    NUMBER(6),
    SCHEDULE_TYPE                 VARCHAR2(12 char),
    WAGE                          VARCHAR2(10 char),
    WAGE_PERIOD_CODE              VARCHAR2(12 char),
    SUPERVISOR_NAME               VARCHAR2(40 char),
    ADDRESS_1                     VARCHAR2(40 char),
    ADDRESS_2                     VARCHAR2(40 char),
    CITY                          VARCHAR2(12 char),
    PROV_STATE_CODE               VARCHAR2(12 char),
    CONTACT_NUMBER                VARCHAR2(32 char),
    CONTACT_TYPE                  VARCHAR2(12 char),
    MODIFY_USER_ID                VARCHAR2(32 char),
    MODIFY_DATETIME               TIMESTAMP(9),
    EMPLOYMENT_DATE               DATE,
    COMMENT_TEXT                  VARCHAR2(240 char),
    TERMINATION_DATE              DATE,
    PHONE_AREA                    VARCHAR2(4 char),
    PHONE_EXT                     VARCHAR2(6 char),
    CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
    CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
    ACTIVE_FLAG                   VARCHAR2(1 char)  default 'N',
    EMPLOYER_CORP_ID              NUMBER(10),
    AUDIT_TIMESTAMP               TIMESTAMP(9),
    AUDIT_USER_ID                 VARCHAR2(32 char),
    AUDIT_MODULE_NAME             VARCHAR2(65 char),
    AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
    AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
    constraint PERSON_EMPLOYMENTS_PK
        primary key (PERSON_ID, EMPLOYMENT_SEQ)
);

comment on table PERSON_EMPLOYMENTS is 'A record of a persons employment. These details can be used as an authorisation check e.g. of visitor, or a lawyer. NOTE: Only held for persons that have a relationship with the offender that is classed as official (reference Contact Person Types) e.g. Solicitor. Address details are derived from the primary Corporate Address and business telephone number from Corporate Phone (with type of BUS).';
comment on column PERSON_EMPLOYMENTS.MODIFY_USER_ID is 'The user who modifies the record';
comment on column PERSON_EMPLOYMENTS.MODIFY_DATETIME is 'The timestamp when the record is modified ';
comment on column PERSON_EMPLOYMENTS.COMMENT_TEXT is 'Free text comment data';
comment on column PERSON_EMPLOYMENTS.PHONE_AREA is 'Phone Area Code for standard format phone number.';
comment on column PERSON_EMPLOYMENTS.PHONE_EXT is 'Phone Extension for standard format phone number.';
comment on column PERSON_EMPLOYMENTS.CREATE_DATETIME is 'The timestamp when the record is created';
comment on column PERSON_EMPLOYMENTS.CREATE_USER_ID is 'The user who creates the record';
comment on column PERSON_EMPLOYMENTS.ACTIVE_FLAG is 'If person employment is a active one ?';
comment on column PERSON_EMPLOYMENTS.EMPLOYER_CORP_ID is 'FK to corporates table';

