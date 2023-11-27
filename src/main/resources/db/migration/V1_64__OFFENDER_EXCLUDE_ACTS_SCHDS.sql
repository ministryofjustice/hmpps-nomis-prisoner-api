CREATE TABLE OFFENDER_EXCLUDE_ACTS_SCHDS (
   OFFENDER_EXCLUDE_ACT_SCHD_ID     NUMBER(10,0) NOT NULL,
   OFFENDER_BOOK_ID                 NUMBER(10,0) NOT NULL,
   OFF_PRGREF_ID                    NUMBER(10,0) NOT NULL,
   SLOT_CATEGORY_CODE               VARCHAR2(12) NULL,
   EXCLUDE_DAY                      VARCHAR2(12) NOT NULL,
   CRS_ACTY_ID                      NUMBER(10,0) NOT NULL,
   CREATE_DATETIME                  TIMESTAMP DEFAULT systimestamp  NOT NULL,
   CREATE_USER_ID                   VARCHAR2(32) DEFAULT USER  NOT NULL,
   MODIFY_DATETIME                  TIMESTAMP NULL,
   MODIFY_USER_ID                   VARCHAR2(32) NULL,
   AUDIT_TIMESTAMP                  TIMESTAMP NULL,
   AUDIT_USER_ID                    VARCHAR2(32) NULL,
   AUDIT_MODULE_NAME                VARCHAR2(65) NULL,
   AUDIT_CLIENT_USER_ID             VARCHAR2(64) NULL,
   AUDIT_CLIENT_IP_ADDRESS          VARCHAR2(39) NULL,
   AUDIT_CLIENT_WORKSTATION_NAME    VARCHAR2(64) NULL,
   AUDIT_ADDITIONAL_INFO            VARCHAR2(256) NULL,

   CONSTRAINT OFF_EXCLUDE_ACTS_SCHDS_FK1 FOREIGN KEY (OFFENDER_BOOK_ID) REFERENCES OFFENDER_BOOKINGS(OFFENDER_BOOK_ID),
   CONSTRAINT OFF_EXCLUDE_ACTS_SCHDS_FK3 FOREIGN KEY (CRS_ACTY_ID) REFERENCES COURSE_ACTIVITIES(CRS_ACTY_ID),
   CONSTRAINT OFF_EXCLUDE_ACTS_SCHDS_FK4 FOREIGN KEY (OFF_PRGREF_ID) REFERENCES OFFENDER_PROGRAM_PROFILES(OFF_PRGREF_ID)
);

CREATE UNIQUE INDEX OFFENDER_EXCLUDE_ACT_SCHDS_PK ON OFFENDER_EXCLUDE_ACTS_SCHDS (OFFENDER_EXCLUDE_ACT_SCHD_ID);
CREATE UNIQUE INDEX OFF_EXCLUDE_ACTS_SCHDS_UK1 ON OFFENDER_EXCLUDE_ACTS_SCHDS (OFF_PRGREF_ID,EXCLUDE_DAY,SLOT_CATEGORY_CODE);

CREATE INDEX OFF_EXCLUDE_ACT_SCHDS_FK1 ON OFFENDER_EXCLUDE_ACTS_SCHDS (OFFENDER_BOOK_ID);
CREATE INDEX OFF_EXCLUDE_ACT_SCHDS_FK3 ON OFFENDER_EXCLUDE_ACTS_SCHDS (CRS_ACTY_ID);
CREATE INDEX OFF_EXCLUDE_ACT_SCHDS_FK4 ON OFFENDER_EXCLUDE_ACTS_SCHDS (OFF_PRGREF_ID);