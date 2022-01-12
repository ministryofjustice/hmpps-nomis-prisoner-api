package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import org.apache.commons.lang3.StringUtils
import org.hibernate.Hibernate
import org.hibernate.annotations.BatchSize
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.springframework.data.annotation.CreatedDate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender.Companion.SEX
import java.time.LocalDate
import java.util.Objects
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.SequenceGenerator
import javax.persistence.Table

@Entity
@Table(name = "OFFENDERS")
@BatchSize(size = 25)
data class Offender(
  @SequenceGenerator(name = "OFFENDER_ID", sequenceName = "OFFENDER_ID", allocationSize = 1)
  @GeneratedValue(generator = "OFFENDER_ID")
  @Id
  @Column(name = "OFFENDER_ID", nullable = false)
  val id: Long,

  @Column(name = "OFFENDER_ID_DISPLAY", nullable = false)
  val nomsId: String,

  @Column(name = "LAST_NAME", nullable = false)
  val lastName: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(formula = JoinFormula(value = "'$SEX'", referencedColumnName = "domain")),
      JoinColumnOrFormula(column = JoinColumn(name = "SEX_CODE", referencedColumnName = "code", nullable = false))
    ]
  )
  val gender: Gender,

  @Column(name = "ID_SOURCE_CODE", nullable = false)
  val idSourceCode: String = "SEQ",

  @Column(name = "CREATE_DATE", nullable = false)
  @CreatedDate
  val createDate: LocalDate = LocalDate.now(),

  @Column(name = "NAME_SEQUENCE")
  val nameSequence: String? = "1234",

  @Column(name = "CASELOAD_TYPE")
  val caseloadType: String? = "INST",

  @Column(name = "FIRST_NAME", nullable = false)
  val firstName: String? = null,

  @Column(name = "MIDDLE_NAME")
  val middleName: String? = null,

  @Column(name = "MIDDLE_NAME_2")
  val middleName2: String? = null,

  @Column(name = "BIRTH_DATE", nullable = false)
  val birthDate: LocalDate? = null,

  @Column(name = "ROOT_OFFENDER_ID")
  val rootOffenderId: Long? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ROOT_OFFENDER_ID", updatable = false, insertable = false)
  val rootOffender: Offender? = null,

  @OneToMany(mappedBy = "rootOffender", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  @BatchSize(size = 25)
  val bookings: List<OffenderBooking> = emptyList(),

  @OneToMany(mappedBy = "offender", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val identifiers: List<OffenderIdentifier> = ArrayList(),

  //    @ManyToOne(fetch = FetchType.LAZY)
  //    @JoinColumnsOrFormulas(value = {
  //        @JoinColumnOrFormula(formula = @JoinFormula(value = "'" + ETHNICITY + "'", referencedColumnName = "domain")),
  //        @JoinColumnOrFormula(column = @JoinColumn(name = "RACE_CODE", referencedColumnName = "code"))
  //    })
  //    private Ethnicity ethnicity;
  //
  //    @ManyToOne(fetch = FetchType.LAZY)
  //    @JoinColumnsOrFormulas(value = {
  //        @JoinColumnOrFormula(formula = @JoinFormula(value = "'" + TITLE + "'", referencedColumnName = "domain")),
  //        @JoinColumnOrFormula(column = @JoinColumn(name = "TITLE", referencedColumnName = "code"))
  //    })
  //    private Title title;
  //
  //    @ManyToOne(fetch = FetchType.LAZY)
  //    @JoinColumnsOrFormulas(value = {
  //        @JoinColumnOrFormula(formula = @JoinFormula(value = "'" + SUFFIX + "'", referencedColumnName = "domain")),
  //        @JoinColumnOrFormula(column = @JoinColumn(name = "SUFFIX", referencedColumnName = "code"))
  //    })
  //    private Suffix suffix;

  @Column(name = "LAST_NAME_KEY", nullable = false)
  var lastNameKey: String? = null,

  @Column(name = "LAST_NAME_SOUNDEX")
  val lastNameSoundex: String? = null,

  @Column(name = "LAST_NAME_ALPHA_KEY")
  val lastNameAlphaKey: String? = null,

  //    @OneToMany(mappedBy = "offender", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  //    @Where(clause = "OWNER_CLASS = '"+OffenderAddress.ADDR_TYPE+"'")
  //    private List<OffenderAddress> addresses = new ArrayList<>();
) {
  val middleNames: String
    get() = StringUtils.trimToNull(StringUtils.trimToEmpty(middleName) + " " + StringUtils.trimToEmpty(middleName2))

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Offender
    return id == other.id
  }

  override fun hashCode(): Int {
    return Objects.hashCode(id)
  }
}
