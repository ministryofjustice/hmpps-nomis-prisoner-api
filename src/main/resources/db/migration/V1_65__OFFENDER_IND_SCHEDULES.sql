CREATE TABLE "OFFENDER_IND_SCHEDULES"
(
  "EVENT_ID"                      NUMBER(10, 0)                            NOT NULL ,
  "OFFENDER_BOOK_ID"              NUMBER(10, 0)                            NOT NULL ,
  "EVENT_DATE"                    DATE,
  "START_TIME"                    DATE,
  "END_TIME"                      DATE,
  "EVENT_CLASS"                   VARCHAR2(12 CHAR)                        NOT NULL ,
  "EVENT_TYPE"                    VARCHAR2(12 CHAR)                        NOT NULL ,
  "EVENT_SUB_TYPE"                VARCHAR2(12 CHAR)                        NOT NULL ,
  "EVENT_STATUS"                  VARCHAR2(12 CHAR)                        NOT NULL ,
  "COMMENT_TEXT"                  VARCHAR2(4000 CHAR),
  "HIDDEN_COMMENT_TEXT"           VARCHAR2(240 CHAR),
  "APPLICATION_DATE"              DATE,
  "PARENT_EVENT_ID"               NUMBER(10, 0),
  "AGY_LOC_ID"                    VARCHAR2(6 CHAR),
  "TO_AGY_LOC_ID"                 VARCHAR2(6 CHAR),
  "TO_INTERNAL_LOCATION_ID"       NUMBER(10, 0),
  "FROM_CITY"                     VARCHAR2(20 CHAR),
  "TO_CITY"                       VARCHAR2(20 CHAR),
  "CRS_SCH_ID"                    NUMBER(10, 0),
  "ORDER_ID"                      NUMBER(10, 0),
  "SENTENCE_SEQ"                  NUMBER(10, 0),
  "OUTCOME_REASON_CODE"           VARCHAR2(12 CHAR),
  "JUDGE_NAME"                    VARCHAR2(60 CHAR),
  "CHECK_BOX_1"                   VARCHAR2(1 CHAR)    DEFAULT 'N',
  "CHECK_BOX_2"                   VARCHAR2(1 CHAR)    DEFAULT 'N',
  "IN_CHARGE_STAFF_ID"            NUMBER(10, 0),
  "CREDITED_HOURS"                NUMBER(6, 0),
  "REPORT_IN_DATE"                DATE,
  "PIECE_WORK"                    NUMBER(11, 2),
  "ENGAGEMENT_CODE"               VARCHAR2(12 CHAR),
  "UNDERSTANDING_CODE"            VARCHAR2(12 CHAR),
  "DETAILS"                       VARCHAR2(40 CHAR),
  "CREDITED_WORK_HOUR"            NUMBER(6, 2),
  "AGREED_TRAVEL_HOUR"            NUMBER(6, 2),
  "UNPAID_WORK_SUPERVISOR"        VARCHAR2(30 CHAR),
  "UNPAID_WORK_BEHAVIOUR"         VARCHAR2(12 CHAR),
  "UNPAID_WORK_ACTION"            VARCHAR2(12 CHAR),
  "SICK_NOTE_RECEIVED_DATE"       DATE,
  "SICK_NOTE_EXPIRY_DATE"         DATE,
  "COURT_EVENT_RESULT"            VARCHAR2(12 CHAR),
  "UNEXCUSED_ABSENCE_FLAG"        VARCHAR2(1 CHAR)    DEFAULT 'N',
  "CREATE_USER_ID"                VARCHAR2(32 CHAR)   DEFAULT USER         NOT NULL ,
  "MODIFY_USER_ID"                VARCHAR2(32 CHAR),
  "CREATE_DATETIME"               TIMESTAMP(9)        DEFAULT systimestamp NOT NULL ,
  "MODIFY_DATETIME"               TIMESTAMP(9),
  "ESCORT_CODE"                   VARCHAR2(12 CHAR),
  "CONFIRM_FLAG"                  VARCHAR2(1 CHAR)    DEFAULT 'N',
  "DIRECTION_CODE"                VARCHAR2(12 CHAR),
  "TO_CITY_CODE"                  VARCHAR2(12 CHAR),
  "FROM_CITY_CODE"                VARCHAR2(12 CHAR),
  "OFF_PRGREF_ID"                 NUMBER(10, 0),
  "IN_TIME"                       DATE,
  "OUT_TIME"                      DATE,
  "PERFORMANCE_CODE"              VARCHAR2(12 CHAR),
  "TEMP_ABS_SCH_ID"               NUMBER(10, 0),
  "REFERENCE_ID"                  NUMBER(10, 0),
  "TRANSPORT_CODE"                VARCHAR2(12 CHAR),
  "APPLICATION_TIME"              DATE,
  "TO_COUNTRY_CODE"               VARCHAR2(12 CHAR),
  "OJ_LOCATION_CODE"              VARCHAR2(12 CHAR),
  "CONTACT_PERSON_NAME"           VARCHAR2(40 CHAR),
  "TO_ADDRESS_OWNER_CLASS"        VARCHAR2(12 CHAR),
  "TO_ADDRESS_ID"                 NUMBER(10, 0),
  "RETURN_DATE"                   DATE,
  "RETURN_TIME"                   DATE,
  "TO_CORPORATE_ID"               NUMBER(10, 0),
  "TA_ID"                         NUMBER(10, 0),
  "EVENT_OUTCOME"                 VARCHAR2(12 CHAR),
  "AUDIT_TIMESTAMP"               TIMESTAMP(9),
  "AUDIT_USER_ID"                 VARCHAR2(32 CHAR),
  "AUDIT_MODULE_NAME"             VARCHAR2(65 CHAR),
  "AUDIT_CLIENT_USER_ID"          VARCHAR2(64 CHAR),
  "AUDIT_CLIENT_IP_ADDRESS"       VARCHAR2(39 CHAR),
  "AUDIT_CLIENT_WORKSTATION_NAME" VARCHAR2(64 CHAR),
  "AUDIT_ADDITIONAL_INFO"         VARCHAR2(256 CHAR),
  "OFFENDER_PRG_OBLIGATION_ID"    NUMBER(10, 0),
  "OFFENDER_MOVEMENT_APP_ID"      NUMBER(10, 0),

  CONSTRAINT "OFFENDER_IND_SCHEDULES_PK" PRIMARY KEY ("EVENT_ID"),

  CONSTRAINT "OFF_IND_SCH_OFF_BKG_FK"     FOREIGN KEY ("OFFENDER_BOOK_ID")           REFERENCES "OFFENDER_BOOKINGS" ("OFFENDER_BOOK_ID") ,
  CONSTRAINT "OFF_IND_SCH_AGY_LOC_FK"     FOREIGN KEY ("AGY_LOC_ID")                 REFERENCES "AGENCY_LOCATIONS" ("AGY_LOC_ID"),
  CONSTRAINT "OFF_IND_SCH_AGY_LOC_FK2"    FOREIGN KEY ("TO_AGY_LOC_ID")              REFERENCES "AGENCY_LOCATIONS" ("AGY_LOC_ID") ,
  -- not required (only a handful where IN_CHARGE_STAFF_ID != NULL in T3) ? CONSTRAINT "OFF_IND_SCH_STF_FK"         FOREIGN KEY ("IN_CHARGE_STAFF_ID")         REFERENCES "STAFF_MEMBERS" ("STAFF_ID") ,
  -- none <> null in T3 CONSTRAINT "OFF_IND_SCH_OFF_PRG_OB_FK"  FOREIGN KEY ("OFFENDER_PRG_OBLIGATION_ID") REFERENCES "OFFENDER_PRG_OBLIGATIONS" ("OFFENDER_PRG_OBLIGATION_ID") ,
  -- Only for TAPs: CONSTRAINT "OFF_IND_SCH_OFF_MOV_APP_FK" FOREIGN KEY ("OFFENDER_MOVEMENT_APP_ID")   REFERENCES "OFFENDER_MOVEMENT_APPS" ("OFFENDER_MOVEMENT_APP_ID") ,

  CONSTRAINT "OFFNDER_IND_SCHEDULE_CHK1" CHECK (EVENT_CLASS IN ('EXT_MOV', 'INT_MOV', 'COMM'))
);

COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."EVENT_ID" IS 'Schedule(Event) ID';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."OFFENDER_BOOK_ID" IS 'FK to Offender Booking';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."EVENT_DATE" IS 'Schedule Date of the event (no time)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."START_TIME" IS 'Schedule Time of the event';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."END_TIME" IS 'Schedule End Time of the event';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."EVENT_CLASS" IS 'Ref Domain (EVENT_CLASS)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."EVENT_TYPE" IS 'Ref Domain (MOVE_TYPE, INT_SCH_TYPE, EVENTS)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."EVENT_SUB_TYPE" IS 'Ref DOmain (MOVE_RSN, INS_SCH_RSN, EVENT_SUBTYP)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."EVENT_STATUS" IS 'Ref Domain (EVENT_STS)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."COMMENT_TEXT" IS 'Comments';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."HIDDEN_COMMENT_TEXT" IS 'Hidden comments for scurity';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."APPLICATION_DATE" IS 'The date time of the schedule application such as temporary absence';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."PARENT_EVENT_ID" IS 'Previous related Event ID';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."AGY_LOC_ID" IS 'Record Owner : Agency Location for security sake';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."TO_AGY_LOC_ID" IS 'Movement : to which agency location';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."TO_INTERNAL_LOCATION_ID" IS 'FK to Agency Internal Location';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."FROM_CITY" IS 'Movement : From which city';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."TO_CITY" IS 'Movement : To which city';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."CRS_SCH_ID" IS 'FK to course schedules';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."ORDER_ID" IS 'FK to Orders';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."SENTENCE_SEQ" IS 'FK to Offender Sentences';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."OUTCOME_REASON_CODE" IS 'Reference Code (CANC_RSN)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."JUDGE_NAME" IS 'For court movement';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."CHECK_BOX_1" IS 'For court movement';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."CHECK_BOX_2" IS 'For court movement';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."IN_CHARGE_STAFF_ID" IS 'The staff memeber who in charge of this schedules such as movement escort';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."CREDITED_HOURS" IS 'Credit hours';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."REPORT_IN_DATE" IS 'Report In Date';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."PIECE_WORK" IS 'No of piece work';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."ENGAGEMENT_CODE" IS 'Ref Domain (ENGAGE)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."UNDERSTANDING_CODE" IS 'Ref Domain (UNSTAND)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."DETAILS" IS 'Details text';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."CREDITED_WORK_HOUR" IS 'No of credited hours';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."AGREED_TRAVEL_HOUR" IS 'Travel time (hours)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."UNPAID_WORK_SUPERVISOR" IS 'supervisor name';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."UNPAID_WORK_BEHAVIOUR" IS 'Ref Domain (BEHAVIOR)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."UNPAID_WORK_ACTION" IS 'Ref Domain (COMM_ACTION)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."SICK_NOTE_RECEIVED_DATE" IS 'The date sick note received';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."SICK_NOTE_EXPIRY_DATE" IS 'The expiry date of the sick note';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."COURT_EVENT_RESULT" IS 'Ref Domain (CRT_RESULT)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."UNEXCUSED_ABSENCE_FLAG" IS 'If the event is unexcused absence';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."CREATE_USER_ID" IS 'The user who creates the record';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."MODIFY_USER_ID" IS 'The user who modifies the record';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."CREATE_DATETIME" IS 'The timestamp when the record is created';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."MODIFY_DATETIME" IS 'The timestamp when the record is modified ';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."ESCORT_CODE" IS 'The escort type of the movements: Reference Code(ESCORT)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."CONFIRM_FLAG" IS '? if the schedule confirmed';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."DIRECTION_CODE" IS 'The direction of the movements: Reference Code(MOVE_DIRECT)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."TO_CITY_CODE" IS 'Reference Code (CITY)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."FROM_CITY_CODE" IS 'Reference Code (CITY))';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."OFF_PRGREF_ID" IS 'The Reference ID of the Offender Program Profiles';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."IN_TIME" IS 'The actual in time';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."OUT_TIME" IS 'The actual out time';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."PERFORMANCE_CODE" IS 'The attendance performance ';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."TEMP_ABS_SCH_ID" IS 'The tempary absence schedule ID';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."REFERENCE_ID" IS 'The general reference ID for virtual schedule';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."TRANSPORT_CODE" IS 'The transport of the movement. Reference code(TRANSPORT)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."APPLICATION_TIME" IS 'The time of application (For internal movements)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."TO_COUNTRY_CODE" IS 'the country of the destination. REference Code(COUNTRY)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."OJ_LOCATION_CODE" IS 'the OJ Location. REference Code(OJ_LOCATION)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."CONTACT_PERSON_NAME" IS 'The contact person name';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."TO_ADDRESS_OWNER_CLASS" IS 'The party class of the address, such as OFF Offender, AGY Agency Location';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."TO_ADDRESS_ID" IS 'The address ID of the movement destination';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."RETURN_DATE" IS 'The Date of the return (for movements such as temporary absence)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."RETURN_TIME" IS 'The Time of the return (for movements such as temporary absence)';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."TO_CORPORATE_ID" IS 'The party ID of corporate for the movement destination';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."TA_ID" IS 'The Temporary absence group ID';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."EVENT_OUTCOME" IS 'The actual outcome of the schedule';
COMMENT ON COLUMN "OFFENDER_IND_SCHEDULES"."OFFENDER_PRG_OBLIGATION_ID" IS 'The offender program obligation ID';

