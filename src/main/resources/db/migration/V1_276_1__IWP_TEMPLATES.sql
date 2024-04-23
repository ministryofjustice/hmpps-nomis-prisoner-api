CREATE TABLE "IWP_TEMPLATES" (
    "TEMPLATE_ID"	NUMBER	NOT NULL,
    "TEMPLATE_NAME"	VARCHAR2(12 CHAR)	NOT NULL,
    "DESCRIPTION"	VARCHAR2(256 CHAR),
    "LOCATION"	VARCHAR2(256 CHAR),
    "ACTIVE_FLAG" VARCHAR2(1 CHAR) DEFAULT 'N',
    "TEMPLATE_BODY"	BLOB,
    "DATE_CREATED" DATE DEFAULT trunc(sysdate) NOT NULL,
    "USER_CREATED" VARCHAR2(32 CHAR) DEFAULT user NOT NULL,
    "LOCK_PASSWORD"	VARCHAR2(32 CHAR),
    "OBJECT_TYPE" VARCHAR2(12 CHAR),
    "CREATE_DATETIME" TIMESTAMP (9) DEFAULT systimestamp NOT NULL,
    "CREATE_USER_ID" VARCHAR2(32 CHAR) DEFAULT USER NOT NULL,
    "MODIFY_DATETIME" TIMESTAMP (9),
    "MODIFY_USER_ID" VARCHAR2(32 CHAR),
    "EXPIRY_DATE"	DATE,
    "AUDIT_TIMESTAMP" TIMESTAMP (9),
    "AUDIT_USER_ID" VARCHAR2(32 CHAR),
    "AUDIT_MODULE_NAME" VARCHAR2(65 CHAR),
    "AUDIT_CLIENT_USER_ID" VARCHAR2(64 CHAR),
    "AUDIT_CLIENT_IP_ADDRESS" VARCHAR2(39 CHAR),
    "AUDIT_CLIENT_WORKSTATION_NAME" VARCHAR2(64 CHAR),
    CONSTRAINT "IWP_TEMPLATES_PK" PRIMARY KEY ("TEMPLATE_ID")
);
CREATE UNIQUE INDEX "IWP_TEMPLATES_UK" ON "IWP_TEMPLATES" ("TEMPLATE_NAME");
