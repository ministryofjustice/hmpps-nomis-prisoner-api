package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.YesNoConverter
import java.time.LocalDate

@Entity
@Table(name = "PERSONS")
data class Person(
  @Id
  @Column(name = "PERSON_ID")
  @GeneratedValue(generator = "PERSON_ID")
  @SequenceOrUseId(name = "PERSON_ID")
  var id: Long = 0,

  @Column(name = "FIRST_NAME", nullable = false)
  val firstName: String,

  @Column(name = "LAST_NAME", nullable = false)
  val lastName: String,

  @Column(name = "MIDDLE_NAME")
  val middleName: String? = null,

  @Column(name = "BIRTHDATE")
  val birthDate: LocalDate? = null,

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${PersonAddress.ADDR_TYPE}'")
  val addresses: MutableList<PersonAddress> = mutableListOf(),

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${PersonPhone.PHONE_TYPE}'")
  val phones: MutableList<PersonPhone> = mutableListOf(),

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = LAZY)
  @SQLRestriction("OWNER_CLASS = '${PersonInternetAddress.TYPE}'")
  val internetAddresses: MutableList<PersonInternetAddress> = mutableListOf(),

  @OneToMany(mappedBy = "id.person", cascade = [CascadeType.ALL], fetch = LAZY)
  val employments: MutableList<PersonEmployment> = mutableListOf(),

  @OneToMany(mappedBy = "id.person", cascade = [CascadeType.ALL], fetch = LAZY)
  val identifiers: MutableList<PersonIdentifier> = mutableListOf(),

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = LAZY)
  val contacts: MutableList<OffenderContactPerson> = mutableListOf(),

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = LAZY)
  val restrictions: MutableList<VisitorRestriction> = mutableListOf(),

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + Title.TITLE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "TITLE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val title: Title? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + Gender.SEX + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "SEX", referencedColumnName = "code", nullable = true)),
    ],
  )
  val sex: Gender? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + Language.LANG + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "LANGUAGE_CODE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val language: Language? = null,

  @Column(name = "INTERPRETER_REQUIRED")
  @Convert(converter = YesNoConverter::class)
  val interpreterRequired: Boolean = false,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + MaritalStatus.MARITAL_STAT + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "MARITAL_STATUS", referencedColumnName = "code", nullable = true)),
    ],
  )
  val domesticStatus: MaritalStatus? = null,

  @Column(name = "DECEASED_DATE")
  val deceasedDate: LocalDate? = null,

  @Column(name = "STAFF_FLAG")
  @Convert(converter = YesNoConverter::class)
  val isStaff: Boolean? = false,

  @Column(name = "REMITTER_FLAG")
  @Convert(converter = YesNoConverter::class)
  val isRemitter: Boolean? = null,

  /* columns not mapped
  OCCUPATION_CODE - always null
  CRIMINAL_HISTORY_TEXT - always null
  NAME_TYPE - always null
  ALIAS_PERSON_ID - always null
  ROOT_PERSON_ID - always null
  COMPREHEND_ENGLISH_FLAG - always default of N
  BIRTH_PLACE - always null
  EMPLOYER - always null
  PROFILE_CODE - always null
  PRIMARY_LANGUAGE_CODE - always null
  MEMO_TEXT - always null
  SUSPENDED_FLAG - always default of N
  CITIZENSHIP - always null
  CORONER_NUMBER - always null
  ATTENTION - always null
  CARE_OF - always null
  SUSPENDED_DATE - always null
  NAME_SEQUENCE - always null
  KEEP_BIOMETRICS - not used in NOMIS
   */

) : NomisAuditableEntity() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Person

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $id )"
}
