create table AGY_INT_LOC_AMENDMENTS
(
    AGY_INT_LOC_AMENDMENT_ID      NUMBER(10)                             NOT NULL PRIMARY KEY,
    INTERNAL_LOCATION_ID          NUMBER(10)                             NOT NULL
        constraint AGY_IL_AMENDS_AGY_INT_LOC_FK references AGENCY_INTERNAL_LOCATIONS,
    AMEND_DATE                    DATE                                   NOT NULL,
    COLUMN_NAME                   VARCHAR2(30 CHAR),
    OLD_VALUE                     VARCHAR2(240 CHAR),
    NEW_VALUE                     VARCHAR2(240 CHAR),
    DEACTIVATE_REASON_CODE        VARCHAR2(12 CHAR),
    INT_LOC_PROFILE_CODE          VARCHAR2(12 CHAR),
    ACTION_CODE                   VARCHAR2(12 CHAR),
    AMEND_USER_ID                 VARCHAR2(30 CHAR)                      NOT NULL,

    CREATE_DATETIME               TIMESTAMP(9)      default systimestamp NOT NULL,
    CREATE_USER_ID                VARCHAR2(32 char) default USER         NOT NULL,
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
