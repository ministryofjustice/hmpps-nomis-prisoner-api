create table ACCOUNT_PERIODS
(
    ACCOUNT_PERIOD_ID             NUMBER(10)                             not null,
    ACCOUNT_PERIOD_TYPE           VARCHAR2(12 char)                      not null,
    START_DATE                    DATE,
    END_DATE                      DATE,
    PARENT_ACCOUNT_PERIOD_ID      NUMBER(10),
    MODIFY_USER_ID                VARCHAR2(32 char),
    MODIFY_DATE                   DATE                                   not null,
    LIST_SEQ                      NUMBER(6)         default 99,
    CREATE_DATETIME               TIMESTAMP(9)      default systimestamp not null,
    CREATE_USER_ID                VARCHAR2(32 char) default USER         not null,
    MODIFY_DATETIME               TIMESTAMP(9),
    AUDIT_TIMESTAMP               TIMESTAMP(9),
    AUDIT_USER_ID                 VARCHAR2(32 char),
    AUDIT_MODULE_NAME             VARCHAR2(65 char),
    AUDIT_CLIENT_USER_ID          VARCHAR2(64 char),
    AUDIT_CLIENT_IP_ADDRESS       VARCHAR2(39 char),
    AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64 char),
    AUDIT_ADDITIONAL_INFO         VARCHAR2(256 char),
    constraint ACCOUNT_PERIODS_PK
        primary key (ACCOUNT_PERIOD_ID),
    constraint AC_PRD_AC_PRD_F1
        foreign key (PARENT_ACCOUNT_PERIOD_ID) references ACCOUNT_PERIODS
);

comment on table ACCOUNT_PERIODS is 'Maintenance of accounting periods.';

comment on column ACCOUNT_PERIODS.ACCOUNT_PERIOD_ID is 'Identifier for an accounting period ie. 9601, 9602..';

comment on column ACCOUNT_PERIODS.ACCOUNT_PERIOD_TYPE is 'Reference Code [PERIOD_TYPE ] ie. Month, Year';

comment on column ACCOUNT_PERIODS.START_DATE is 'The start date for the accounting period.';

comment on column ACCOUNT_PERIODS.END_DATE is 'The end date for the accounting period.';

comment on column ACCOUNT_PERIODS.PARENT_ACCOUNT_PERIOD_ID is 'Master Account Period';

comment on column ACCOUNT_PERIODS.MODIFY_USER_ID is 'The user who modifies the record';

comment on column ACCOUNT_PERIODS.MODIFY_DATE is 'Modify Date';

comment on column ACCOUNT_PERIODS.LIST_SEQ is 'The sequence in which the data should be shown';

comment on column ACCOUNT_PERIODS.CREATE_DATETIME is 'The timestamp when the record is created';

comment on column ACCOUNT_PERIODS.CREATE_USER_ID is 'The user who creates the record';

comment on column ACCOUNT_PERIODS.MODIFY_DATETIME is 'The timestamp when the record is modified ';

CREATE UNIQUE INDEX ACCOUNT_PERIODS_PK ON ACCOUNT_PERIODS (ACCOUNT_PERIOD_ID);
CREATE INDEX AC_PRD_AC_PRD_F1 ON ACCOUNT_PERIODS (PARENT_ACCOUNT_PERIOD_ID);
CREATE INDEX ACCOUNT_PERIODS_N1 ON ACCOUNT_PERIODS (START_DATE, END_DATE);
