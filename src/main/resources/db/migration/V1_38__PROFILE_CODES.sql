CREATE TABLE PROFILE_CODES (
    PROFILE_TYPE VARCHAR2(12) NOT NULL,
    PROFILE_CODE VARCHAR2(12) NOT NULL,
    DESCRIPTION VARCHAR2(40),
    LIST_SEQ NUMBER(6,0) NOT NULL,
    UPDATE_ALLOWED_FLAG VARCHAR2(1) DEFAULT 'Y' NOT NULL,
    ACTIVE_FLAG VARCHAR2(1) DEFAULT 'Y' NOT NULL,
    EXPIRY_DATE DATE,
    USER_ID VARCHAR2(32),
    MODIFIED_DATE DATE NOT NULL,
    CREATE_DATETIME TIMESTAMP DEFAULT systimestamp NOT NULL,
    CREATE_USER_ID VARCHAR2(32) DEFAULT USER NOT NULL,
    MODIFY_DATETIME TIMESTAMP,
    MODIFY_USER_ID VARCHAR2(32),
    AUDIT_TIMESTAMP TIMESTAMP,
    AUDIT_USER_ID VARCHAR2(32),
    AUDIT_MODULE_NAME VARCHAR2(65),
    AUDIT_CLIENT_USER_ID VARCHAR2(64),
    AUDIT_CLIENT_IP_ADDRESS VARCHAR2(39),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64),
    AUDIT_ADDITIONAL_INFO VARCHAR2(256),
    CONSTRAINT PROFILE_CODES_PK PRIMARY KEY (PROFILE_TYPE,PROFILE_CODE)
);