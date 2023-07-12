package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.WITNESS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationEvidence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResultAward
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentRepair
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationInvestigation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction.Companion.NO_FURTHER_ACTION_CODE
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

@Component
@Transactional
class NomisDataBuilder(
  private val programServiceBuilderFactory: ProgramServiceBuilderFactory? = ProgramServiceBuilderFactory(),
  private val offenderBuilderFactory: OffenderBuilderFactory? = null, // note this means the offender DSL is not available in unit tests whereas programService is.
  private val staffBuilderFactory: StaffBuilderFactory? = null,
  private val adjudicationIncidentBuilderFactory: AdjudicationIncidentBuilderFactory? = null,
) {
  fun build(dsl: NomisData.() -> Unit) = NomisData(programServiceBuilderFactory, offenderBuilderFactory, staffBuilderFactory, adjudicationIncidentBuilderFactory).apply(dsl)
}

class NomisData(
  private val programServiceBuilderFactory: ProgramServiceBuilderFactory? = null,
  private val offenderBuilderFactory: OffenderBuilderFactory? = null,
  private val staffBuilderFactory: StaffBuilderFactory? = null,
  private val adjudicationIncidentBuilderFactory: AdjudicationIncidentBuilderFactory? = null,
) : NomisDataDsl {
  @StaffDslMarker
  override fun staff(firstName: String, lastName: String, dsl: StaffDsl.() -> Unit): Staff =
    staffBuilderFactory!!.builder()
      .let { builder ->
        builder.build(lastName, firstName)
          .also {
            builder.apply(dsl)
          }
      }

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
  ): AdjudicationIncident = adjudicationIncidentBuilderFactory!!.builder()
    .let { builder ->
      builder.build(
        whenCreated = whenCreated,
        incidentDetails = incidentDetails,
        reportedDateTime = reportedDateTime,
        reportedDate = reportedDate,
        incidentDateTime = incidentDateTime,
        incidentDate = incidentDate,
        prisonId = prisonId,
        agencyInternalLocationId = agencyInternalLocationId,
        reportingStaff = reportingStaff,
      )
        .also {
          builder.apply(dsl)
        }
    }

  @OffenderDslMarker
  override fun offender(
    nomsId: String,
    lastName: String,
    firstName: String,
    birthDate: LocalDate,
    genderCode: String,
    dsl: OffenderDsl.() -> Unit,
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
interface NomisDataDsl : ProgramServiceDslApi, OffenderDslApi {
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
}

@NomisDataDslMarker
interface StaffDsl

@NomisDataDslMarker
interface AdjudicationIncidentPartyDslApi {
  @AdjudicationPartyDslMarker
  fun adjudicationParty(
    incident: AdjudicationIncident,
    comment: String = "They witnessed everything",
    role: PartyRole = WITNESS,
    partyAddedDate: LocalDate = LocalDate.of(2023, 5, 10),
    staff: Staff? = null,
    adjudicationNumber: Long? = null,
    actionDecision: String = NO_FURTHER_ACTION_CODE,
    dsl: AdjudicationPartyDsl.() -> Unit = {},
  ): AdjudicationIncidentParty
}

@NomisDataDslMarker
interface AdjudicationIncidentDsl {
  @AdjudicationRepairDslMarker
  fun repair(
    repairType: String = "CLEA",
    comment: String? = null,
    repairCost: BigDecimal? = null,
    dsl: AdjudicationRepairDsl.() -> Unit = {},
  ): AdjudicationIncidentRepair

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
  ): AdjudicationIncidentParty
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
  ): AdjudicationInvestigation

  @AdjudicationChargeDslMarker
  fun charge(
    offenceCode: String = "51:1B",
    guiltyEvidence: String? = null,
    reportDetail: String? = null,
    dsl: AdjudicationChargeDsl.() -> Unit = {},
  ): AdjudicationIncidentCharge

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
  ): AdjudicationHearing
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
  ): AdjudicationEvidence
}

@NomisDataDslMarker
interface AdjudicationEvidenceDsl

@NomisDataDslMarker
interface AdjudicationChargeDsl

@NomisDataDslMarker
interface AdjudicationHearingDsl {
  @AdjudicationHearingResultDslMarker
  fun result(
    charge: AdjudicationIncidentCharge,
    pleaFindingCode: String = "NOT_GUILTY",
    findingCode: String = "PROVED",
    dsl: AdjudicationHearingResultDsl.() -> Unit = {},
  ): AdjudicationHearingResult
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
    consecutiveHearingResultAward: AdjudicationHearingResultAward? = null,
    dsl: AdjudicationHearingResultAwardDsl.() -> Unit = {},
  ): AdjudicationHearingResultAward
}

@NomisDataDslMarker
interface AdjudicationHearingResultAwardDsl

@DslMarker
annotation class NomisDataDslMarker

@DslMarker
annotation class StaffDslMarker

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
