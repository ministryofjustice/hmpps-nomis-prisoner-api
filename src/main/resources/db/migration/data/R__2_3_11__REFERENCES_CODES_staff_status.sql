INSERT INTO REFERENCE_CODES (DOMAIN, CODE, PARENT_DOMAIN, PARENT_CODE, DESCRIPTION, LIST_SEQ, ACTIVE_FLAG, SYSTEM_DATA_FLAG, EXPIRED_DATE)
VALUES
    ('STAFF_STATUS', 'ACTIVE', null, null, 'Active', 1, 'Y', 'N', null),
    ('STAFF_STATUS', 'CAREER', null, null, 'Career Break', 2, 'Y', 'N', null),
    ('STAFF_STATUS', 'INACT', null, null, 'Inactive', 1, 'Y', 'N', null),
    ('STAFF_STATUS', 'MAT', null, null, 'Maternity Leave', 2, 'Y', 'N', null),
    ('STAFF_STATUS', 'PAT', null, null, 'Paternity Leave', 2, 'Y', 'N', null),
    ('STAFF_STATUS', 'SAB', null, null, 'Sabbatical Leave', 2, 'Y', 'N', null),
    ('STAFF_STATUS', 'SICK', null, null, 'Long Term Sick', 2, 'Y', 'N', null),
    ('STAFF_STATUS', 'SUS', null, null, 'Suspended', 2, 'Y', 'N', null);