COMMENT ON TABLE "OFFENDER_IND_SCHEDULES" IS 'An event scheduled for an offender serving either a custodial or community sentence. NOTE: This entity does not represent an exhaustive collection of all events that are scheduled for an offender. For example, court appearances and scheduled movement to and from court (from/to prison) are represented by the Court Event entity. Subtypes exist to define each valid type of Offender Individual Schedules.';

CREATE INDEX "OFFENDER_IND_SCHEDULES_NI1" ON "OFFENDER_IND_SCHEDULES" ("OFFENDER_BOOK_ID");
CREATE INDEX "OFFENDER_IND_SCHEDULES_NI2" ON "OFFENDER_IND_SCHEDULES" ("EVENT_DATE");
CREATE INDEX "OFFENDER_IND_SCHEDULES_NI3" ON "OFFENDER_IND_SCHEDULES" ("REFERENCE_ID");
CREATE INDEX "OFFENDER_IND_SCHEDULES_NI4" ON "OFFENDER_IND_SCHEDULES" ("AGY_LOC_ID");
CREATE INDEX "OFFENDER_IND_SCHEDULES_NI5" ON "OFFENDER_IND_SCHEDULES" ("TA_ID");
CREATE INDEX "OFFENDER_IND_SCHEDULES_NI6" ON "OFFENDER_IND_SCHEDULES" ("TEMP_ABS_SCH_ID");
CREATE INDEX "OFFENDER_IND_SCHEDULES_X01" ON "OFFENDER_IND_SCHEDULES" ("EVENT_TYPE", "EVENT_SUB_TYPE", "OFFENDER_BOOK_ID");
CREATE INDEX "OFFENDER_IND_SCHEDULES_X02" ON "OFFENDER_IND_SCHEDULES" ("OFFENDER_BOOK_ID", "EVENT_DATE");
CREATE INDEX "OFF_IND_SCH_AGY_INT_LOC_FK" ON "OFFENDER_IND_SCHEDULES" ("TO_INTERNAL_LOCATION_ID");
CREATE INDEX "OFF_IND_SCH_AGY_LOC_FK2"    ON "OFFENDER_IND_SCHEDULES" ("TO_AGY_LOC_ID");
CREATE INDEX "OFF_IND_SCH_OFF_MOV_APP_FK" ON "OFFENDER_IND_SCHEDULES" ("OFFENDER_MOVEMENT_APP_ID");
CREATE INDEX "OFF_IND_SCH_OFF_PRG_OB_FK"  ON "OFFENDER_IND_SCHEDULES" ("OFFENDER_PRG_OBLIGATION_ID");
CREATE INDEX "OFF_IND_SCH_STF_FK"         ON "OFFENDER_IND_SCHEDULES" ("IN_CHARGE_STAFF_ID");

CREATE TRIGGER OFFENDER_IND_SCHEDULES_TA
  BEFORE UPDATE on OFFENDER_IND_SCHEDULES FOR EACH ROW
  CALL "uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.ModifyTrigger";
