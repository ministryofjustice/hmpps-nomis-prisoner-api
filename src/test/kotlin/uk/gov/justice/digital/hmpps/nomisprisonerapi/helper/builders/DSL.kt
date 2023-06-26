package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.WITNESS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction.Companion.NO_FURTHER_ACTION_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.forceControllingOfficerRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.reportingOfficerRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.suspectRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.victimRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.witnessRole
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

fun testData(repository: Repository, dsl: TestData.() -> Unit) = TestData(repository).apply(dsl)

class TestData(private val repository: Repository) : TestDataDsl {
  @StaffDslMarker
  override fun staff(firstName: String, lastName: String, dsl: StaffDsl.() -> Unit): Staff =
    repository.save(StaffBuilder(firstName, lastName).apply(dsl))

  @AdjudicationIncidentDslMarker
  override fun adjudicationIncident(
    incidentDetails: String,
    reportedDateTime: LocalDateTime,
    reportedDate: LocalDate,
    incidentDateTime: LocalDateTime,
    incidentDate: LocalDate,
    prisonId: String,
    agencyInternalLocationId: Long,
    reportingStaff: Staff,
    dsl: AdjudicationIncidentDsl.() -> Unit,
  ): AdjudicationIncident = repository.save(
    AdjudicationIncidentBuilder(
      incidentDetails = incidentDetails,
      reportedDateTime = reportedDateTime,
      reportedDate = reportedDate,
      incidentDateTime = incidentDateTime,
      incidentDate = incidentDate,
      prisonId = prisonId,
      agencyInternalLocationId = agencyInternalLocationId,
      reportingStaff = reportingStaff,
    ).apply(dsl),
  )

  @OffenderDslMarker
  override fun offender(
    nomsId: String,
    lastName: String,
    firstName: String,
    birthDate: LocalDate,
    genderCode: String,
    dsl: OffenderDsl.() -> Unit,
  ): Offender = repository.save(OffenderBuilder(nomsId, lastName, firstName, birthDate, genderCode).apply(dsl))

  @CourseActivityDslMarker
  override fun courseActivity(
    courseActivityId: Long,
    code: String,
    programId: Long,
    prisonId: String,
    description: String,
    capacity: Int,
    active: Boolean,
    startDate: String,
    endDate: String?,
    minimumIncentiveLevelCode: String,
    internalLocationId: Long?,
    payRates: List<CourseActivityPayRateBuilder>,
    courseSchedules: List<CourseScheduleBuilder>,
    courseScheduleRules: List<CourseScheduleRuleBuilder>,
    excludeBankHolidays: Boolean,
    dsl: CourseActivityDsl.() -> Unit,
  ): CourseActivity =
    repository.save(
      CourseActivityBuilder(
        repository,
        courseActivityId,
        code,
        programId,
        prisonId,
        description,
        capacity,
        active,
        startDate,
        endDate,
        minimumIncentiveLevelCode,
        internalLocationId,
        payRates,
        courseSchedules,
        courseScheduleRules,
        excludeBankHolidays,
      ).apply(dsl),
    )
}

@TestDataDslMarker
interface TestDataDsl {
  @StaffDslMarker
  fun staff(firstName: String, lastName: String, dsl: StaffDsl.() -> Unit = {}): Staff

  @AdjudicationIncidentDslMarker
  fun adjudicationIncident(
    incidentDetails: String = "Big fight",
    reportedDateTime: LocalDateTime = LocalDateTime.now(),
    reportedDate: LocalDate = LocalDate.now(),
    incidentDateTime: LocalDateTime = LocalDateTime.now(),
    incidentDate: LocalDate = LocalDate.now(),
    prisonId: String = "MDI",
    agencyInternalLocationId: Long = -41,
    reportingStaff: Staff,
    dsl: AdjudicationIncidentDsl.() -> Unit = {},
  ): AdjudicationIncident

  @OffenderDslMarker
  fun offender(
    nomsId: String = "A5194DY",
    lastName: String = "NTHANDA",
    firstName: String = "LEKAN",
    birthDate: LocalDate = LocalDate.of(1965, 7, 19),
    genderCode: String = "M",
    dsl: OffenderDsl.() -> Unit = {},
  ): Offender

  @CourseActivityDslMarker
  fun courseActivity(
    courseActivityId: Long = 0,
    code: String = "CA",
    programId: Long = 20,
    prisonId: String = "LEI",
    description: String = "test course activity",
    capacity: Int = 23,
    active: Boolean = true,
    startDate: String = "2022-10-31",
    endDate: String? = null,
    minimumIncentiveLevelCode: String = "STD",
    internalLocationId: Long? = -8,
    payRates: List<CourseActivityPayRateBuilder> = listOf(),
    courseSchedules: List<CourseScheduleBuilder> = listOf(CourseScheduleBuilder()),
    courseScheduleRules: List<CourseScheduleRuleBuilder> = listOf(CourseScheduleRuleBuilder()),
    excludeBankHolidays: Boolean = false,
    dsl: CourseActivityDsl.() -> Unit = {},
  ): CourseActivity
}

