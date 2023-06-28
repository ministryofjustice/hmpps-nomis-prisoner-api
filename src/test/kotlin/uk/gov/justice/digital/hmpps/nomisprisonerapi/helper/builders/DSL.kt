package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.WITNESS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction.Companion.NO_FURTHER_ACTION_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SlotCategory
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
  ): Offender = repository.save(
    OffenderBuilder(nomsId, lastName, firstName, birthDate, genderCode, repository = repository).apply(dsl),
  )

  @ProgramServiceDslMarker
  override fun programService(
    programCode: String,
    programId: Long,
    description: String,
    active: Boolean,
    dsl: ProgramServiceDsl.() -> Unit,
  ): ProgramService =
    repository.save(ProgramServiceBuilder(repository, programCode, programId, description, active).apply(dsl))
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

  @ProgramServiceDslMarker
  fun programService(
    programCode: String = "INTTEST",
    programId: Long = 20,
    description: String = "test program",
    active: Boolean = true,
    dsl: ProgramServiceDsl.() -> Unit = {},
  ): ProgramService
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

  @IncentiveDslMarker
  fun incentive(
    iepLevelCode: String = "ENT",
    userId: String? = null,
    sequence: Long = 1,
    commentText: String = "comment",
    auditModuleName: String? = null,
    iepDateTime: LocalDateTime = LocalDateTime.now(),
  )

  @CourseAllocationDslMarker
  fun courseAllocation(
    courseActivity: CourseActivity,
    startDate: String? = "2022-10-31",
    programStatusCode: String = "ALLOC",
    endDate: String? = null,
    payBands: MutableList<OffenderProgramProfilePayBandBuilder> = mutableListOf(),
    endReasonCode: String? = null,
    endComment: String? = null,
    attendances: MutableList<OffenderCourseAttendanceBuilder> = mutableListOf(),
    dsl: CourseAllocationDsl.() -> Unit = {},
  )
}

@TestDataDslMarker
interface IncentiveDsl

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

  @AdjudicationHearingDslMarker
  fun hearing(
    internalLocationId: Long? = null,
    scheduleDate: LocalDate? = null,
    scheduleTime: LocalDateTime? = null,
    hearingDate: LocalDate? = null,
    hearingTime: LocalDateTime? = null,
    hearingStaffId: Long? = null,
    dsl: AdjudicationHearingDsl.() -> Unit = {},
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
interface AdjudicationHearingDsl {
  @AdjudicationHearingResultDslMarker
  fun result(
    chargeSequence: Int,
    pleaFindingCode: String = "NOT_GUILTY",
    findingCode: String = "PROVED",
    dsl: AdjudicationHearingResultDsl.() -> Unit = {},
  )
}

@TestDataDslMarker
interface ProgramServiceDsl {
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
    courseSchedules: List<CourseScheduleBuilder> = listOf(),
    courseScheduleRules: List<CourseScheduleRuleBuilder> = listOf(),
    courseAllocations: List<OffenderProgramProfileBuilder> = listOf(),
    excludeBankHolidays: Boolean = false,
    dsl: CourseActivityDsl.() -> Unit = {},
  ): CourseActivity
}

@TestDataDslMarker
interface AdjudicationHearingResultDsl

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

  @CourseScheduleDslMarker
  fun courseSchedule(
    courseScheduleId: Long = 0,
    scheduleDate: String = "2022-11-01",
    startTime: String = "08:00",
    endTime: String = "11:00",
    slotCategory: SlotCategory = SlotCategory.AM,
    scheduleStatus: String = "SCH",
  )

  @CourseScheduleRuleDslMarker
  fun courseScheduleRule(
    id: Long = 0,
    startTimeHours: Int = 9,
    startTimeMinutes: Int = 30,
    endTimeHours: Int = 12,
    endTimeMinutes: Int = 30,
    monday: Boolean = true,
    tuesday: Boolean = true,
    wednesday: Boolean = true,
    thursday: Boolean = true,
    friday: Boolean = true,
    saturday: Boolean = false,
    sunday: Boolean = false,
  )
}

@TestDataDslMarker
interface CourseActivityPayRateDsl

@TestDataDslMarker
interface CourseScheduleDsl

@TestDataDslMarker
interface CourseScheduleRuleDsl

@TestDataDslMarker
interface CourseAllocationDsl {
  @CourseAllocationPayBandDslMarker
  fun payBand(
    startDate: String = "2022-10-31",
    endDate: String? = null,
    payBandCode: String = "5",
  )

  @CourseAttendanceDslMarker
  fun courseAttendance(
    courseSchedule: CourseSchedule,
    eventId: Long = 0,
    eventStatusCode: String = "SCH",
    toInternalLocationId: Long? = -8,
    outcomeReasonCode: String? = null,
    paidTransactionId: Long? = null,
  )
}

@TestDataDslMarker
interface CourseAllocationPayBandDsl

@TestDataDslMarker
interface CourseAttendanceDsl

@DslMarker
annotation class TestDataDslMarker

@DslMarker
annotation class StaffDslMarker

@DslMarker
annotation class OffenderDslMarker

@DslMarker
annotation class BookingDslMarker

@DslMarker
annotation class IncentiveDslMarker

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
annotation class AdjudicationHearingDslMarker

@DslMarker
annotation class AdjudicationHearingResultDslMarker

@DslMarker
annotation class ProgramServiceDslMarker

@DslMarker
annotation class CourseActivityDslMarker

@DslMarker
annotation class CourseActivityPayRateDslMarker

@DslMarker
annotation class CourseScheduleDslMarker

@DslMarker
annotation class CourseScheduleRuleDslMarker

@DslMarker
annotation class CourseAllocationDslMarker

@DslMarker
annotation class CourseAllocationPayBandDslMarker

@DslMarker
annotation class CourseAttendanceDslMarker
