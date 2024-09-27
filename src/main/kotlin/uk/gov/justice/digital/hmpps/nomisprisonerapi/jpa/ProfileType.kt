package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import org.hibernate.Hibernate
import org.hibernate.annotations.DiscriminatorFormula

@Entity(name = "PROFILE_TYPES")
@DiscriminatorFormula("CODE_VALUE_TYPE")
@Inheritance
abstract class ProfileType(
  @Id
  @Column(name = "PROFILE_TYPE")
  open val type: String,

  @Column(name = "PROFILE_CATEGORY")
  open val category: String? = null,

  @Column
  open val description: String,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ProfileType

    return type == other.type
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String = this::class.simpleName + "(profileType = $type)"
}
