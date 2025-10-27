package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTimeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository
import java.time.DayOfWeek

@Service
@Transactional
class VisitsConfigurationService(val agencyVisitTimeRepository: AgencyVisitTimeRepository, val agencyLocationRepository: AgencyLocationRepository) {
  fun getVisitTimeSlotIds(pageRequest: Pageable): Page<VisitTimeSlotIdResponse> = agencyVisitTimeRepository.findAllIds(pageRequest).map {
    VisitTimeSlotIdResponse(
      prisonId = it.prisonId,
      dayOfWeek = it.weekdayCode.asDayOfWeek(),
      timeSlotSequence = it.timeSlotSequence,
    )
  }

  fun getVisitTimeSlot(prisonId: String, dayOfWeek: DayOfWeek, timeSlotSequence: Int): VisitTimeSlotResponse = agencyVisitTimeRepository.findByIdOrNull(
    AgencyVisitTimeId(
      location = lookupAgency(prisonId),
      weekDay = dayOfWeek.asDayOfWeek(),
      timeSlotSequence = timeSlotSequence,
    ),
  )?.let {
    VisitTimeSlotResponse(
      prisonId = it.agencyVisitTimesId.location.id,
      dayOfWeek = it.agencyVisitTimesId.weekDay.asDayOfWeek(),
      timeSlotSequence = it.agencyVisitTimesId.timeSlotSequence,
      startTime = it.startTime,
      endTime = it.endTime,
      effectiveDate = it.effectiveDate,
      expiryDate = it.expiryDate,
      visitSlots = it.visitSlots.map {
        VisitSlotResponse(
          internalLocation = it.agencyInternalLocation.let { location ->
            VisitInternalLocationResponse(
              id = location.locationId,
              code = location.locationCode,
            )
          },
          maxGroups = it.maxGroups,
          maxAdults = it.maxAdults,
        )
      },
    )
  } ?: throw NotFoundException("Visit time slot $prisonId, $dayOfWeek, $timeSlotSequence does not exist")

  private fun lookupAgency(prisonId: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(prisonId) ?: throw BadDataException("Prison $prisonId does not exist")
}

private fun String.asDayOfWeek(): DayOfWeek = when (this) {
  "MON" -> DayOfWeek.MONDAY
  "TUE" -> DayOfWeek.TUESDAY
  "WED" -> DayOfWeek.WEDNESDAY
  "THU" -> DayOfWeek.THURSDAY
  "FRI" -> DayOfWeek.FRIDAY
  "SAT" -> DayOfWeek.SATURDAY
  "SUN" -> DayOfWeek.SUNDAY
  else -> throw IllegalArgumentException("Invalid day of week $this")
}
private fun DayOfWeek.asDayOfWeek(): String = when (this) {
  DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> this.name.take(3)
}
