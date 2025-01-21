package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.SQLRestriction
import org.springframework.data.annotation.CreatedDate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Ethnicity.Companion.ETHNICITY
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender.Companion.SEX
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NameType.Companion.NAME_TYPE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Title.Companion.TITLE
import java.time.LocalDate

@Entity
@Table(name = "OFFENDERS")
data class Offender(
  @SequenceGenerator(name = "OFFENDER_ID", sequenceName = "OFFENDER_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_ID")
  @Id
  @Column(name = "OFFENDER_ID", nullable = false)
  var id: Long = 0,

  @Column(name = "OFFENDER_ID_DISPLAY", nullable = false)
  val nomsId: String,

  @Column(name = "LAST_NAME", nullable = false)
  val lastName: String,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'$SEX'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "SEX_CODE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val gender: Gender,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'$TITLE'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "TITLE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val title: Title? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'$ETHNICITY'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "RACE_CODE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val ethnicity: Ethnicity? = null,

  // Note that NAME_TYPE is always null in NOMIS, instead ALIAS_NAME_TYPE is used
  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'$NAME_TYPE'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "ALIAS_NAME_TYPE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val nameType: NameType? = null,

  @Column(name = "ID_SOURCE_CODE", nullable = false)
  val idSourceCode: String = "SEQ",

  @Column(name = "CREATE_DATE", nullable = false)
  @CreatedDate
  val createDate: LocalDate = LocalDate.now(),

  @Column(name = "NAME_SEQUENCE")
  val nameSequence: String? = "1234",

  @Column(name = "CASELOAD_TYPE")
  val caseloadType: String? = "INST",

  @Column(name = "FIRST_NAME")
  val firstName: String,

  @Column(name = "MIDDLE_NAME")
  val middleName: String? = null,

  @Column(name = "MIDDLE_NAME_2")
  val middleName2: String? = null,

  @Column(name = "BIRTH_DATE")
  val birthDate: LocalDate? = null,

  @Column(name = "BIRTH_PLACE")
  val birthPlace: String? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = ("'${Country.COUNTRY}'"), referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "BIRTH_COUNTRY_CODE", referencedColumnName = "code")),
    ],
  )
  val birthCountry: Country? = null,

  @Column(name = "ROOT_OFFENDER_ID")
  var rootOffenderId: Long? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "ROOT_OFFENDER_ID", updatable = false, insertable = false)
  var rootOffender: Offender? = null,

  // CAUTION: this list is only populated for the root offender; normally the function getAllBookings() below should be used instead
  @OneToMany(mappedBy = "rootOffender", cascade = [CascadeType.ALL], fetch = LAZY, orphanRemoval = true)
  private val allBookings: MutableList<OffenderBooking> = mutableListOf(),

  @OneToMany(mappedBy = "id.offender", cascade = [CascadeType.ALL], fetch = LAZY)
  val identifiers: MutableList<OffenderIdentifier> = mutableListOf(),

  @OneToMany(mappedBy = "offender", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${OffenderAddress.ADDR_TYPE}'")
  val addresses: MutableList<OffenderAddress> = mutableListOf(),

  @OneToMany(mappedBy = "offender", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${OffenderPhone.PHONE_TYPE}'")
  val phones: MutableList<OffenderPhone> = mutableListOf(),

  @OneToMany(mappedBy = "offender", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${OffenderInternetAddress.TYPE}'")
  val internetAddresses: MutableList<OffenderInternetAddress> = mutableListOf(),

  @Column(name = "LAST_NAME_KEY", nullable = false)
  var lastNameKey: String? = null,

  @Column(name = "LAST_NAME_SOUNDEX")
  val lastNameSoundex: String? = null,

  @Column(name = "LAST_NAME_ALPHA_KEY")
  val lastNameAlphaKey: String? = null,
) {

  fun getAllBookings(): MutableList<OffenderBooking>? = rootOffender?.allBookings

  fun latestBooking(): OffenderBooking =
    getAllBookings()?.firstOrNull { it.bookingSequence == 1 } ?: throw IllegalStateException("Offender has no active bookings")

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Offender
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String =
    "${javaClass.simpleName}(id = $id, nomsId=$nomsId, firstName = $firstName, lastName = $lastName)"
}
