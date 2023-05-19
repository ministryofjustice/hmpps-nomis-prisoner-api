create table CASELOADS
(
  CASELOAD_ID                   VARCHAR2(6 char)                       not null
    constraint CASELOAD_PK
      primary key,
  DESCRIPTION                   VARCHAR2(40 char)                      not null,
  CASELOAD_TYPE                 VARCHAR2(12 char)                      not null,
  LIST_SEQ                      NUMBER(6),
  TRUST_ACCOUNTS_FLAG           VARCHAR2(1 char)  default 'Y'          not null,
  ACCESS_PROPERTY_FLAG          VARCHAR2(1 char)  default 'N',
  TRUST_CASELOAD_ID             VARCHAR2(6 char),
  PAYROLL_FLAG                  VARCHAR2(1 char)  default 'N'          not null,
  ACTIVE_FLAG                   VARCHAR2(1 char)  default 'Y'          not null,
  DEACTIVATION_DATE             DATE,
  COMMISSARY_FLAG               VARCHAR2(1 char)  default 'N'          not null,
  PAYROLL_TRUST_CASELOAD        VARCHAR2(6 char)
    constraint CSLD_FK3
      references CASELOADS,
  COMMISSARY_TRUST_CASELOAD     VARCHAR2(6 char)
    constraint CSLD_FK1
      references CASELOADS,
  TRUST_COMMISSARY_CASELOAD     VARCHAR2(6 char)
    constraint CSLD_FK2
      references CASELOADS,
  COMMUNITY_TRUST_CASELOAD      VARCHAR2(6 char)
    constraint CSLD_FK4
      references CASELOADS,
  MDT_FLAG                      VARCHAR2(1 char)  default 'N'          not null,
  CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
  CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
  MODIFY_DATETIME               TIMESTAMP(9),
  MODIFY_USER_ID                VARCHAR2(32 char),
  AUDIT_TIMESTAMP               TIMESTAMP(9),
  AUDIT_USER_ID                 VARCHAR2(32 char),
  AUDIT_MODULE_NAME             VARCHAR2(65 char),
  AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
  AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
  AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
  AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
  CASELOAD_FUNCTION             VARCHAR2(12 char)                      not null
)
;

comment on table CASELOADS is 'An administrative business unit (grouping) for management of offender records by establishments (Institution) and;or probation offices;court workers (Community).'
;

comment on column CASELOADS.CASELOAD_ID is 'An identifing code for a caseload'
;

comment on column CASELOADS.DESCRIPTION is 'The description of the Caseload ID'
;

comment on column CASELOADS.CASELOAD_TYPE is 'Refrence Code [ CSLD_TYPE ] : Type of Caseload - ie Institution, Office etc.'
;

comment on column CASELOADS.LIST_SEQ is 'Controls the order in which caseload information will appear on a list of values.'
;

comment on column CASELOADS.TRUST_ACCOUNTS_FLAG is 'Indicates this institutional caseload has trust accounting capabilities.'
;

comment on column CASELOADS.ACTIVE_FLAG is 'Active data indicator'
;

comment on column CASELOADS.COMMISSARY_FLAG is 'Caseload is a Commissary Caseload.'
;

comment on column CASELOADS.PAYROLL_TRUST_CASELOAD is 'Central Trust Caseload for Payroll. Multiple Payroll, Single Trust.'
;

comment on column CASELOADS.COMMISSARY_TRUST_CASELOAD is 'Central Trust Caseload for Commissary. Multiple Commissary, Single Trust.'
;

comment on column CASELOADS.TRUST_COMMISSARY_CASELOAD is 'Central Commissary Caseload for Trust. Multiple Trust, One Commissary.'
;

comment on column CASELOADS.COMMUNITY_TRUST_CASELOAD is 'Link between a non-financial community caseload to a financial admin caseload'
;

comment on column CASELOADS.MDT_FLAG is 'Mandatory Drug Testing Flag'
;

comment on column CASELOADS.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column CASELOADS.CREATE_USER_ID is 'The user who creates the record'
;

comment on column CASELOADS.MODIFY_DATETIME is 'The timestamp when the record is modified '
;

comment on column CASELOADS.MODIFY_USER_ID is 'The user who modifies the record'
;

