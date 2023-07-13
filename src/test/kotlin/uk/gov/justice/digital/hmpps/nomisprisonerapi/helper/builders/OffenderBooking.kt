package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.PartyRole.WITNESS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentDecisionAction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderProgramProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class BookingDslMarker

@NomisDataDslMarker
interface BookingDsl {
  @AdjudicationPartyDslMarker
  fun adjudicationParty(
    incident: AdjudicationIncident,
    comment: String = "They witnessed everything",
    role: PartyRole = WITNESS,
    partyAddedDate: LocalDate = LocalDate.of(2023, 5, 10),
    staff: Staff? = null,
    adjudicationNumber: Long? = null,
    actionDecision: String = IncidentDecisionAction.NO_FURTHER_ACTION_CODE,
    dsl: AdjudicationPartyDsl.() -> Unit = {},
  ): AdjudicationIncidentParty

  @IncentiveDslMarker
  fun incentive(
    iepLevelCode: String = "ENT",
    userId: String? = null,
    sequence: Long = 1,
    commentText: String = "comment",
    auditModuleName: String? = null,
    iepDateTime: LocalDateTime = LocalDateTime.now(),
  ): Incentive

  @CourseAllocationDslMarker
  fun courseAllocation(
    courseActivity: CourseActivity,
    startDate: String? = "2022-10-31",
    programStatusCode: String = "ALLOC",
    endDate: String? = null,
    endReasonCode: String? = null,
    endComment: String? = null,
    dsl: CourseAllocationDsl.() -> Unit = { payBand() },
  ): OffenderProgramProfile
}

@Component
class BookingBuilderRepository(
  private val offenderBookingRepository: OffenderBookingRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun save(offenderBooking: OffenderBooking): OffenderBooking = offenderBookingRepository.save(offenderBooking)
  fun lookupAgencyLocation(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id)!!
}

@Component
class BookingBuilderFactory(
  private val repository: BookingBuilderRepository,
  private val courseAllocationBuilderFactory: CourseAllocationBuilderFactory,
  private val incentiveBuilderFactory: IncentiveBuilderFactory,
  private val adjudicationPartyBuilderFactory: AdjudicationPartyBuilderFactory,
) {
  fun builder() = BookingBuilder(
    repository,
    courseAllocationBuilderFactory,
    incentiveBuilderFactory,
    adjudicationPartyBuilderFactory,
  )
}

class BookingBuilder(
  private val repository: BookingBuilderRepository,
  private val courseAllocationBuilderFactory: CourseAllocationBuilderFactory,
  private val incentiveBuilderFactory: IncentiveBuilderFactory,
  private val adjudicationPartyBuilderFactory: AdjudicationPartyBuilderFactory,
) : BookingDsl {

  private lateinit var offenderBooking: OffenderBooking

  fun build(
    offender: Offender,
    bookingSequence: Int,
    agencyLocationCode: String,
    bookingBeginDate: LocalDateTime = LocalDateTime.now(),
    active: Boolean,
    inOutStatus: String,
    youthAdultCode: String,
  ): OffenderBooking {
    val agencyLocation = repository.lookupAgencyLocation(agencyLocationCode)
    return OffenderBooking(
      offender = offender,
      rootOffender = offender,
      bookingSequence = bookingSequence,
      createLocation = agencyLocation,
      location = agencyLocation,
      bookingBeginDate = bookingBeginDate,
      active = active,
      inOutStatus = inOutStatus,
      youthAdultCode = youthAdultCode,
    )
      .let { repository.save(it) }
      .also { offenderBooking = it }
  }

  override fun courseAllocation(
    courseActivity: CourseActivity,
    startDate: String?,
    programStatusCode: String,
    endDate: String?,
    endReasonCode: String?,
    endComment: String?,
    dsl: CourseAllocationDsl.() -> Unit,
  ) =
    courseAllocationBuilderFactory.builder()
      .let { builder ->
        builder.build(offenderBooking, startDate, programStatusCode, endDate, endReasonCode, endComment, courseActivity)
          .also { offenderBooking.offenderProgramProfiles += it }
          .also { builder.apply(dsl) }
      }

  override fun incentive(
    iepLevelCode: String,
    userId: String?,
    sequence: Long,
    commentText: String,
    auditModuleName: String?,
    iepDateTime: LocalDateTime,
  ) =
    incentiveBuilderFactory.builder()
      .build(
        offenderBooking,
        iepLevelCode,
        userId,
        sequence,
        commentText,
        auditModuleName,
        iepDateTime,
      )
      .also { offenderBooking.incentives += it }

  override fun adjudicationParty(
    incident: AdjudicationIncident,
    comment: String,
    role: PartyRole,
    partyAddedDate: LocalDate,
    staff: Staff?,
    adjudicationNumber: Long?,
    actionDecision: String,
    dsl: AdjudicationPartyDsl.() -> Unit,
  ) =
    adjudicationPartyBuilderFactory.builder().let { builder ->
      builder.build(
        adjudicationNumber = adjudicationNumber,
        comment = comment,
        staff = staff,
        incidentRole = role.code,
        actionDecision = actionDecision,
        partyAddedDate = partyAddedDate,
        incident = incident,
        offenderBooking = offenderBooking,
        whenCreated = LocalDateTime.now(),
        index = incident.parties.size + 1,
      )
        .also { incident.parties += it }
        .also { builder.apply(dsl) }
    }
}
