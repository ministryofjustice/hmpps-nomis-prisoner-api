package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.SQLRestriction
import java.time.LocalDate
import java.util.stream.Collectors

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
  @SQLRestriction("OWNER_CLASS = '${PersonAddress.ADDR_TYPE}'")
  val addresses: MutableList<PersonAddress> = mutableListOf(),

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @SQLRestriction("OWNER_CLASS = '${PersonPhone.PHONE_TYPE}'")
  val phones: MutableList<PersonPhone> = mutableListOf(),

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  @SQLRestriction("OWNER_CLASS = '${PersonInternetAddress.TYPE}'")
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
