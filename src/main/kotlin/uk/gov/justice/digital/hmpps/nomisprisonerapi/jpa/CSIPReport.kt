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

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  var originalAgencyLocation: AgencyLocation,

  @Column(name = "ROOT_OFFENDER_ID", nullable = false)
  val rootOffenderId: Long,

  // ------------------------- Referral Details -------------------------//
  @Column(name = "CSIP_SEQ")
  val logNumber: String? = null,

  @Column(name = "RFR_INCIDENT_DATE", nullable = false)
  val incidentDate: LocalDate = LocalDate.now(),

  @Column(name = "RFR_INCIDENT_TIME")
  val incidentTime: LocalDateTime? = null,

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
  @Convert(converter = YesNoConverter::class)
  val staffAssaulted: Boolean = false,

  @Column(name = "RFR_STAFF_NAME")
  val staffAssaultedName: String? = null,

  // ------------------ Additional Referral Details ---------------------//

  @Column(name = "CDR_RELEASE_DATE")
  val releaseDate: LocalDate? = null,

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

  @Column(name = "CDR_SENT_DENT")
  @Convert(converter = YesNoConverter::class)
  val saferCustodyTeamInformed: Boolean = false,
  @Column(name = "REFERRAL_COMPLETE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val referralComplete: Boolean = false,
  @Column(name = "REFERRAL_COMPLETED_BY")
  val referralCompletedBy: String? = null,
  @Column(name = "REFERRAL_COMPLETED_DATE")
  val referralCompletedDate: LocalDate? = null,

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

  // ---- NOT MAPPED - unused ---- //
  // INV_NOMIS_CASE_NOTE VARCHAR2(1) DEFAULT 'N', - are these all N in preprod
  // RFR_COMMENT VARCHAR2(4000), = all null in preprod
  // INV_NAME VARCHAR2(100),  = all null in preprod

  // ---- NOT MAPPED audit data ---- //
  // MODIFY_DATETIME TIMESTAMP,
  // MODIFY_USER_ID VARCHAR2(32),
  // AUDIT_TIMESTAMP TIMESTAMP,
  // AUDIT_USER_ID VARCHAR2(32),
  // AUDIT_MODULE_NAME VARCHAR2(65),
  // AUDIT_CLIENT_USER_ID VARCHAR2(64),
  // AUDIT_CLIENT_IP_ADDRESS VARCHAR2(39),
  // AUDIT_CLIENT_WORKSTATION_NAME VARCHAR2(64),
  // AUDIT_ADDITIONAL_INFO VARCHAR2(256),

  // ---------------------- Decisions & Actions -------------------------//

  @Column(name = "INV_CONCLUSION")
  var conclusion: String? = null,

  // Currently mapped INV_OUTCOME as CSIPOutcome type, same as CDR_OUTCOME:CSIPOutcome
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
          name = "INV_OUTCOME",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var decisionOutcome: CSIPOutcome? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CSIPSignedOffRole.CSIP_ROLE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "INV_SIGNED_OFF_BY",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  var signedOffRole: CSIPSignedOffRole? = null,

  @Column(name = "INV_OUTCOME_RECORDED_BY")
  var recordedBy: String? = null,

  @Column(name = "INV_OUTCOME_DATE")
  var recordedDate: LocalDate? = null,

  @Column(name = "INV_NEXT_STEPS")
  var nextSteps: String? = null,

  @Column(name = "INV_OTHER")
  var otherDetails: String? = null,

  @Column(name = "OPEN_CSIP_ALERT")
  @Convert(converter = YesNoConverter::class)
  var openCSIPAlert: Boolean = false,
  @Column(name = "INV_NON_ASSOC_UPDATED")
  @Convert(converter = YesNoConverter::class)
  var nonAssociationsUpdated: Boolean = false,
  @Column(name = "INV_OBSERVATION_BOOK")
  @Convert(converter = YesNoConverter::class)
  var observationBook: Boolean = false,
  @Column(name = "INV_MOVE")
  @Convert(converter = YesNoConverter::class)
  var unitOrCellMove: Boolean = false,
  @Column(name = "INV_REVIEW")
  @Convert(converter = YesNoConverter::class)
  var csraOrRsraReview: Boolean = false,
  @Column(name = "INV_SERVICE_REFERRAL")
  @Convert(converter = YesNoConverter::class)
  var serviceReferral: Boolean = false,
  @Column(name = "INV_SIM_REFERRAL")
  @Convert(converter = YesNoConverter::class)
  var simReferral: Boolean = false,

  // ----------------------------- Plan & Reviews-------------------------//

  @Column(name = "CASE_MANAGER")
  var caseManager: String? = null,
  @Column(name = "REASON")
  var reasonForPlan: String? = null,
  @Column(name = "CASE_REV_DATE")
  var firstCaseReviewDate: LocalDate? = null,

  @OneToMany(mappedBy = "csipReport", cascade = [CascadeType.ALL], orphanRemoval = true)
  val plans: MutableList<CSIPPlan> = mutableListOf(),

  @OneToMany(mappedBy = "csipReport", cascade = [CascadeType.ALL], orphanRemoval = true)
  val reviews: MutableList<CSIPReview> = mutableListOf(),

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
