package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.Table
import org.hibernate.Hibernate

@Entity
@Table(name = "V_ADDRESSES")
@Inheritance
class OffenderMovementAddress(
  @Id
  val addressId: Long,

  val ownerClass: String,

  val commentText: String? = null,

  val house: String? = null,

  val street: String? = null,

  val locality: String? = null,

  val cityName: String? = null,

  val county: String? = null,

  val country: String? = null,

  val postalCode: String? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OffenderMovementAddress
    return addressId == other.addressId
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
