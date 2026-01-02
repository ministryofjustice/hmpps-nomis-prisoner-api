INSERT INTO ASSESSMENTS (ASSESSMENT_ID,ASSESSMENT_CLASS,PARENT_ASSESSMENT_ID,ASSESSMENT_CODE,CELL_SHARING_ALERT_FLAG,DESCRIPTION,LIST_SEQ,ACTIVE_FLAG,UPDATE_ALLOWED_FLAG,EFFECTIVE_DATE,EXPIRY_DATE,MUTUAL_EXCLUSIVE_FLAG,
                         DETERMINE_SUP_LEVEL_FLAG,REQUIRE_EVALUATION_FLAG,REQUIRE_APPROVAL_FLAG,REVIEW_CYCLE_DAYS,CASELOAD_TYPE,REVIEW_FLAG,SEARCH_CRITERIA_FLAG,OVERRIDEABLE_FLAG,ASSESSMENT_TYPE,CALCULATE_TOTAL_FLAG,SCREEN_CODE)
  VALUES
 (9682,'TYPE', null,'CSRREV',  'Y','CSR Review',    1,'Y','Y',to_date('01-01-2000','DD-MM-YYYY'),null,                              'N','N','N','Y',90,  'INST','Y','N','N',       null,'Y','ASSESS'),
 (9683,'TYPE', null,'CSRDO',   'Y','CSR Locate',    3,'N','Y',to_date('01-01-2000','DD-MM-YYYY'),to_date('23-05-2009','DD-MM-YYYY'),'N','N','N','Y',null,'INST','N','N','N',       null,'Y',null),
 (9684,'TYPE', null,'CSR1',    'Y','CSR Reception', 1,'Y','Y',to_date('01-01-2000','DD-MM-YYYY'),null,                              'Y','N','Y','Y',null,'INST','N','N','N','INCLUSIVE','Y','ASSESS'),
 (9685,'TYPE', null,'CSRH',    'N','CSR Health',    2,'N','Y',to_date('01-01-2000','DD-MM-YYYY'),to_date('23-05-2009','DD-MM-YYYY'),'N','N','N','Y',null,'INST','Y','N','N',       null,'Y',null),
 (9686,'TYPE', null,'CSRF',    'Y','CSR Full',      1,'Y','Y',to_date('01-01-2000','DD-MM-YYYY'),null,                              'N','N','N','Y',30,  'INST','Y','N','N',       null,'Y','ASSESS'),
 (9687,'TYPE', null,'CSR',     'Y','CSR Rating',    1,'Y','Y',to_date('01-01-2000','DD-MM-YYYY'),null,                              'N','N','N','Y',1,   'INST','Y','N','N',       null,'Y','ASSESS');

-- Just basic CSRA related rows for now. There is more in the prison-api R__ file.
