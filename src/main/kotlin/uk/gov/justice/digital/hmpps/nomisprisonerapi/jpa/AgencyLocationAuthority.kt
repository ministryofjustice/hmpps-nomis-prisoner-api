package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen
import java.io.Serializable

@Embeddable
data class AgencyLocationAuthorityId(

  @ManyToOne(optional = false, fetch = LAZY)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "AGY_LOC_ID",
        referencedColumnName = "AGY_LOC_ID",
        insertable = false,
        updatable = false,
      ),
    ],
  )
  val agencyLocation: AgencyLocation,

  @Column(name = "LOCAL_AUTHORITY_CODE")
  val localAuthorityCode: String,

) : Serializable

@Entity
@Table(name = "AGENCY_LOCATION_AUTHORITIES")
@EntityOpen
class AgencyLocationAuthority(
  @Id
  var id: AgencyLocationAuthorityId,

) : NomisAuditableEntityBasic() {
  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + LocalAuthorityType.LOCAL_AUTH + "'",
          referencedColumnName = "domain",

        ),

      ), JoinColumnOrFormula(
        column = JoinColumn(
          name = "LOCAL_AUTHORITY_CODE",
          updatable = false,
          insertable = false,
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  lateinit var authority: LocalAuthorityType

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AgencyLocationAuthority

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
