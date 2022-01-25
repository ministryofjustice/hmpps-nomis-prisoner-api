package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.hibernate.Hibernate
import org.hibernate.annotations.Where
import java.time.LocalDate
import java.util.stream.Collectors
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "PERSONS")
data class Person(
  @Id
  @Column(name = "PERSON_ID")
  @SequenceGenerator(name = "PERSON_ID", sequenceName = "PERSON_ID", allocationSize = 1)
  @GeneratedValue(generator = "PERSON_ID")
  var id: Long = 0,

  @Column(name = "FIRST_NAME", nullable = false)
  val firstName: String,

  @Column(name = "LAST_NAME", nullable = false)
  val lastName: String,

  @Column(name = "MIDDLE_NAME")
  val middleName: String? = null,

  @Column(name = "BIRTHDATE")
  val birthDate: LocalDate? = null,

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @Where(clause = "OWNER_CLASS = '" + PersonAddress.ADDR_TYPE + "'")
  val addresses: List<PersonAddress> = ArrayList(),

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @Where(clause = "OWNER_CLASS = '" + PersonPhone.PHONE_TYPE + "'")
  val phones: List<PersonPhone> = ArrayList(),

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @Where(clause = "OWNER_CLASS = '" + PersonInternetAddress.TYPE + "'")
  val internetAddresses: List<PersonInternetAddress> = ArrayList(),
) {
  fun getEmails(): List<PersonInternetAddress> {
    return internetAddresses.stream().filter { ia: PersonInternetAddress -> "EMAIL" == ia.internetAddressClass }
      .collect(Collectors.toList())
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Person

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(id = $id )"
  }
}
