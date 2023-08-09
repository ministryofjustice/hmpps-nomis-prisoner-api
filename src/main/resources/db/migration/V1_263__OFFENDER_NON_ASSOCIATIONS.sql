
  CREATE TABLE "OFFENDER_NON_ASSOCIATIONS"
   (    "OFFENDER_ID" NUMBER(10,0) NOT NULL,
    "NS_OFFENDER_ID" NUMBER(10,0) NOT NULL,
    "OFFENDER_BOOK_ID" NUMBER(10,0) NOT NULL,
    "NS_OFFENDER_BOOK_ID" NUMBER(10,0) NOT NULL,
    "NS_REASON_CODE" VARCHAR2(12 CHAR),
    "NS_LEVEL_CODE" VARCHAR2(12 CHAR),
    "INTERNAL_LOCATION_FLAG" VARCHAR2(12 CHAR),
    "TRANSPORT_FLAG" VARCHAR2(1 CHAR) DEFAULT 'N',
    "RECIP_NS_REASON_CODE" VARCHAR2(12 CHAR),
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
     CONSTRAINT "OFFENDER_NON_ASSOCIATIONS_PK" PRIMARY KEY ("OFFENDER_ID", "NS_OFFENDER_ID")
  );

  CREATE INDEX "OFFENDER_NON_ASSOCIATIONS_NI3" ON "OFFENDER_NON_ASSOCIATIONS" ("NS_OFFENDER_ID");
  CREATE INDEX "OFFENDEDR_NON_ASSOCIATIONS_NI1" ON "OFFENDER_NON_ASSOCIATIONS" ("NS_OFFENDER_BOOK_ID");
  CREATE INDEX "OFFENDER_NON_ASSOCIATIONS_NI2" ON "OFFENDER_NON_ASSOCIATIONS" ("OFFENDER_BOOK_ID");
  CREATE UNIQUE INDEX "OFFENDER_NON_ASSOCIATIONS_PK" ON "OFFENDER_NON_ASSOCIATIONS" ("OFFENDER_ID", "NS_OFFENDER_ID");
