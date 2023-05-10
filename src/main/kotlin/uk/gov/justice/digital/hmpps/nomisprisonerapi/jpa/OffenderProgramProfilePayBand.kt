package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.io.Serializable
import java.time.LocalDate

@Embeddable
data class OffenderProgramProfilePayBandId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFF_PRGREF_ID", nullable = false)
  val offenderProgramProfile: OffenderProgramProfile,

  @Column(nullable = false)
  val startDate: LocalDate,
) : Serializable

@Entity
@Table(name = "OFFENDER_PRG_PRF_PAY_BANDS")
data class OffenderProgramProfilePayBand(

  @EmbeddedId
  val id: OffenderProgramProfilePayBandId,

  var endDate: LocalDate? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + PayBand.PAY_BAND + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "PAY_BAND_CODE", referencedColumnName = "code", nullable = true)),
    ],
  )
  var payBand: PayBand,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderProgramProfilePayBand
    return id.offenderProgramProfile.offenderProgramReferenceId == other.id.offenderProgramProfile.offenderProgramReferenceId &&
      id.startDate == other.id.startDate
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String {
    return "OffenderProgramProfilePayBands(offenderProgramProfile=${id.offenderProgramProfile.offenderProgramReferenceId}, startDate=$id.startDate, endDate=$endDate, payBandCode='$payBand')"
  }
}
