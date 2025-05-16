create table LINK_CASE_TXNS
(
    CASE_ID                       NUMBER(10)                             not null
        constraint LINK_CASE_TXN_OFF_CASE_FK1
            references OFFENDER_CASES,
    COMBINED_CASE_ID              NUMBER(10)                             not null
        constraint LINK_CASE_TXN_OFF_CASE_FK2
            references OFFENDER_CASES,
    OFFENDER_CHARGE_ID            NUMBER(10)                             not null
        constraint LINK_CASE_TXN_OFF_CHG_FK
            references OFFENDER_CHARGES,
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
    EVENT_ID                      NUMBER(10)
        constraint LINK_CASE_TXNS_FK3
            references COURT_EVENTS,
    constraint LINK_CASE_TXNS_PK
        primary key (CASE_ID, COMBINED_CASE_ID, OFFENDER_CHARGE_ID)
)
;

comment on table LINK_CASE_TXNS is 'The transaction details of link cases'
;

comment on column LINK_CASE_TXNS.CASE_ID is 'The original case ID'
;

comment on column LINK_CASE_TXNS.COMBINED_CASE_ID is 'The new combined case ID'
;

comment on column LINK_CASE_TXNS.OFFENDER_CHARGE_ID is 'The offender charge ID'
;

