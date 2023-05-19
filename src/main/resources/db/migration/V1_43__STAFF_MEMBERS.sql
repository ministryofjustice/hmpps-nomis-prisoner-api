create table STAFF_MEMBERS
(
  STAFF_ID                      NUMBER(10)                             not null
    constraint STAFF_MEMBERS_PK
      primary key,
  ASSIGNED_CASELOAD_ID          VARCHAR2(6 char)
    constraint STAFF_CSLD_F1
      references CASELOADS,
  WORKING_STOCK_LOC_ID          VARCHAR2(6 char),
  WORKING_CASELOAD_ID           VARCHAR2(6 char)
    constraint STAFF_CSLD_F2
      references CASELOADS,
  USER_ID                       VARCHAR2(32 char)
    constraint STAFF_MEMBERS_PK2
      unique,
  BADGE_ID                      VARCHAR2(20 char),
  LAST_NAME                     VARCHAR2(35 char)                      not null,
  FIRST_NAME                    VARCHAR2(35 char)                      not null,
  MIDDLE_NAME                   VARCHAR2(35 char),
  ABBREVIATION                  VARCHAR2(15 char),
  POSITION                      VARCHAR2(25 char),
  BIRTHDATE                     DATE,
  TERMINATION_DATE              DATE,
  UPDATE_ALLOWED_FLAG           VARCHAR2(1 char)  default 'Y'          not null,
  DEFAULT_PRINTER_ID            NUMBER(10),
  SUSPENDED_FLAG                VARCHAR2(1 char)  default 'N',
  SUPERVISOR_STAFF_ID           NUMBER(10)
    constraint STAFF_STAFF_F1
      references STAFF_MEMBERS,
  COMM_RECEIPT_PRINTER_ID       VARCHAR2(12 char),
  PERSONNEL_TYPE                VARCHAR2(12 char),
  AS_OF_DATE                    DATE,
  EMERGENCY_CONTACT             VARCHAR2(20 char),
  ROLE                          VARCHAR2(12 char),
  SEX_CODE                      VARCHAR2(12 char),
  STATUS                        VARCHAR2(12 char),
  SUSPENSION_DATE               DATE,
  SUSPENSION_REASON             VARCHAR2(12 char),
  FORCE_PASSWORD_CHANGE_FLAG    VARCHAR2(1 char)  default 'N',
  LAST_PASSWORD_CHANGE_DATE     DATE,
  LICENSE_CODE                  VARCHAR2(12 char),
  LICENSE_EXPIRY_DATE           DATE,
  CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
  CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
  MODIFY_DATETIME               TIMESTAMP(9),
  MODIFY_USER_ID                VARCHAR2(32 char),
  TITLE                         VARCHAR2(12 char),
  NAME_SEQUENCE                 VARCHAR2(12 char),
  QUEUE_CLUSTER_ID              NUMBER(6),
  AUDIT_TIMESTAMP               TIMESTAMP(9),
  AUDIT_USER_ID                 VARCHAR2(32 char),
  AUDIT_MODULE_NAME             VARCHAR2(65 char),
  AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
  AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
  AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
  AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
  FIRST_LOGON_FLAG              VARCHAR2(1 char)  default 'N',
  SIGNIFICANT_DATE              DATE,
  SIGNIFICANT_NAME              VARCHAR2(100 char),
  NATIONAL_INSURANCE_NUMBER     VARCHAR2(200 char)
)
;

comment on table STAFF_MEMBERS is 'A person who has a direct (eg. prison officers) or supporting (eg. Human Resources) role within the Offender Management process Typically the person will have a contract of employment with NOMS but in some cases will not - for example, unpaid workers or contractors who require security passes or access to NOMIS.'
;

comment on column STAFF_MEMBERS.STAFF_ID is 'System generated number associated with user account'
;

comment on column STAFF_MEMBERS.ASSIGNED_CASELOAD_ID is ' Caseload staff member assigned to.'
;

comment on column STAFF_MEMBERS.WORKING_STOCK_LOC_ID is ' Commissary location where stock items are kept.'
;

comment on column STAFF_MEMBERS.WORKING_CASELOAD_ID is ' Caseload staff member is currently working on.'
;

comment on column STAFF_MEMBERS.USER_ID is ' User Account Id for the staff member.'
;

comment on column STAFF_MEMBERS.BADGE_ID is 'Officer Badge No.'
;

comment on column STAFF_MEMBERS.LAST_NAME is ' Last name of staff member.'
;

comment on column STAFF_MEMBERS.FIRST_NAME is ' First name of staff member.'
;

comment on column STAFF_MEMBERS.MIDDLE_NAME is ' Middle name of staff member.'
;

comment on column STAFF_MEMBERS.ABBREVIATION is ' Abbreviation of staff member"s name.'
;

comment on column STAFF_MEMBERS.POSITION is ' Staff member"s job position.'
;

comment on column STAFF_MEMBERS.BIRTHDATE is ' Satff member"s birth date.'
;

comment on column STAFF_MEMBERS.TERMINATION_DATE is ' Date staff member terminated from job.'
;

comment on column STAFF_MEMBERS.UPDATE_ALLOWED_FLAG is ' Should user have update capability on caseload (Y;N)?'
;

comment on column STAFF_MEMBERS.DEFAULT_PRINTER_ID is ' Default printer for the staff member.'
;

comment on column STAFF_MEMBERS.SUSPENDED_FLAG is 'Allows for the temporary suspension of the staff member"s user account.'
;

comment on column STAFF_MEMBERS.SUPERVISOR_STAFF_ID is ' Supervisor"s staff id.'
;

comment on column STAFF_MEMBERS.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column STAFF_MEMBERS.CREATE_USER_ID is 'The user who creates the record'
;

comment on column STAFF_MEMBERS.MODIFY_DATETIME is 'The timestamp when the record is modified '
;

comment on column STAFF_MEMBERS.MODIFY_USER_ID is 'The user who modifies the record'
;

comment on column STAFF_MEMBERS.TITLE is 'The title of the staff'
;

comment on column STAFF_MEMBERS.FIRST_LOGON_FLAG is '? If it is the first logon of the staff'
;

