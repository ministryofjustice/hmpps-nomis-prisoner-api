create table OFFENDER_CASE_STATUSES
(
    CASE_ID                       NUMBER(10)                             not null
        constraint OFF_CASE_STS_OFF_CASE_FK
            references OFFENDER_CASES,
    STATUS_UPDATE_REASON          VARCHAR2(12 char)                      not null
        constraint OFF_CASE_STS_LGL_UPD_RSN_FK
            references LEGAL_UPDATE_REASONS,
    STATUS_UPDATE_COMMENT         VARCHAR2(400 char),
    STATUS_UPDATE_DATE            DATE              default sysdate      not null,
    STATUS_UPDATE_STAFF_ID        NUMBER(10)                             not null,
    OFFENDER_CASE_STATUS_ID       NUMBER(10)                             not null
        constraint OFFENDER_CASE_STATUSES_PK
            primary key,
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
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char)
);
comment on table OFFENDER_CASE_STATUSES is 'The historical record of status updated reasons';
comment on column OFFENDER_CASE_STATUSES.CASE_ID is 'The case ID';
comment on column OFFENDER_CASE_STATUSES.STATUS_UPDATE_REASON is 'The update reason';
comment on column OFFENDER_CASE_STATUSES.STATUS_UPDATE_COMMENT is 'The comment of the update reason';
comment on column OFFENDER_CASE_STATUSES.STATUS_UPDATE_DATE is 'The date of the reaon updated';
comment on column OFFENDER_CASE_STATUSES.STATUS_UPDATE_STAFF_ID is 'The staff who perform the update reasons';
comment on column OFFENDER_CASE_STATUSES.OFFENDER_CASE_STATUS_ID is 'The ID of the status update reason history';