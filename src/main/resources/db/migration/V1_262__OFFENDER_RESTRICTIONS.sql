create table OFFENDER_RESTRICTIONS
(
    OFFENDER_BOOK_ID              NUMBER(10)                             not null
        constraint OFF_REST_OFF_BKG_FK
            references OFFENDER_BOOKINGS,
    OFFENDER_RESTRICTION_ID       NUMBER(10)                             not null
        constraint OFFENDER_RESTRICTIONS_PK
            primary key,
    RESTRICTION_TYPE              VARCHAR2(12 char)                      not null,
    EFFECTIVE_DATE                DATE                                   not null,
    EXPIRY_DATE                   DATE,
    COMMENT_TEXT                  VARCHAR2(240 char),
    AUTHORISED_STAFF_ID           NUMBER(10)
        constraint OFF_REST_STF_FK2
            references STAFF_MEMBERS,
    ENTERED_STAFF_ID              NUMBER(10)                             not null
        constraint OFF_REST_STF_FK1
            references STAFF_MEMBERS,
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
)
;

comment on table OFFENDER_RESTRICTIONS is 'The general restrictions imposed on an offender'
;

comment on column OFFENDER_RESTRICTIONS.OFFENDER_BOOK_ID is 'The offender book ID'
;

comment on column OFFENDER_RESTRICTIONS.OFFENDER_RESTRICTION_ID is 'The ID of the restriction'
;

comment on column OFFENDER_RESTRICTIONS.RESTRICTION_TYPE is 'The restriction type.  Reference Codes(VIS_RST_TYPE)'
;

comment on column OFFENDER_RESTRICTIONS.EFFECTIVE_DATE is 'The effective date'
;

comment on column OFFENDER_RESTRICTIONS.EXPIRY_DATE is 'The expiry date of the restrictions'
;

comment on column OFFENDER_RESTRICTIONS.COMMENT_TEXT is 'The general comment text'
;

comment on column OFFENDER_RESTRICTIONS.AUTHORISED_STAFF_ID is 'The staff who authroises the restriction'
;

comment on column OFFENDER_RESTRICTIONS.ENTERED_STAFF_ID is 'The staff who enters the restriction'
;

comment on column OFFENDER_RESTRICTIONS.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column OFFENDER_RESTRICTIONS.CREATE_USER_ID is 'The user who creates the record'
;

comment on column OFFENDER_RESTRICTIONS.MODIFY_DATETIME is 'The timestamp when the record is modified '
;

comment on column OFFENDER_RESTRICTIONS.MODIFY_USER_ID is 'The user who modifies the record'
;

