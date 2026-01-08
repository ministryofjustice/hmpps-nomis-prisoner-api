package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalScheduleReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAppointment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAppointmentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class AppointmentService(
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderAppointmentRepository: OffenderAppointmentRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val internalScheduleReasonRepository: ReferenceCodeRepository<InternalScheduleReason>,
  private val telemetryClient: TelemetryClient,
) {
  fun createAppointment(dto: CreateAppointmentRequest): CreateAppointmentResponse = CreateAppointmentResponse(
    offenderAppointmentRepository.save(mapModel(dto))
      .also {
        telemetryClient.trackEvent(
          "appointment-created",
          mapOf(
            "eventId" to it.eventId.toString(),
            "bookingId" to it.offenderBooking.bookingId.toString(),
            "location" to it.internalLocation?.locationId.toString(),
          ),
          null,
        )
      }
      .eventId,
  )

  fun updateAppointment(eventId: Long, dto: UpdateAppointmentRequest) {
    offenderAppointmentRepository.findByIdOrNull(eventId)
      ?.apply {
        val location = dto.internalLocationId?.let {
          agencyInternalLocationRepository.findByIdOrNull(dto.internalLocationId)
            ?: throw BadDataException("Room with id=${dto.internalLocationId} does not exist")
        }
          ?: offenderBooking.assignedLivingUnit
          ?: throw BadDataException("No location found for bookingId ${offenderBooking.bookingId}, appointment event=$eventId")

        val subType = internalScheduleReasonRepository.findByIdOrNull(InternalScheduleReason.pk(dto.eventSubType))
          ?: throw BadDataException("EventSubType with code=${dto.eventSubType} does not exist")

        if (dto.endTime != null && dto.endTime < dto.startTime) {
          throw BadDataException("End time must be after start time")
        }

        internalLocation = location
        startTime = LocalDateTime.of(dto.eventDate, dto.startTime)
        endTime = dto.endTime?.let { LocalDateTime.of(dto.eventDate, it) }
        eventDate = dto.eventDate
        eventSubType = subType
        comment = dto.comment

        telemetryClient.trackEvent(
          "appointment-updated",
          mapOf(
            "eventId" to eventId.toString(),
            "bookingId" to offenderBooking.bookingId.toString(),
            "location" to internalLocation?.locationId.toString(),
          ),
          null,
        )
      }
      ?: throw NotFoundException("Appointment with event id $eventId not found")
  }

  fun cancelAppointment(eventId: Long) {
    offenderAppointmentRepository.findByIdOrNull(eventId)
      ?.apply {
        eventStatus = eventStatusRepository.findById(EventStatus.CANCELLED).orElseThrow()

        telemetryClient.trackEvent(
          "appointment-cancelled",
          mapOf(
            "eventId" to eventId.toString(),
            "bookingId" to offenderBooking.bookingId.toString(),
            "location" to internalLocation?.locationId.toString(),
          ),
          null,
        )
      }
      ?: throw NotFoundException("Appointment with event id $eventId not found")
  }

  fun uncancelAppointment(eventId: Long) {
    offenderAppointmentRepository.findByIdOrNull(eventId)
      ?.apply {
        eventStatus = eventStatusRepository.findById(EventStatus.SCHEDULED_APPROVED).orElseThrow()

        telemetryClient.trackEvent(
          "appointment-uncancelled",
          mapOf(
            "eventId" to eventId.toString(),
            "bookingId" to offenderBooking.bookingId.toString(),
            "location" to internalLocation?.locationId.toString(),
          ),
          null,
        )
      }
      ?: throw NotFoundException("Appointment with event id $eventId not found")
  }

  fun deleteAppointment(eventId: Long) {
    offenderAppointmentRepository.findByIdOrNull(eventId)
      ?.also {
        offenderAppointmentRepository.delete(it)
        telemetryClient.trackEvent(
          "appointment-deleted",
          mapOf(
            "eventId" to it.eventId.toString(),
            "bookingId" to it.offenderBooking.bookingId.toString(),
            "location" to it.internalLocation?.locationId.toString(),
          ),
          null,
        )
      }
      ?: throw NotFoundException("Appointment with event id $eventId not found")
  }

  private fun mapModel(dto: CreateAppointmentRequest): OffenderAppointment {
    val offenderBooking = offenderBookingRepository.findByIdOrNull(dto.bookingId)
      ?: throw BadDataException("Booking with id=${dto.bookingId} not found")

    val location = dto.internalLocationId?.let {
      agencyInternalLocationRepository.findByIdOrNull(dto.internalLocationId)
        ?: throw BadDataException("Room with id=${dto.internalLocationId} does not exist")
    }
      ?.also {
        if (it.agency.id != offenderBooking.location?.id) {
          throw BadDataException("Room with id=${dto.internalLocationId} is in ${it.agency.id}, not in the offender's prison: ${offenderBooking.location?.id}")
        }
      }
      ?: offenderBooking.assignedLivingUnit

    if (dto.endTime != null && dto.endTime < dto.startTime) {
      throw BadDataException("End time must be after start time")
    }

    val eventSubType = internalScheduleReasonRepository.findByIdOrNull(InternalScheduleReason.pk(dto.eventSubType))
      ?: throw BadDataException("EventSubType with code=${dto.eventSubType} does not exist")

    return OffenderAppointment(
      offenderBooking = offenderBooking,
      eventDate = dto.eventDate,
      startTime = LocalDateTime.of(dto.eventDate, dto.startTime),
      endTime = dto.endTime?.let { LocalDateTime.of(dto.eventDate, it) },
      prison = offenderBooking.location!!,
      eventStatus = eventStatusRepository.findById(EventStatus.SCHEDULED_APPROVED).orElseThrow(),
      internalLocation = location,
      eventSubType = eventSubType,
      comment = dto.comment,
    )
  }

  fun getAppointment(bookingId: Long, locationId: Long, date: LocalDateTime): AppointmentResponse = offenderAppointmentRepository.findOneByBookingLocationDateAndStartTime(
    bookingId = bookingId,
    locationId = locationId,
    date = date.toLocalDate(),
    hour = date.hour,
    minute = date.minute,
  )?.let {
    mapModel(it)
  }
    ?: throw NotFoundException("Appointment not found")

  fun getAppointment(eventId: Long): AppointmentResponse = offenderAppointmentRepository.findByIdOrNull(eventId)?.let {
    mapModel(it)
  }
    ?: throw NotFoundException("Appointment not found")

  fun findIdsByFilter(pageRequest: Pageable, appointmentFilter: AppointmentFilter): Page<AppointmentIdResponse> {
    val prisons = appointmentFilter.prisonIds
    val fromDate = appointmentFilter.fromDate ?: LocalDate.now().plusYears(-100)
    val toDate = appointmentFilter.toDate ?: LocalDate.now().plusYears(100)
    val totalElements = offenderAppointmentRepository.findAllCount(
      prisons,
      fromDate,
      toDate,
    )
    return offenderAppointmentRepository.findAllByPage(
      prisons,
      fromDate,
      toDate,
      pageRequest.offset + 1,
      pageRequest.offset + pageRequest.pageSize,
    )
      .let { content -> PageImpl(content, pageRequest, totalElements) }
      .map { AppointmentIdResponse(eventId = it) }
  }

  fun findCountsByFilter(appointmentFilter: AppointmentFilter): List<AppointmentCountsResponse> = offenderAppointmentRepository.findCountsByFilter(
    appointmentFilter.prisonIds,
    appointmentFilter.fromDate ?: LocalDate.now().plusYears(-100),
    appointmentFilter.toDate ?: LocalDate.now().plusYears(100),
  )
    .map {
      AppointmentCountsResponse(
        prisonId = it.getPrisonId(),
        eventSubType = it.getEventSubType(),
        future = it.getPastOrFuture() == "FUTURE",
        count = it.getAppointmentCount(),
      )
    }
}

private fun mapModel(entity: OffenderAppointment): AppointmentResponse = AppointmentResponse(
  bookingId = entity.offenderBooking.bookingId,
  offenderNo = entity.offenderBooking.offender.nomsId,
  startDateTime = entity.startTime?.let { LocalDateTime.of(entity.eventDate, it.toLocalTime()) },
  endDateTime = entity.endTime?.let { LocalDateTime.of(entity.eventDate, it.toLocalTime()) },
  status = entity.eventStatus.code,
  subtype = entity.eventSubType.code,
  internalLocation = entity.internalLocation?.locationId,
  prisonId = entity.prison.id,
  comment = entity.comment,
  createdDate = entity.createDatetime,
  createdBy = entity.createUsername,
  modifiedDate = entity.modifyDatetime,
  modifiedBy = entity.modifyUserId,
)
