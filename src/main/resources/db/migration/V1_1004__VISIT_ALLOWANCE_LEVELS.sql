create table "VISIT_ALLOWANCE_LEVELS"
(
    IEP_LEVEL                     VARCHAR2(12 char)                      not null,
    AGY_LOC_ID                    VARCHAR2(6 char)                       not null,
    VISIT_TYPE                    VARCHAR2(12 char)                      not null,
    REMAND_VISITS                 NUMBER(3),
    WEEKENDS                      NUMBER(3),
    HOURS                         NUMBER(3),
    ACTIVE_FLAG                   VARCHAR2(1 char)                       not null,
    EXPIRY_DATE                   DATE,
    USER_ID                       VARCHAR2(40 char),
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
    constraint VISIT_ALLOWANCE_LEVELS_PK
        primary key (IEP_LEVEL, AGY_LOC_ID, VISIT_TYPE),
    constraint VIS_LEV_IEP_LEV_FK
        foreign key (IEP_LEVEL, AGY_LOC_ID) references IEP_LEVELS(IEP_LEVEL, AGY_LOC_ID)
);


