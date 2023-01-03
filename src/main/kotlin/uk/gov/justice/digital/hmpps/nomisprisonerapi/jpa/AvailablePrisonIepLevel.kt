package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import java.io.Serializable

@Entity
@IdClass(AvailablePrisonIepLevel.Companion.PK::class)
@Table(name = "IEP_LEVELS")
data class AvailablePrisonIepLevel(

  @Id
  @Column(name = "IEP_LEVEL")
  val id: String,

  @Id
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val agencyLocation: AgencyLocation,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + IEPLevel.IEP_LEVEL + "'",
          referencedColumnName = "domain"
        )
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "IEP_LEVEL",
          referencedColumnName = "code",
          updatable = false,
          insertable = false
        )
      )
    ]
  )
  val iepLevel: IEPLevel,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = false,
) : Serializable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AvailablePrisonIepLevel
    return iepLevel == other.iepLevel && agencyLocation.id == other.agencyLocation.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  companion object {
    class PK(
      private val id: String? = null,
      private val agencyLocation: AgencyLocation? = null,
    ) : Serializable
  }
}
