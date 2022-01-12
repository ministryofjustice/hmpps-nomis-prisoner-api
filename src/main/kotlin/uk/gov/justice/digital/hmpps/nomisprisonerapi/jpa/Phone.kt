package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import javax.persistence.Column
import javax.persistence.DiscriminatorColumn
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Inheritance
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "PHONES")
@DiscriminatorColumn(name = "OWNER_CLASS")
@Inheritance
abstract class Phone {
  @Id
  @SequenceGenerator(name = "PHONE_ID", sequenceName = "PHONE_ID", allocationSize = 1)
  @GeneratedValue(generator = "PHONE_ID")
  @Column(name = "PHONE_ID", nullable = false)
  var phoneId: Long? = null

  @Column(name = "PHONE_TYPE")
  var phoneType: String? = null

  @Column(name = "PHONE_NO")
  var phoneNo: String? = null

  @Column(name = "EXT_NO")
  var extNo: String? = null
}
