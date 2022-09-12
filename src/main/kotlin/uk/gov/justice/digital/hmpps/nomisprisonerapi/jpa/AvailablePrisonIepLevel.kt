package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.Type
import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id
import javax.persistence.IdClass
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@IdClass(AvailablePrisonIepLevel.Companion.PK::class)
@Table(name = "IEP_LEVELS")
@BatchSize(size = 25)
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
  @Type(type = "yes_no")
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
