package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.YesNoConverter
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.time.LocalDate
import java.util.Objects

@Entity
@Table(name = "AGENCY_LOCATIONS")
@EntityOpen
data class AgencyLocation(
  @Id
  @Column(name = "AGY_LOC_ID")
  val id: String,

  @Column(name = "DESCRIPTION")
  val description: String,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AgencyLocationType.AGY_LOC_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "AGENCY_LOCATION_TYPE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  val type: AgencyLocationType? = null,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = false,

  @Column(name = "DEACTIVATION_DATE")
  val deactivationDate: LocalDate? = null,

  @OneToMany(mappedBy = "agencyLocation", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${AgencyLocationAddress.ADDR_TYPE}'")
  val addresses: MutableList<AgencyLocationAddress> = mutableListOf(),
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AgencyLocation
    return id == other.id
  }

  override fun hashCode(): Int = Objects.hashCode(id)

  companion object {
    const val IN = "IN"
    const val OUT = "OUT"
    const val TRN = "TRN"
  }
}
