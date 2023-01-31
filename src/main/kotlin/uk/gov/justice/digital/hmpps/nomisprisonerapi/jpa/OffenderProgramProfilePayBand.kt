package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.time.LocalDate

@Entity
@Table(name = "OFFENDER_PRG_PRF_PAY_BANDS")
data class OffenderProgramProfilePayBand(
  @Id
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFF_PRGREF_ID", nullable = false)
  val offenderProgramProfile: OffenderProgramProfile,

  @Id
  @Column(nullable = false)
  val startDate: LocalDate,

  val endDate: LocalDate? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + PayBand.PAY_BAND + "'",
          referencedColumnName = "domain"
        )
      ), JoinColumnOrFormula(column = JoinColumn(name = "PAY_BAND_CODE", referencedColumnName = "code", nullable = true))
    ]
  )
  val payBand: PayBand,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderProgramProfilePayBand
    return offenderProgramProfile.offenderProgramReferenceId == other.offenderProgramProfile.offenderProgramReferenceId &&
      startDate == other.startDate
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String {
    return "OffenderProgramProfilePayBands(offenderProgramProfile=$offenderProgramProfile, startDate=$startDate, endDate=$endDate, payBandCode='$payBand')"
  }
}
