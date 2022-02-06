package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.Column
import javax.persistence.DiscriminatorColumn
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.Table

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
