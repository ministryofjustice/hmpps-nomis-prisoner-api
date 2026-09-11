create table RANDOM_TESTING_PROGRAMS
(
    RTP_ID                        NUMBER(10)                             not null
        constraint RTP_PK primary key,
    CASELOAD_ID                   VARCHAR2(6 char)                       not null,
    RTP_DATE                      DATE                                   not null,
    MAIN_PERCENTAGE               NUMBER(3)                              not null,
    RESERVE_PERCENTAGE            NUMBER(3)                              not null,
    SELECTIONS_COUNT              NUMBER(6),
    ELIGIBLE_COUNT                NUMBER(6),
    RESERVE_COUNT                 NUMBER(6),
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
    constraint RTP_UK unique (CASELOAD_ID, RTP_DATE)
);

comment on column RANDOM_TESTING_PROGRAMS.RTP_ID is 'Id of random testing programme';
comment on column RANDOM_TESTING_PROGRAMS.CASELOAD_ID is 'The caseload of the programme';
comment on column RANDOM_TESTING_PROGRAMS.RTP_DATE is 'Date of random testing programme';
comment on column RANDOM_TESTING_PROGRAMS.MAIN_PERCENTAGE is 'Required % for main selection';
comment on column RANDOM_TESTING_PROGRAMS.RESERVE_PERCENTAGE is 'Required % for reserve selection';
comment on column RANDOM_TESTING_PROGRAMS.SELECTIONS_COUNT is 'Number of selections on main list';
comment on column RANDOM_TESTING_PROGRAMS.ELIGIBLE_COUNT is 'Number of eligible offenders at generation of random list';
comment on column RANDOM_TESTING_PROGRAMS.RESERVE_COUNT is 'Number of selections on reserve list';
comment on column RANDOM_TESTING_PROGRAMS.CREATE_DATETIME is 'The timestamp when the record is created';
comment on column RANDOM_TESTING_PROGRAMS.CREATE_USER_ID is 'The user who creates the record';
comment on column RANDOM_TESTING_PROGRAMS.MODIFY_DATETIME is 'The timestamp when the record is modified ';
comment on column RANDOM_TESTING_PROGRAMS.MODIFY_USER_ID is 'The user who modifies the record';

create sequence RTP_ID START WITH 1;


