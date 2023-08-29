package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrimaryKeyJoinColumn
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import java.time.LocalDate

@Entity
@Table(name = "COURSE_ACTIVITIES")
data class CourseActivity(
  @Id
  @SequenceGenerator(name = "CRS_ACTY_ID", sequenceName = "CRS_ACTY_ID", allocationSize = 1)
  @GeneratedValue(generator = "CRS_ACTY_ID")
  @Column(name = "CRS_ACTY_ID", nullable = false)
  val courseActivityId: Long = 0,

  @Column
  val code: String? = null,

  @Column
  val caseloadId: String? = null,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val prison: AgencyLocation,

  @Column
  var description: String? = null,

  @Column
  var capacity: Int? = null,

  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = false,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "PROGRAM_ID", nullable = false)
  var program: ProgramService,

  @Column
  var scheduleStartDate: LocalDate,

  @Column
  var scheduleEndDate: LocalDate? = null,

  @OneToMany(mappedBy = "id.courseActivity", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val payRates: MutableList<CourseActivityPayRate> = mutableListOf(),

  @Column
  val caseloadType: String? = "INST",

  @Column(nullable = false)
  val courseClass: String? = "COURSE",

  @Column
  val providerPartyClass: String? = "AGY",

  @Column
  val providerPartyCode: String? = prison.id,

  @Column
  val courseActivityType: String? = "PA",

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + IEPLevel.IEP_LEVEL + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "IEP_LEVEL", referencedColumnName = "code")),
    ],
  )
  var iepLevel: IEPLevel,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "INTERNAL_LOCATION_ID")
  var internalLocation: AgencyInternalLocation?,

  @Column(name = "HOLIDAY_FLAG")
  @Convert(converter = YesNoConverter::class)
  var excludeBankHolidays: Boolean = false, // If the course/activity conforms to national holidays

  @Column
  @Enumerated(STRING)
  var payPerSession: PayPerSession,

  @Column(name = "OUTSIDE_WORK_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  var outsideWork: Boolean = false,

  @OneToMany(mappedBy = "courseActivity", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val offenderProgramProfiles: MutableList<OffenderProgramProfile> = mutableListOf(),

  @OneToMany(mappedBy = "courseActivity", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val courseSchedules: MutableList<CourseSchedule> = mutableListOf(),

  @OneToMany(mappedBy = "courseActivity", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
  val courseScheduleRules: MutableList<CourseScheduleRule> = mutableListOf(),

  @Column
  var commentText: String = "Copied from the DPS activities service",

  @PrimaryKeyJoinColumn
  @OneToOne(mappedBy = "courseActivity", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  var area: CourseActivityArea? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as CourseActivity

    return courseActivityId == other.courseActivityId
  }

  override fun hashCode(): Int = javaClass.hashCode()

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(courseActivityId = $courseActivityId, desc = $description )"
  }
}

enum class PayPerSession { F, H }