@TestDataDslMarker
interface StaffDsl

@TestDataDslMarker
interface OffenderDsl {
  @BookingDslMarker
  fun booking(
    bookingBeginDate: LocalDateTime = LocalDateTime.now(),
    active: Boolean = true,
    inOutStatus: String = "IN",
    youthAdultCode: String = "N",
    visitBalanceBuilder: VisitBalanceBuilder? = null,
    agencyLocationId: String = "BXI",
    dsl: BookingDsl.() -> Unit = {},
  )
}

@TestDataDslMarker
interface BookingDsl {
  @AdjudicationPartyDslMarker
  fun adjudicationParty(
    incident: AdjudicationIncident,
    adjudicationNumber: Long = 1224,
    comment: String = "party comment",
    partyAddedDate: LocalDate = LocalDate.of(2023, 5, 10),
    dsl: AdjudicationPartyDsl.() -> Unit = {},
  )
}

@TestDataDslMarker
interface AdjudicationIncidentDsl {
  @AdjudicationRepairDslMarker
  fun repair(
    repairType: String = "CLEA",
    comment: String? = null,
    repairCost: BigDecimal? = null,
    dsl: AdjudicationRepairDsl.() -> Unit = {},
  )

  @AdjudicationPartyDslMarker
  fun party(
    comment: String = "They witnessed everything",
    role: PartyRole = WITNESS,
    partyAddedDate: LocalDate = LocalDate.of(2023, 5, 10),
    offenderBooking: OffenderBooking? = null,
    staff: Staff? = null,
    adjudicationNumber: Long? = null,
    actionDecision: String = NO_FURTHER_ACTION_CODE,
    dsl: AdjudicationPartyDsl.() -> Unit = {},
  )
}
enum class PartyRole(val code: String) {

  WITNESS(witnessRole),
  VICTIM(victimRole),
  SUSPECT(suspectRole),
  STAFF_CONTROL(forceControllingOfficerRole),
  STAFF_REPORTING_OFFICER(reportingOfficerRole),
}

@TestDataDslMarker
interface AdjudicationPartyDsl {
  @AdjudicationInvestigationDslMarker
  fun investigation(
    investigator: Staff,
    comment: String? = null,
    assignedDate: LocalDate = LocalDate.now(),
    dsl: AdjudicationInvestigationDsl.() -> Unit = {},
  )

  @AdjudicationChargeDslMarker
  fun charge(
    offenceCode: String = "51:1B",
    guiltyEvidence: String? = null,
    reportDetail: String? = null,
    dsl: AdjudicationChargeDsl.() -> Unit = {},
  )
}

@TestDataDslMarker
interface AdjudicationRepairDsl

@TestDataDslMarker
interface AdjudicationInvestigationDsl {
  @AdjudicationEvidenceDslMarker
  fun evidence(
    detail: String = "Knife found",
    type: String = "WEAP",
    date: LocalDate = LocalDate.now(),
    dsl: AdjudicationEvidenceDsl.() -> Unit = {},
  )
}

@TestDataDslMarker
interface AdjudicationEvidenceDsl

@TestDataDslMarker
interface AdjudicationChargeDsl

@TestDataDslMarker
interface CourseActivityDsl {
  @CourseActivityPayRateDslMarker
  fun payRate(
    iepLevelCode: String = "STD",
    payBandCode: String = "5",
    startDate: String = "2022-10-31",
    endDate: String? = null,
    halfDayRate: Double = 3.2,
  )
}

@TestDataDslMarker
interface CourseActivityPayRateDsl

@DslMarker
annotation class TestDataDslMarker

@DslMarker
annotation class StaffDslMarker

@DslMarker
annotation class OffenderDslMarker

@DslMarker
annotation class BookingDslMarker

@DslMarker
annotation class AdjudicationIncidentDslMarker

@DslMarker
annotation class AdjudicationRepairDslMarker

@DslMarker
annotation class AdjudicationPartyDslMarker

@DslMarker
annotation class AdjudicationChargeDslMarker

@DslMarker
annotation class AdjudicationInvestigationDslMarker

@DslMarker
annotation class AdjudicationEvidenceDslMarker

@DslMarker
annotation class CourseActivityDslMarker

@DslMarker
annotation class CourseActivityPayRateDslMarker
