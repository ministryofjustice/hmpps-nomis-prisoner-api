package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "DBA_USERS")
class AccountDetail(
  @Id
  @Column(name = "USERNAME")
  val username: String = "",

  // TODO switch to enum
  @Column(name = "ACCOUNT_STATUS")
  val status: String = "EXPIRED",

  // n.b. Not mapped all fields
)
