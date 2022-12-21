package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.DiscriminatorColumn
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Inheritance
import jakarta.persistence.Table

@Entity
@Table(name = "INTERNET_ADDRESSES")
@DiscriminatorColumn(name = "OWNER_CLASS")
@Inheritance
abstract class InternetAddress {
  @Id
  @Column(name = "INTERNET_ADDRESS_ID", nullable = false)
  open val internetAddressId: Long = 0

  @Column(name = "INTERNET_ADDRESS_CLASS")
  open val internetAddressClass: String? = null

  @Column(name = "INTERNET_ADDRESS")
  open val internetAddress: String? = null
}
