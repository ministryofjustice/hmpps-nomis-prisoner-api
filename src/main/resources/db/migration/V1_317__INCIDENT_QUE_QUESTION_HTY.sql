CREATE TABLE INCIDENT_QUE_QUESTION_HTY (
  INCIDENT_QUESTIONNAIRE_ID NUMBER(10,0) NOT NULL,
  QUESTION_SEQ NUMBER(6,0) NOT NULL,
  QUESTIONNAIRE_QUE_ID NUMBER(10,0) NOT NULL,
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
  CONSTRAINT INCIDENT_QUE_QUESTION_PK PRIMARY KEY (INCIDENT_QUESTIONNAIRE_ID,QUESTION_SEQ),

  CONSTRAINT INC_QUE_QUE_HTY_QUE_QUE_FK FOREIGN KEY (QUESTIONNAIRE_QUE_ID) REFERENCES QUESTIONNAIRE_QUESTIONS(QUESTIONNAIRE_QUE_ID),
  CONSTRAINT INC_QUE_QUE_INC_QUE_HTY_FK FOREIGN KEY (INCIDENT_QUESTIONNAIRE_ID) REFERENCES INCIDENT_QUESTIONNAIRE_HTY(INCIDENT_QUESTIONNAIRE_ID)
);
CREATE UNIQUE INDEX INCIDENT_CASE_QUESTION_HTY_PK ON INCIDENT_QUE_QUESTION_HTY (INCIDENT_QUESTIONNAIRE_ID,QUESTION_SEQ);
CREATE INDEX INC_QUE_QUE_HTY_QUE_QUE_FK ON INCIDENT_QUE_QUESTION_HTY (QUESTIONNAIRE_QUE_ID);