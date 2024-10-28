package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table

@Entity
@Table(name = "INTERNET_ADDRESSES")
@DiscriminatorColumn(name = "OWNER_CLASS")
@Inheritance
abstract class InternetAddress(
  @Column(name = "INTERNET_ADDRESS")
  open val internetAddress: String,

  @Column(name = "INTERNET_ADDRESS_CLASS")
  open val internetAddressClass: String,
) : NomisAuditableEntity() {
  @Id
  @SequenceGenerator(name = "INTERNET_ADDRESS_ID", sequenceName = "INTERNET_ADDRESS_ID", allocationSize = 1)
  @GeneratedValue(generator = "INTERNET_ADDRESS_ID")
  @Column(name = "INTERNET_ADDRESS_ID", nullable = false)
  val internetAddressId: Long = 0
}
