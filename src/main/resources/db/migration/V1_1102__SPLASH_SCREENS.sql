create table SPLASH_SCREENS
(
    SPLASH_ID                     NUMBER                                 not null
        constraint SPLASH_SCREENS_PK
            primary key,
    MODULE_NAME                   VARCHAR2(20 char)                      not null
        constraint SPLASH_SCREENS_UK1
            unique,
    FUNCTION_NAME                 VARCHAR2(100 char)
        constraint SPLASH_SCREENS_FK1
            references SPLASH_SCREEN_FUNCS,
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
    WARNING_TEXT                  VARCHAR2(1000 char),
    BLOCKED_TEXT                  VARCHAR2(1000 char),
    BLOCK_ACCESS_CODE             VARCHAR2(12 char) default 'NO'         not null
);
