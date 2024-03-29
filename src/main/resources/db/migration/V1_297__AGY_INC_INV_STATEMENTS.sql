CREATE TABLE AGY_INC_INV_STATEMENTS (
                                        AGY_II_STATEMENT_ID NUMBER(10,0) NOT NULL,
                                        AGY_INC_INVESTIGATION_ID NUMBER(10,0) NOT NULL,
                                        STATEMENT_TYPE VARCHAR2(12) NOT NULL,
                                        NAME_OF_STATEMENT_TAKER VARCHAR2(60),
                                        DATE_OF_STATEMENT_TAKEN DATE NOT NULL,
                                        STATEMENT_DETAIL VARCHAR2(4000) NOT NULL,
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
                                        CONSTRAINT AGY_INC_INV_STATEMENTS_PK PRIMARY KEY (AGY_II_STATEMENT_ID),
                                        CONSTRAINT AGY_II_STMT_AGY_II_FK FOREIGN KEY (AGY_INC_INVESTIGATION_ID) REFERENCES AGY_INC_INVESTIGATIONS(AGY_INC_INVESTIGATION_ID)
);
CREATE INDEX AGY_INC_INV_STATEMENTS_NI1 ON AGY_INC_INV_STATEMENTS (AGY_INC_INVESTIGATION_ID);
CREATE UNIQUE INDEX AGY_INC_INV_STATEMENTS_PK ON AGY_INC_INV_STATEMENTS (AGY_II_STATEMENT_ID);
