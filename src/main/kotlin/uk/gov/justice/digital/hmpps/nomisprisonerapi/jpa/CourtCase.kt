package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "OFFENDER_CASES")
@EntityOpen
class CourtCase(
  @SequenceGenerator(
    name = "CASE_ID",
    sequenceName = "CASE_ID",
    allocationSize = 1,
  )
  @GeneratedValue(generator = "CASE_ID")
  @Id
  @Column(name = "CASE_ID")
  val id: Long = 0,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "CASE_SEQ", nullable = false)
  val caseSequence: Int = 1,

  // Nullable on DB but no nulls on production
  val beginDate: LocalDate,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val court: AgencyLocation,

  // optional and without referential integrity on DB but no nulls and all reference codes 100% match
  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + LegalCaseType.LEG_CASE_TYP + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "CASE_TYPE", referencedColumnName = "code")),
    ],
  )
  val legalCaseType: LegalCaseType,

  // optional and without referential integrity on DB but no nulls and all reference codes 100% match
  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + CaseStatus.CASE_STS + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "CASE_STATUS", referencedColumnName = "code")),
    ],
  )
  val caseStatus: CaseStatus,

  @Column(name = "CASE_INFO_NUMBER")
  val primaryCaseInfoNumber: String? = null,

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "COMBINED_CASE_ID")
  val combinedCase: CourtCase? = null,

  val statusUpdateComment: String? = null,

  // no matching reference domain
  val statusUpdateReason: String? = null,

  val statusUpdateDate: LocalDate? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "STATUS_UPDATE_STAFF_ID")
  val statusUpdateStaff: Staff? = null,

  // always has a value
  val lidsCaseNumber: Int = 1,

  @Column(name = "NOMLEGALCASEREF")
  val lidsCaseId: Int? = null,

  @Column(name = "NOMLEGALCASEREFTRANSTO")
  val lidsCombinedCaseId: Int? = null,

  @OneToMany(mappedBy = "courtCase", cascade = [CascadeType.ALL], orphanRemoval = true)
  var courtEvents: MutableList<CourtEvent> = mutableListOf(),

  @OneToMany(mappedBy = "courtCase", cascade = [CascadeType.ALL], orphanRemoval = true)
  var sentences: MutableList<OffenderSentence> = mutableListOf(),

  @OneToMany(mappedBy = "courtCase", cascade = [CascadeType.ALL], orphanRemoval = true)
  var offenderCharges: MutableList<OffenderCharge> = mutableListOf(),

  @OneToMany(mappedBy = "id.courtCase", cascade = [CascadeType.ALL], orphanRemoval = true)
  var caseInfoNumbers: MutableList<OffenderCaseIdentifier> = mutableListOf(),

  @Column(name = "CREATE_DATETIME")
  @Generated
  var createDatetime: LocalDateTime = LocalDateTime.now(),
  /* COLUMNS NOT MAPPED
    VICTIM_LIAISON_UNIT - not used
    CASE_INFO_PREFIX - not used
   */

) {

  fun getDpsCaseInfoNumbers(): List<OffenderCaseIdentifier> = caseInfoNumbers.filter { it.isDpsCaseInfoNumber() }

  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CourtCase
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
