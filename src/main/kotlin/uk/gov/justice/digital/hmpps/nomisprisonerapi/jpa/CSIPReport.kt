package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_CSIP_REPORTS")
@EntityOpen
data class CSIPReport(
  @Id
  @Column(name = "CSIP_ID")
  @SequenceGenerator(name = "CSIP_ID", sequenceName = "CSIP_ID", allocationSize = 1)
  @GeneratedValue(generator = "CSIP_ID")
  val id: Long = 0,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking,

  @Column(name = "ROOT_OFFENDER_ID", nullable = false)
  val rootOffenderId: Long,

  // ------------------------- Referral Details -------------------------//
  @Column(name = "CSIP_SEQ")
  val logNumber: String? = null,

  @Column(name = "RFR_INCIDENT_DATE", nullable = false)
  val incidentDate: LocalDate = LocalDate.now(),

  @Column(name = "RFR_INCIDENT_TIME", nullable = false)
  val incidentTime: LocalDateTime = LocalDateTime.now(),

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CSIPIncidentType.CSIP_TYP + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "RFR_INCIDENT_TYPE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val type: CSIPIncidentType,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CSIPIncidentLocation.CSIP_LOC + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "RFR_INCIDENT_LOCATION", referencedColumnName = "code", nullable = true)),
    ],
  )
  val location: CSIPIncidentLocation,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CSIPAreaOfWork.CSIP_FUNC + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "RFR_CSIP_FUNCTION", referencedColumnName = "code", nullable = true)),
    ],
  )
  val areaOfWork: CSIPAreaOfWork,

  @Column(name = "RFR_REPORTED_BY")
  val reportedBy: String? = null,

  @Column(name = "RFR_DATE_REPORTED", nullable = false)
  val reportedDate: LocalDate = LocalDate.now(),

  @Column(name = "RFR_PROACTIVE_RESPONSE")
  @Convert(converter = YesNoConverter::class)
  val proActiveReferral: Boolean = false,

  @Column(name = "RFR_STAFF_ASSAULTED")
  @Convert(converter = YesNoConverter::class) val staffAssaulted: Boolean = false,

  @Column(name = "RFR_STAFF_NAME")
  val staffAssaultedName: String? = null,

  // ------------------ Additional Referral Details ---------------------//

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CSIPInvolvement.CSIP_INV + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "CDR_INVOLVEMENT",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  val involvement: CSIPInvolvement? = null,

  @Column(name = "CDR_CONCERN_DESCRIPTION")
  val concernDescription: String? = null,

  @OneToMany(mappedBy = "csipReport", cascade = [CascadeType.ALL], orphanRemoval = true)
  val factors: MutableList<CSIPFactor> = mutableListOf(),

  @Column(name = "INV_KNOWN_REASONS")
  val knownReasons: String? = null,

  @Column(name = "CDR_OTHER_INFORMATION")
  val otherInformation: String? = null,

  // REFERRAL_COMPLETE_FLAG VARCHAR2(1) DEFAULT 'N',
  // REFERRAL_COMPLETED_BY VARCHAR2(32),
  // REFERRAL_COMPLETED_DATE DATE,
  // CDR_SENT_DENT VARCHAR2(1) DEFAULT 'N',

  // -------------------- Safer Custody Screening -----------------------//

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CSIPOutcome.CSIP_OUT + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "CDR_OUTCOME",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var outcome: CSIPOutcome? = null,

  @Column(name = "CDR_DECISION_REASON")
  var reasonForDecision: String? = null,

  @Column(name = "CDR_OUTCOME_RECORDED_BY")
  var outcomeCreateUsername: String? = null,

  @Column(name = "CDR_OUTCOME_DATE")
  var outcomeCreateDate: LocalDate? = null,

  // --------------------------- Investigation --------------------------//

  @Column(name = "INV_STAFF_INVOLVED")
  var staffInvolved: String? = null,

  @Column(name = "INV_EVIDENCE_SECURED")
  var evidenceSecured: String? = null,

  @Column(name = "INV_OCCURRENCE_REASON")
  var reasonOccurred: String? = null,

  @Column(name = "INV_USUAL_BEHAVIOUR")
  var usualBehaviour: String? = null,

  @Column(name = "INV_PERSONS_TRIGGER")
  var trigger: String? = null,

  @Column(name = "INV_PROTECTIVE_FACTORS")
  var protectiveFactors: String? = null,

  @OneToMany(mappedBy = "csipReport", cascade = [CascadeType.ALL], orphanRemoval = true)
  val interviews: MutableList<CSIPInterview> = mutableListOf(),

  // TODO Investigate when/where these are set
  // INV_NOMIS_CASE_NOTE VARCHAR2(1) DEFAULT 'N',
  // CDR_RELEASE_DATE DATE, -- on referral details
  // AGY_LOC_ID VARCHAR2(6),

  // ---- NOT MAPPED ---- //
  // RFR_COMMENT VARCHAR2(4000), = all null in preprod
  // INV_NAME VARCHAR2(100),  = all null in preprod
  // MODIFY_DATETIME TIMESTAMP,
  // MODIFY_USER_ID VARCHAR2(32),
  // AUDIT_TIMESTAMP TIMESTAMP,
  // AUDIT_USER_ID VARCHAR2(32),
  // AUDIT_MODULE_NAME VARCHAR2(65),
  // AUDIT_CLIENT_USER_ID VARCHAR2(64),
  // AUDIT_CLIENT_IP_ADDRESS VARCHAR2(39),
  // AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64),
  // AUDIT_ADDITIONAL_INFO VARCHAR2(256),

  // ---------------------- Decisions & Actions -------------------------/
  // INV_CONCLUSION VARCHAR2(4000),
  // INV_OUTCOME VARCHAR2(12),
  // INV_SIGNED_OFF_BY VARCHAR2(12),
  // INV_OUTCOME_RECORDED_BY VARCHAR2(100),
  // INV_OUTCOME_DATE DATE,
  // INV_NEXT_STEPS VARCHAR2(4000),
  // INV_OTHER VARCHAR2(4000),
  // OPEN_CSIP_ALERT VARCHAR2(1) DEFAULT 'N',
  // INV_NON_ASSOC_UPDATED VARCHAR2(1) DEFAULT 'N',
  // INV_OBSERVATION_BOOK VARCHAR2(1) DEFAULT 'N',
  // INV_MOVE VARCHAR2(1) DEFAULT 'N',
  // INV_REVIEW VARCHAR2(1) DEFAULT 'N',
  // INV_SERVICE_REFERRAL VARCHAR2(1) DEFAULT 'N',
  // INV_SIM_REFERRAL VARCHAR2(1) DEFAULT 'N',

  // ----------------------------- Plan & Decision-----------------------//

  // CASE_MANAGER VARCHAR2(100),
  // REASON VARCHAR2(240),
  // CASE_REV_DATE DATE,
  @OneToMany(mappedBy = "csipReport", cascade = [CascadeType.ALL], orphanRemoval = true)
  val plans: MutableList<CSIPPlan> = mutableListOf(),

  // @OneToMany(mappedBy = "csipReport", cascade = [CascadeType.ALL], orphanRemoval = true)
  // val reviews: MutableList<CSIPReview> = mutableListOf(),

  // ---------------------------------------------------------------------//
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  @Column(name = "CREATE_DATETIME", insertable = false, updatable = false)
  @Generated
  lateinit var createDatetime: LocalDateTime

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CSIPReport

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id)"
  }
}
