create table AGENCY_LOCATION_AUTHORITIES
(
    AGY_LOC_ID                    VARCHAR2(6 char)                       not null
        constraint AGY_LOC_AUTH_AGY_LOC_FK
            references AGENCY_LOCATIONS,
    LOCAL_AUTHORITY_CODE          VARCHAR2(12 char)                      not null,
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
    constraint AGENCY_LOCATION_AUTHORITIES_PK
        primary key (AGY_LOC_ID, LOCAL_AUTHORITY_CODE)
)
;

comment on table AGENCY_LOCATION_AUTHORITIES is 'A Local Authority (i.e. Government organisation responsible for a specific geographic area) association with an Agency Location (e.g. Prison, Probation Office, Court).'
;

comment on column AGENCY_LOCATION_AUTHORITIES.AGY_LOC_ID is 'FK to Agency location'
;

comment on column AGENCY_LOCATION_AUTHORITIES.LOCAL_AUTHORITY_CODE is 'Reference Code(LOC_AUTH)'
;

