create table OIC_OFFENCES
(
    OIC_OFFENCE_CODE              VARCHAR2(12 char)                      not null,
    DESCRIPTION                   VARCHAR2(350 char)                     not null,
    ACTIVE_FLAG                   VARCHAR2(1 char)  default 'Y'          not null,
    LIST_SEQ                      NUMBER(6),
    UPDATE_ALLOWED_FLAG           VARCHAR2(1 char)  default 'Y'          not null,
    EXPIRY_DATE                   DATE,
    MODIFY_USER_ID                VARCHAR2(32 char),
    OIC_OFFENCE_CATEGORY          VARCHAR2(12 char),
    OIC_OFFENCE_TYPE              VARCHAR2(12 char),
    MAX_PENALTY_MONTHS            NUMBER(3),
    MAX_PENALTY_DAYS              NUMBER(3),
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
    START_DATE                    DATE                                   not null,
    END_DATE                      DATE,
    OIC_OFFENCE_ID                NUMBER(10)                             not null
        constraint OIC_OFFENCES_PK
            primary key,
    constraint OIC_OFFENCES_UK1
        unique (OIC_OFFENCE_CODE, START_DATE)
)
;

comment on table OIC_OFFENCES is 'A disciplinary offence contained in Prison Rule 51 or YOI Rule 55 (young offenders). NOTE: at some point before Release 8, the primary key of this entity was changed, to enable effective dating of records. This was done because from time to time the paragraph offence description (which is the on-screen caption for the Description attribute of this entity) is changed, but we want to retain the same Offence Code. This could not be done previously, when the PK was simply Oic Offence Code - the Active Flag for the record could be set to no, but then the replacement record would have had to have a different value of Oic Offence Code. Not ideal. What we now have is a new internal numeric ID Oic Offence Id as the new PK of this entity, plus a new unique Alternate Key composed of the previous PK Oic Offence Code plus new attribute Start Date.'
;

comment on column OIC_OFFENCES.OIC_OFFENCE_CODE is 'The code associated with the OIC charge.'
;

comment on column OIC_OFFENCES.DESCRIPTION is 'Description associated with offence code.'
;

comment on column OIC_OFFENCES.ACTIVE_FLAG is 'An active code will display on list of values. If
 the active flag is set to "N", then the Expiry Date
 field will be populated with the system date.
 If the flag is subsequently set to "Y" the Expiry
 Date field will be cleared.'
;

comment on column OIC_OFFENCES.LIST_SEQ is 'Ordering for list of values.'
;

comment on column OIC_OFFENCES.UPDATE_ALLOWED_FLAG is 'Protected flag for code.'
;

comment on column OIC_OFFENCES.EXPIRY_DATE is 'Deactivation date for code.'
;

comment on column OIC_OFFENCES.MODIFY_USER_ID is 'The user who modifies the record'
;

comment on column OIC_OFFENCES.OIC_OFFENCE_CATEGORY is 'Reference Code ( OIC_OFN_CAT ). The category or grouping for OIC offences.'
;

comment on column OIC_OFFENCES.OIC_OFFENCE_TYPE is 'Reference Code (OIC_OFN_TYPE) The type of offence (FULL, PD)'
;

comment on column OIC_OFFENCES.MAX_PENALTY_MONTHS is 'Max penalty in months'
;

comment on column OIC_OFFENCES.MAX_PENALTY_DAYS is 'Max penalty in days'
;

comment on column OIC_OFFENCES.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column OIC_OFFENCES.CREATE_USER_ID is 'The user who creates the record'
;

comment on column OIC_OFFENCES.MODIFY_DATETIME is 'The timestamp when the record is modified '
;

comment on column OIC_OFFENCES.START_DATE is 'The date the Paragraph is activated'
;

comment on column OIC_OFFENCES.END_DATE is 'The date the Paragraph is deactivated'
;
;
