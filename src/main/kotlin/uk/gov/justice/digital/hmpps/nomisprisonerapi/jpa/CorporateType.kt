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

@Embeddable
class CorporateOrganisationTypePK(
  @JoinColumn(name = "CORPORATE_ID", nullable = false)
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val corporate: Corporate,

  @Column(name = "CORPORATE_TYPE", nullable = false)
  var typeCode: String,
) : Serializable

@Entity
@Table(name = "CORPORATE_TYPES")
class CorporateType(
  @EmbeddedId
  val id: CorporateOrganisationTypePK,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'${CorporateOrganisationType.CORP_TYPE}'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "CORPORATE_TYPE", referencedColumnName = "code", nullable = false, insertable = false, updatable = false)),
    ],
  )
  val type: CorporateOrganisationType,

) : NomisAuditableEntity() {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CorporateType

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
