
  CREATE TABLE "OFFENDER_RESTRICTIONS"
   (    "OFFENDER_BOOK_ID" NUMBER(10,0) NOT NULL,
    "OFFENDER_RESTRICTION_ID" NUMBER(10,0) NOT NULL,
    "RESTRICTION_TYPE" VARCHAR2(12 CHAR) NOT NULL,
    "EFFECTIVE_DATE" DATE NOT NULL,
    "EXPIRY_DATE" DATE,
    "COMMENT_TEXT" VARCHAR2(240 CHAR),
    "AUTHORISED_STAFF_ID" NUMBER(10,0),
    "ENTERED_STAFF_ID" NUMBER(10,0) NOT NULL,
    "CREATE_DATETIME" TIMESTAMP (9) DEFAULT systimestamp NOT NULL,
    "CREATE_USER_ID" VARCHAR2(32 CHAR) DEFAULT USER NOT NULL,
    "MODIFY_DATETIME" TIMESTAMP (9),
    "MODIFY_USER_ID" VARCHAR2(32 CHAR),
    "AUDIT_TIMESTAMP" TIMESTAMP (9),
    "AUDIT_USER_ID" VARCHAR2(32 CHAR),
    "AUDIT_MODULE_NAME" VARCHAR2(65 CHAR),
    "AUDIT_CLIENT_USER_ID" VARCHAR2(64 CHAR),
    "AUDIT_CLIENT_IP_ADDRESS" VARCHAR2(39 CHAR),
    "AUDIT_CLIENT_WORKSTATION_NAME" VARCHAR2(64 CHAR),
    "AUDIT_ADDITIONAL_INFO" VARCHAR2(256 CHAR),
     CONSTRAINT "OFFENDER_RESTRICTIONS_PK" PRIMARY KEY ("OFFENDER_RESTRICTION_ID")
  );

  CREATE INDEX "OFFENDER_RESTRICTIONS_NI1" ON "OFFENDER_RESTRICTIONS" ("OFFENDER_BOOK_ID");


  CREATE INDEX "OFF_REST_STF_FK1" ON "OFFENDER_RESTRICTIONS" ("ENTERED_STAFF_ID");


  CREATE INDEX "OFF_REST_STF_FK2" ON "OFFENDER_RESTRICTIONS" ("AUTHORISED_STAFF_ID");


  CREATE UNIQUE INDEX "OFFENDER_RESTRICTIONS_PK" ON "OFFENDER_RESTRICTIONS" ("OFFENDER_RESTRICTION_ID");
