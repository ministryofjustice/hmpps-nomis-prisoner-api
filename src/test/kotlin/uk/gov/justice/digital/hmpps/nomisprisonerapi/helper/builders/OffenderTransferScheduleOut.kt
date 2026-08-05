package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Escort
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleWaitList
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class OffenderTransferScheduleOutDslMarker

@OffenderTransferScheduleOutDslMarker
interface OffenderTransferScheduleOutDsl {
  @OffenderTransferScheduleWaitListDslMarker
  fun waitList(
    requestDate: LocalDate = LocalDate.now(),
    waitListStatus: String = "PEN",
    statusDate: LocalDate = LocalDate.now(),
    transferPriority: String = "1",
    approvedFlag: Boolean = false,
    approvedStaff: Staff? = null,
    outcomeReasonCode: String? = null,
    commentText1: String? = null,
    commentText2: String? = null,
    dsl: OffenderTransferScheduleWaitListDsl.() -> Unit = {},
  ): OffenderTransferScheduleWaitList
}

@Component
class OffenderTransferScheduleOutBuilderRepository(
  private val repository: OffenderTransferScheduleOutRepository,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val movementReasonRepository: ReferenceCodeRepository<MovementReason>,
  private val escortRepository: ReferenceCodeRepository<Escort>,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun save(transferScheduleOut: OffenderTransferScheduleOut): OffenderTransferScheduleOut = repository.saveAndFlush(transferScheduleOut)
  fun eventStatusOf(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
  fun movementReasonOf(code: String): MovementReason = movementReasonRepository.findByIdOrNull(MovementReason.pk(code))!!
  fun escortOf(code: String): Escort = escortRepository.findByIdOrNull(Escort.pk(code))!!
  fun agencyLocationOf(id: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(id) ?: throw RuntimeException("Agency location with id=$id not found")
}

@Component
class OffenderTransferScheduleOutBuilderFactory(
  private val repository: OffenderTransferScheduleOutBuilderRepository,
  private val waitListBuilderFactory: OffenderTransferScheduleWaitListBuilderFactory,
) {
  fun builder() = OffenderTransferScheduleOutBuilder(repository, waitListBuilderFactory)
}

class OffenderTransferScheduleOutBuilder(
  private val repository: OffenderTransferScheduleOutBuilderRepository,
  private val waitListBuilderFactory: OffenderTransferScheduleWaitListBuilderFactory,
) : OffenderTransferScheduleOutDsl {

  private lateinit var transferScheduleOut: OffenderTransferScheduleOut

  fun build(
    offenderBooking: OffenderBooking,
    eventDate: LocalDate?,
    startTime: LocalDateTime?,
    eventSubType: String,
    eventStatus: String,
    comment: String?,
    escort: String?,
    fromPrison: String,
    toPrison: String?,
  ): OffenderTransferScheduleOut = repository.save(
    OffenderTransferScheduleOut(
      offenderBooking = offenderBooking,
      eventDate = eventDate,
      startTime = startTime,
      eventSubType = repository.movementReasonOf(eventSubType),
      eventStatus = repository.eventStatusOf(eventStatus),
      comment = comment,
      escort = escort?.let { repository.escortOf(it) },
      fromPrison = repository.agencyLocationOf(fromPrison),
      toPrison = toPrison?.let { repository.agencyLocationOf(it) },
    ),
  )
    .also { transferScheduleOut = it }

  override fun waitList(
    requestDate: LocalDate,
    waitListStatus: String,
    statusDate: LocalDate,
    transferPriority: String,
    approvedFlag: Boolean,
    approvedStaff: Staff?,
    outcomeReasonCode: String?,
    commentText1: String?,
    commentText2: String?,
    dsl: OffenderTransferScheduleWaitListDsl.() -> Unit,
  ): OffenderTransferScheduleWaitList = waitListBuilderFactory.builder().let { builder ->
    builder.build(
      schedule = transferScheduleOut,
      requestDate = requestDate,
      waitListStatus = waitListStatus,
      statusDate = statusDate,
      transferPriority = transferPriority,
      approvedFlag = approvedFlag,
      approvedStaff = approvedStaff,
      outcomeReasonCode = outcomeReasonCode,
      commentText1 = commentText1,
      commentText2 = commentText2,
    )
      .also { transferScheduleOut.waitList = it }
      .also { builder.apply(dsl) }
  }
}
