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

  /* data that new service stores:
  {
    "prisonCode": "PVI",
    "attendanceRequired": false, ----
    "summary": "Maths level 1", ---
    "description": "A basic maths course suitable for introduction to the subject",
    "categoryId": 0,  ---
    "tierId": 1,      ---
    "eligibilityRuleIds": [  ---- course_activity_profiles ?
      1,
      2,
      3
    ],
    "pay": [ ---
      {
        "incentiveLevel": "Basic",
        "payBand": "A",
        "rate": 150,
        "pieceRate": 150,
        "pieceRateItems": 10
      }
    ],
    "riskLevel": "High", ----
    "minimumIncentiveLevel": "Basic",
    "startDate": "2022-12-23",
    "endDate": "2022-12-23"
  }
     */
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
  val description: String? = null,

  @Column
  val capacity: Int? = null,

  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  val active: Boolean = false,

  @ManyToOne(optional = false)
  @JoinColumn(name = "PROGRAM_ID", nullable = false)
  val program: ProgramService,

  @Column
  val scheduleStartDate: LocalDate? = null,

  @Column
  val scheduleEndDate: LocalDate? = null,

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
          referencedColumnName = "domain"
        )
      ),
      JoinColumnOrFormula(column = JoinColumn(name = "IEP_LEVEL", referencedColumnName = "code"))
    ]
  )
  val iepLevel: IEPLevel,

  @ManyToOne(optional = false)
  @JoinColumn(name = "INTERNAL_LOCATION_ID", nullable = false)
  var internalLocation: AgencyInternalLocation,

  @Column(name = "HOLIDAY_FLAG")
  @Convert(converter = YesNoConverter::class)
  val holiday: Boolean = false, // If the course/activity conforms to national holidays

  @Column
  @Enumerated(STRING)
  val payPerSession: PayPerSession? = null,

  @OneToMany(mappedBy = "courseActivity", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
  val offenderProgramProfiles: MutableList<OffenderProgramProfile> = mutableListOf(),
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

/*
other cols, All nullable:

EXPIRY_DATE	DATE	                    		7	Date the Course/Activity Date became inactive - Retrofitted
SERVICES_ADDRESS_ID	NUMBER(10,0)	            		11	The address where the services(course/activity) takes place
PARENT_CRS_ACTY_ID	NUMBER(10,0)	            		13	The parent of course activity ID
PROVIDER_PARTY_ID	NUMBER(10,0)	            		16	The party id of the provider
BENEFICIARY_NAME	VARCHAR2(80 CHAR)	            		18	The name of the beneficiary
BENEFICIARY_CONTACT	VARCHAR2(80 CHAR)	            		19	The contact details of the beneficiary
LIST_SEQ	NUMBER(6,0)	                        		20	The order of the listing
PLACEMENT_CORPORATE_ID	NUMBER(10,0)	            		21	The corporate of the placement
COMMENT_TEXT	VARCHAR2(240 CHAR)	            		22	The general comment text
AGENCY_LOCATION_TYPE	VARCHAR2(12 CHAR)	            		23	The agency location type
PROVIDER_TYPE	VARCHAR2(12 CHAR)	            		24	The Provider Type
BENEFICIARY_TYPE	VARCHAR2(12 CHAR)	            		25	The beneficiary type.  Reference Code(PS_BENEF)
PLACEMENT_TEXT	VARCHAR2(240 CHAR)	            		26	The general text of the placement
 */
