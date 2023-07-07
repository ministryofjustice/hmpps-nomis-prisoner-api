package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.WITNESS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction.Companion.NO_FURTHER_ACTION_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction.Companion.PLACED_ON_REPORT_ACTION_CODE
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.forceControllingOfficerRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.reportingOfficerRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.suspectRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.victimRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.witnessRole
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

fun testData(repository: Repository, dsl: NomisData.() -> Unit) = NomisData(repository).apply(dsl)

@Component
class NomisDataBuilder(
  private val repository: Repository? = null,
  private val programServiceBuilderFactory: ProgramServiceBuilderFactory? = ProgramServiceBuilderFactory(),
  private val offenderBuilderFactory: NewOffenderBuilderFactory? = null, // note this means the offender DSL is not available in unit tests whereas programService is.
) {
  fun build(dsl: NomisData.() -> Unit) = NomisData(repository, programServiceBuilderFactory, offenderBuilderFactory).apply(dsl)
}

class NomisData(
  private val repository: Repository? = null,
  private val programServiceBuilderFactory: ProgramServiceBuilderFactory? = null,
  private val offenderBuilderFactory: NewOffenderBuilderFactory? = null,
) : NomisDataDsl {
  @StaffDslMarker
  override fun staff(firstName: String, lastName: String, dsl: StaffDsl.() -> Unit): Staff =
    repository!!.save(StaffBuilder(firstName, lastName).apply(dsl))

  @AdjudicationIncidentDslMarker
  override fun adjudicationIncident(
    whenCreated: LocalDateTime,
    incidentDetails: String,
    reportedDateTime: LocalDateTime,
    reportedDate: LocalDate,
    incidentDateTime: LocalDateTime,
    incidentDate: LocalDate,
    prisonId: String,
    agencyInternalLocationId: Long,
    reportingStaff: Staff,
    dsl: AdjudicationIncidentDsl.() -> Unit,
  ): AdjudicationIncident = repository!!.save(
    AdjudicationIncidentBuilder(
      whenCreated = whenCreated,
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
  ): Offender = repository!!.save(
    OffenderBuilder(nomsId, lastName, firstName, birthDate, genderCode, repository = repository).apply(dsl),
  )

  // This is here so we can use the new DSL in conjunction with the old one - once everything in the old Offender DSL has been switched to the new one we can get rid of `offender` and rename this
  @NewOffenderDslMarker
  override fun newOffender(
    nomsId: String,
    lastName: String,
    firstName: String,
    birthDate: LocalDate,
    genderCode: String,
    dsl: NewOffenderDsl.() -> Unit,
  ): Offender =
    offenderBuilderFactory!!.builder()
      .let { builder ->
        builder.build(nomsId, lastName, firstName, birthDate, genderCode)
          .also {
            builder.apply(dsl)
          }
      }

  @ProgramServiceDslMarker
  override fun programService(
    programCode: String,
    programId: Long,
    description: String,
    active: Boolean,
    dsl: ProgramServiceDsl.() -> Unit,
  ): ProgramService =
    programServiceBuilderFactory!!.builder()
      .let { builder ->
        builder.build(programCode, programId, description, active)
          .also {
            builder.apply(dsl)
          }
      }
}

@NomisDataDslMarker
interface NomisDataDsl {
  @StaffDslMarker
  fun staff(firstName: String = "AAYAN", lastName: String = "AHMAD", dsl: StaffDsl.() -> Unit = {}): Staff

  @AdjudicationIncidentDslMarker
  fun adjudicationIncident(
    whenCreated: LocalDateTime = LocalDateTime.now(),
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

  // This allows us to slowly migrate from the old DSL to the new one. Once migration is complete we can delete @OffenderDslMarker then rename @NewOffenderDslMarker
  @NewOffenderDslMarker
  fun newOffender(
    nomsId: String = "A5194DY",
    lastName: String = "NTHANDA",
    firstName: String = "LEKAN",
    birthDate: LocalDate = LocalDate.of(1965, 7, 19),
    genderCode: String = "M",
    dsl: NewOffenderDsl.() -> Unit = {},
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

@NomisDataDslMarker
interface StaffDsl

@NomisDataDslMarker
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

@NomisDataDslMarker
interface BookingDsl {
  @AdjudicationPartyDslMarker
  fun adjudicationParty(
    incident: AdjudicationIncident,
    adjudicationNumber: Long = 1224,
    comment: String = "party comment",
    partyAddedDate: LocalDate = LocalDate.of(2023, 5, 10),
    incidentRole: String = suspectRole,
    actionDecision: String = PLACED_ON_REPORT_ACTION_CODE,
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
}

@NomisDataDslMarker
interface IncentiveDsl

@NomisDataDslMarker
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

@NomisDataDslMarker
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
    ref: DataRef<AdjudicationIncidentCharge>? = null,
    dsl: AdjudicationChargeDsl.() -> Unit = {},
  )

  @AdjudicationHearingDslMarker
  fun hearing(
    internalLocationId: Long? = null,
    scheduleDate: LocalDate? = null,
    scheduleTime: LocalDateTime? = null,
    hearingDate: LocalDate? = LocalDate.now(),
    hearingTime: LocalDateTime? = LocalDateTime.now(),
    hearingStaff: Staff? = null,
    hearingTypeCode: String = AdjudicationHearingType.GOVERNORS_HEARING,
    eventStatusCode: String = "SCH",
    comment: String = "Hearing comment",
    representativeText: String = "rep text",
    dsl: AdjudicationHearingDsl.() -> Unit = {},
  )
}

@NomisDataDslMarker
interface AdjudicationRepairDsl

@NomisDataDslMarker
interface AdjudicationInvestigationDsl {
  @AdjudicationEvidenceDslMarker
  fun evidence(
    detail: String = "Knife found",
    type: String = "WEAP",
    date: LocalDate = LocalDate.now(),
    dsl: AdjudicationEvidenceDsl.() -> Unit = {},
  )
}

@NomisDataDslMarker
interface AdjudicationEvidenceDsl

@NomisDataDslMarker
interface AdjudicationChargeDsl

@NomisDataDslMarker
interface AdjudicationHearingDsl {
  @AdjudicationHearingResultDslMarker
  fun result(
    chargeRef: DataRef<AdjudicationIncidentCharge>,
    pleaFindingCode: String = "NOT_GUILTY",
    findingCode: String = "PROVED",
    dsl: AdjudicationHearingResultDsl.() -> Unit = {},
  )
}

@NomisDataDslMarker
interface AdjudicationHearingResultDsl {
  @AdjudicationHearingResultAwardDslMarker
  fun award(
    statusCode: String,
    sanctionDays: Int? = null,
    sanctionMonths: Int? = null,
    compensationAmount: BigDecimal? = null,
    sanctionCode: String,
    comment: String? = null,
    effectiveDate: LocalDate,
    statusDate: LocalDate? = null,
    consecutiveSanctionSeq: Int? = null,
    dsl: AdjudicationHearingResultAwardDsl.() -> Unit = {},
  )
}

@NomisDataDslMarker
interface AdjudicationHearingResultAwardDsl

@DslMarker
annotation class NomisDataDslMarker

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
annotation class AdjudicationHearingResultAwardDslMarker

class DataRef<T>(private var reference: T? = null) {
  fun set(value: T) {
    reference = value
  }
  fun value(): T = reference ?: throw IllegalStateException("Reference not set")
}

fun <T> dataRef() = DataRef<T>()
