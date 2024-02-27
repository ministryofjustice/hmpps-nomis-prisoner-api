CREATE TABLE INTERNAL_LOCATION_USAGES
(   INTERNAL_LOCATION_USAGE_ID    NUMBER(10)                             NOT NULL CONSTRAINT INTERNAL_LOCATION_USAGES_PK PRIMARY KEY,
    AGY_LOC_ID                    VARCHAR2(6 CHAR)                       NOT NULL,
    INTERNAL_LOCATION_USAGE       VARCHAR2(12 CHAR)                      NOT NULL,
    EVENT_SUB_TYPE                VARCHAR2(12 CHAR),
    CREATE_DATETIME               TIMESTAMP(9)      DEFAULT systimestamp NOT NULL,
    CREATE_USER_ID                VARCHAR2(32 CHAR) DEFAULT USER         NOT NULL,
    MODIFY_DATETIME               TIMESTAMP(9),
    MODIFY_USER_ID                VARCHAR2(32 CHAR),
    AUDIT_TIMESTAMP               TIMESTAMP(9),
    AUDIT_USER_ID                 VARCHAR2(32 CHAR),
    AUDIT_MODULE_NAME             VARCHAR2(65 CHAR),
    AUDIT_CLIENT_USER_ID          VARCHAR2(64 CHAR),
    AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 CHAR),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 CHAR),
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 CHAR)
);

CREATE UNIQUE INDEX INTERNAL_LOCATION_USAGES_UI1 ON INTERNAL_LOCATION_USAGES (AGY_LOC_ID, INTERNAL_LOCATION_USAGE, EVENT_SUB_TYPE);
ALTER TABLE INTERNAL_LOCATION_USAGES ADD CONSTRAINT INTERNAL_LOCATION_USAGES_UK UNIQUE (AGY_LOC_ID, INTERNAL_LOCATION_USAGE, EVENT_SUB_TYPE);
