INSERT INTO REFERENCE_CODES (DOMAIN, CODE, PARENT_DOMAIN, PARENT_CODE, DESCRIPTION, LIST_SEQ, ACTIVE_FLAG, SYSTEM_DATA_FLAG, EXPIRED_DATE)
VALUES
    ('CLOAD_TYPE', 'ALL', null, null, 'All', 4, 'Y', 'N', null),
    ('CLOAD_TYPE', 'APP', null, null, 'Application access caseload', 99, 'Y', 'N', null),
    ('CLOAD_TYPE', 'AUTO', null, null, 'System', 99, 'Y', 'N', null),
    ('CLOAD_TYPE', 'BOTH', null, null, 'Both Community/Institutional Caseloads', 3, 'Y','N', null),
    ('CLOAD_TYPE', 'COMM', null, null, 'Probation Office', 1, 'Y', 'N', null),
    ('CLOAD_TYPE', 'INST', null, null, 'Institutional Establishment', 2, 'Y', 'N', null),
    ('CLO_CON_RSN', 'CLOSE_CON', null, '(SYSDEP)', 'Work Items Transfer out', 99, 'Y', 'N', TO_DATE('2004-09-07', 'YYYY-MM-DD'));
