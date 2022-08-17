package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Embeddable
data class IncentiveId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID", nullable = false)
  val offenderBooking: OffenderBooking,

  @Column(name = "IEP_LEVEL_SEQ", nullable = false)
  val sequence: Long
) : Serializable

@Entity
@Table(name = "OFFENDER_IEP_LEVELS")
data class Incentive(
  @EmbeddedId
  val id: IncentiveId,

  @Column
  var commentText: String? = null,

  @Column(nullable = false)
  val iepDate: LocalDate,

  @Column(nullable = false)
  val iepTime: LocalTime,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val location: AgencyLocation,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + IEPLevel.IEP_LEVEL + "'",
          referencedColumnName = "domain"
        )
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "IEP_LEVEL", referencedColumnName = "code"))
    ]
  )
  val iepLevel: IEPLevel,

) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Incentive
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
