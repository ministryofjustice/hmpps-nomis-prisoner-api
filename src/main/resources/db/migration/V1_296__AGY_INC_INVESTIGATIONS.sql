CREATE TABLE AGY_INC_INVESTIGATIONS (
                                        AGY_INC_INVESTIGATION_ID NUMBER(10,0) NOT NULL,
                                        AGENCY_INCIDENT_ID NUMBER(10,0) NOT NULL,
                                        INVESTIGATOR_ID NUMBER(7,0) NOT NULL,
                                        ASSIGNED_DATE DATE NOT NULL,
                                        COMMENT_TEXT VARCHAR2(240),
                                        CREATE_DATETIME TIMESTAMP DEFAULT systimestamp ,
                                        CREATE_USER_ID VARCHAR2(32) DEFAULT USER ,
                                        MODIFY_DATETIME TIMESTAMP,
                                        MODIFY_USER_ID VARCHAR2(32),
                                        AUDIT_TIMESTAMP TIMESTAMP,
                                        AUDIT_USER_ID VARCHAR2(32),
                                        AUDIT_MODULE_NAME VARCHAR2(65),
                                        AUDIT_CLIENT_USER_ID VARCHAR2(64),
                                        AUDIT_CLIENT_IP_ADDRESS VARCHAR2(39),
                                        AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64),
                                        AUDIT_ADDITIONAL_INFO VARCHAR2(256),
                                        PARTY_SEQ NUMBER(6,0) NOT NULL,
                                        CONSTRAINT AGY_INC_INVESTIGATIONS_PK PRIMARY KEY (AGY_INC_INVESTIGATION_ID),
                                        CONSTRAINT AGY_INC_INV_STF_FK FOREIGN KEY (INVESTIGATOR_ID) REFERENCES STAFF_MEMBERS(STAFF_ID),
                                        CONSTRAINT AGY_INV_AGY_INC_PTY_FK FOREIGN KEY (AGENCY_INCIDENT_ID,PARTY_SEQ) REFERENCES AGENCY_INCIDENT_PARTIES(AGENCY_INCIDENT_ID,PARTY_SEQ)
);
CREATE UNIQUE INDEX AGY_INC_INVESTIGATIONS_PK ON AGY_INC_INVESTIGATIONS (AGY_INC_INVESTIGATION_ID);
CREATE INDEX AGY_INC_INV_STF_FK ON AGY_INC_INVESTIGATIONS (INVESTIGATOR_ID);
CREATE INDEX AGY_INV_INVESTIGATIONS_NI1 ON AGY_INC_INVESTIGATIONS (AGENCY_INCIDENT_ID,PARTY_SEQ);
