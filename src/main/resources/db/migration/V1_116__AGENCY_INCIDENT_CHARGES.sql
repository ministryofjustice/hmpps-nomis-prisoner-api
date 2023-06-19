create table AGENCY_INCIDENT_CHARGES
(
    AGENCY_INCIDENT_ID            NUMBER(10)                             not null,
    CHARGE_SEQ                    NUMBER(6)                              not null,
    PARTY_SEQ                     NUMBER(6)                              not null,
    OIC_CHARGE_ID                 VARCHAR2(13 char)
        constraint AGENCY_INCIDENT_CHARGES_UK
            unique,
    FINDING_CODE                  VARCHAR2(12 char),
    GUILTY_EVIDENCE_TEXT          VARCHAR2(400 char),
    REPORT_TEXT                   VARCHAR2(400 char),
    EVIDENCE_DISPOSE_TEXT         VARCHAR2(400 char),
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
    LIDS_CHARGE_NUMBER            NUMBER(6),
    CHARGED_OIC_OFFENCE_ID        NUMBER(10)                             not null
        constraint AGENCY_INCIDENT_CHARGES_FK1
            references OIC_OFFENCES,
    RESULT_OIC_OFFENCE_ID         NUMBER(10)
        constraint AGENCY_INCIDENT_CHARGES_FK2
            references OIC_OFFENCES,
    constraint AGENCY_INCIDENT_CHARGES_PK
        primary key (AGENCY_INCIDENT_ID, CHARGE_SEQ),
    constraint AGY_INC_CHG_AGY_INC_PTY_FK
        foreign key (AGENCY_INCIDENT_ID, PARTY_SEQ) references AGENCY_INCIDENT_PARTIES
)
;

comment on table AGENCY_INCIDENT_CHARGES is 'An offence against Prison Rule 55 or YOI Rule 55 allegedly committed by an offender during an Agency Incident. This may lead to the offender being charged and an adjudication hearing being held. NOTE : the physical primary key of the table corresponding to this entity is a compound of Agency Incident Id and Charge Seq. The use of this key implies that an offender could be charged with the same offence twice in relation to the same Agency Incident. In business terms that is not the case. However, an alternate primary key index exists formed of attributes Charged Oic Offence Id, Agency Incident Id, & Party Seq. That index is the natural key of this entity. Further, the foreign key constraint to Agency Incident Parties is a compound of Agency Incident Id & Party Seq. The foreign key constraint to OIC Offences is Charged Offence Code. Therefore, for the purpose of better representing the business rules applicable to this area the alternate primary key has been used as the primary key of this entity. QUESTION: what is the purpose of physical column RESULT_OIC_OFFENCE_ID? At some point on the v1.1 database between 15 Nov 06 and 16 June 07, the PK of OIC_OFFENCES was changed from OIC_OFFENCE_CODE to new column OIC_OFFENCE_ID. This change was reflected in the AGENCY_INCIDENT_CHARGES not just in the migrated FK CHARGED_OIC_OFFENCE_ID (changed from _CODE) but in the pre-existing parallel association between the two tables which results in migrated FK RESULT_OIC_OFFENCE_ID. This FK has never been modelled in the LDM. The change was made in the database (which shows both columns pointing to the OIC_OFFENCES table) but there does not appear to be any facility in the Application to allow a result to reference a different Offence from the one the offender was originally charged with. If that is the status quo, then we do not need to model a second association between Oic Offence and Agency Incident Charge. But if the Application is changed to allow a new Offence ID to be selected, then we will have to add a parallel assocation to this model.'
;

comment on column AGENCY_INCIDENT_CHARGES.AGENCY_INCIDENT_ID is 'FK to Agency Incidents'
;

comment on column AGENCY_INCIDENT_CHARGES.CHARGE_SEQ is 'Charge seq as part of the PK'
;

comment on column AGENCY_INCIDENT_CHARGES.PARTY_SEQ is 'FK to Agency Incident Parties'
;

comment on column AGENCY_INCIDENT_CHARGES.OIC_CHARGE_ID is 'The ID for OIC charges'
;

comment on column AGENCY_INCIDENT_CHARGES.FINDING_CODE is 'Reference Code ( FINDING ) ie. Guilty, Not Guilty, Dismissed.'
;

comment on column AGENCY_INCIDENT_CHARGES.GUILTY_EVIDENCE_TEXT is 'Evidence relating to the charge.'
;

comment on column AGENCY_INCIDENT_CHARGES.REPORT_TEXT is 'The notice of report details'
;

comment on column AGENCY_INCIDENT_CHARGES.EVIDENCE_DISPOSE_TEXT is 'What was done with the evidence?'
;

comment on column AGENCY_INCIDENT_CHARGES.CREATE_DATETIME is 'The timestamp when the record is created'
;

comment on column AGENCY_INCIDENT_CHARGES.CREATE_USER_ID is 'The user who creates the record'
;

comment on column AGENCY_INCIDENT_CHARGES.MODIFY_DATETIME is 'The timestamp when the record is modified '
;

comment on column AGENCY_INCIDENT_CHARGES.MODIFY_USER_ID is 'The user who modifies the record'
;

CREATE TRIGGER OFFENCE_ID_GENERATE AFTER INSERT ON AGENCY_INCIDENT_CHARGES FOR EACH ROW CALL "uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.AdjudicationOffenceTrigger";