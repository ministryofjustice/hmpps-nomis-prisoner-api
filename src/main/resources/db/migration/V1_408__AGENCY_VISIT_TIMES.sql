CREATE TABLE AGENCY_VISIT_TIMES
(
    AGY_LOC_ID VARCHAR2(6 CHAR) NOT NULL
    , WEEK_DAY VARCHAR2(3 CHAR) NOT NULL
    , TIME_SLOT_SEQ NUMBER(6, 0) NOT NULL
    , START_TIME DATE NOT NULL
    , END_TIME DATE NOT NULL
    , CREATE_DATETIME TIMESTAMP(9) DEFAULT systimestamp NOT NULL
    , CREATE_USER_ID VARCHAR2(32 CHAR) DEFAULT USER NOT NULL
    , MODIFY_DATETIME TIMESTAMP(9)
    , MODIFY_USER_ID VARCHAR2(32 CHAR)
    , AUDIT_TIMESTAMP TIMESTAMP(9)
    , AUDIT_USER_ID VARCHAR2(32 CHAR)
    , AUDIT_MODULE_NAME VARCHAR2(65 CHAR)
    , AUDIT_CLIENT_USER_ID VARCHAR2(64 CHAR)
    , AUDIT_CLIENT_IP_ADDRESS VARCHAR2(39 CHAR)
    , AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 CHAR)
    , AUDIT_ADDITIONAL_INFO VARCHAR2(256 CHAR)
    , EFFECTIVE_DATE DATE
    , EXPIRY_DATE DATE
    , CONSTRAINT AGENCY_VISIT_TIMES_PK PRIMARY KEY
    (AGY_LOC_ID, WEEK_DAY, TIME_SLOT_SEQ),
    CONSTRAINT AGY_VIS_DT_AGY_VIS_DAY_FK FOREIGN KEY(AGY_LOC_ID, WEEK_DAY) REFERENCES AGENCY_VISIT_DAYS(AGY_LOC_ID, WEEK_DAY)
);
CREATE UNIQUE INDEX AGENCY_VISIT_TIMES_PK ON AGENCY_VISIT_TIMES (AGY_LOC_ID ASC, WEEK_DAY ASC, TIME_SLOT_SEQ ASC);
CREATE UNIQUE INDEX AGENCY_VISIT_TIMES_UK ON AGENCY_VISIT_TIMES (AGY_LOC_ID ASC, WEEK_DAY ASC, START_TIME ASC, END_TIME ASC);


