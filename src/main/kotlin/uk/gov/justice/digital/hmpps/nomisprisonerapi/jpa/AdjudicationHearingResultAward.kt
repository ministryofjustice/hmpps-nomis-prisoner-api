package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Embeddable
class AdjudicationHearingResultAwardId(
  @Column(name = "OFFENDER_BOOK_ID", nullable = false)
  var offenderBookId: Long,

  @Column(name = "SANCTION_SEQ", nullable = false)
  var sanctionSequence: Int,
) : Serializable

@Entity
@Table(name = "OFFENDER_OIC_SANCTIONS")
class AdjudicationHearingResultAward(

  @EmbeddedId
  val id: AdjudicationHearingResultAwardId,

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "CONSECUTIVE_OFFENDER_BOOK_ID",
        referencedColumnName = "OFFENDER_BOOK_ID",
      ),
      JoinColumn(
        name = "CONSECUTIVE_SANCTION_SEQ",
        referencedColumnName = "SANCTION_SEQ",
      ),
    ],
  )
  var consecutiveHearingResultAward: AdjudicationHearingResultAward? = null,

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "OIC_HEARING_ID",
        referencedColumnName = "OIC_HEARING_ID",
        nullable = false,
      ),
      JoinColumn(
        name = "RESULT_SEQ",
        referencedColumnName = "RESULT_SEQ",
        nullable = false,
      ),
    ],
  )
  val hearingResult: AdjudicationHearingResult?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
    name = "OIC_INCIDENT_ID",
    referencedColumnName = "OIC_INCIDENT_ID",
    nullable = false,
    insertable = false,
    updatable = false,
  )
  val incidentParty: AdjudicationIncidentParty,

  @Column(name = "EFFECTIVE_DATE")
  val effectiveDate: LocalDate,

  @Column(name = "STATUS_DATE", nullable = false)
  val statusDate: LocalDate? = LocalDate.now(),

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @Column(name = "SANCTION_DAYS")
  val sanctionDays: Int? = null,

  @Column(name = "SANCTION_MONTHS")
  val sanctionMonths: Int? = null,

  @Column(name = "COMPENSATION_AMOUNT")
  val compensationAmount: BigDecimal? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE) // one code of "LOR" not on reference table
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AdjudicationSanctionStatus.OIC_SANCT_ST + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "STATUS",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  val sanctionStatus: AdjudicationSanctionStatus?,

  @Column(
    name = "OIC_SANCTION_CODE",
    updatable = false,
    insertable = false,
  ) // need to map for when type is not one of the reference data codes
  val sanctionCode: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AdjudicationSanctionType.OIC_SANCT + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "OIC_SANCTION_CODE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  val sanctionType: AdjudicationSanctionType? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationHearingResultAward
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
