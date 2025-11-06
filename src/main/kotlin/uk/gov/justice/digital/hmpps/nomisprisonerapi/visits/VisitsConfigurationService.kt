package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTimeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository

@Service
@Transactional
class VisitsConfigurationService(val agencyVisitTimeRepository: AgencyVisitTimeRepository, val agencyLocationRepository: AgencyLocationRepository) {
  fun getVisitTimeSlotIds(pageRequest: Pageable): Page<VisitTimeSlotIdResponse> = agencyVisitTimeRepository.findAllIds(pageRequest).map {
    VisitTimeSlotIdResponse(
      prisonId = it.prisonId,
      dayOfWeek = it.weekdayCode,
      timeSlotSequence = it.timeSlotSequence,
    )
  }

  fun getVisitTimeSlot(prisonId: String, dayOfWeek: WeekDay, timeSlotSequence: Int): VisitTimeSlotResponse = agencyVisitTimeRepository.findByIdOrNull(
    AgencyVisitTimeId(
      location = lookupAgency(prisonId),
      weekDay = dayOfWeek,
      timeSlotSequence = timeSlotSequence,
    ),
  )?.let {
    VisitTimeSlotResponse(
      prisonId = it.agencyVisitTimesId.location.id,
      dayOfWeek = it.agencyVisitTimesId.weekDay,
      timeSlotSequence = it.agencyVisitTimesId.timeSlotSequence,
      startTime = it.startTime,
      endTime = it.endTime,
      effectiveDate = it.effectiveDate,
      expiryDate = it.expiryDate,
      audit = it.toAudit(),
      visitSlots = it.visitSlots.map { visitSlot ->
        VisitSlotResponse(
          id = visitSlot.id,
          internalLocation = visitSlot.agencyInternalLocation.let { location ->
            VisitInternalLocationResponse(
              id = location.locationId,
              code = location.locationCode,
            )
          },
          maxGroups = visitSlot.maxGroups,
          maxAdults = visitSlot.maxAdults,
          audit = visitSlot.toAudit(),
        )
      },
    )
  } ?: throw NotFoundException("Visit time slot $prisonId, $dayOfWeek, $timeSlotSequence does not exist")

  private fun lookupAgency(prisonId: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(prisonId) ?: throw BadDataException("Prison $prisonId does not exist")
}

private fun String.asWeekDay(): WeekDay = WeekDay.entries.find { it.name == this } ?: throw BadDataException("Visit day $this does not exist")
