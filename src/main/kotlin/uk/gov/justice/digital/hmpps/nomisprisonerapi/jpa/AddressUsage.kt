package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
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
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.type.YesNoConverter
import java.io.Serializable

@Embeddable
class AddressUsageId(
  @JoinColumn(name = "ADDRESS_ID", nullable = false)
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  val address: Address,

  @Column(name = "ADDRESS_USAGE", nullable = false)
  var usageCode: String,
) : Serializable

@Entity
@Table(name = "ADDRESS_USAGES")
class AddressUsage(
  @EmbeddedId
  val id: AddressUsageId,

  @Column(name = "ACTIVE_FLAG")
  @Convert(converter = YesNoConverter::class)
  val active: Boolean,

  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'${AddressUsageType.ADDRESS_TYPE}'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "ADDRESS_USAGE", referencedColumnName = "code", nullable = false, insertable = false, updatable = false)),
    ],
  )
  val addressUsage: AddressUsageType?,
) : Serializable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AddressUsage

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
