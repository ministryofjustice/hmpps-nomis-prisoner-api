package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.audit.Audit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTimeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitDayRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitSlotRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository

@Service
@Transactional
class VisitsConfigurationService(
  val agencyVisitDayRepository: AgencyVisitDayRepository,
  val agencyVisitTimeRepository: AgencyVisitTimeRepository,
  val agencyVisitSlotRepository: AgencyVisitSlotRepository,
  val agencyLocationRepository: AgencyLocationRepository,
  val agencyInternalLocationRepository: AgencyInternalLocationRepository,
) {
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
  )?.toVisitTimeSlotResponse() ?: throw NotFoundException("Visit time slot $prisonId, $dayOfWeek, $timeSlotSequence does not exist")

  fun getActivePrisonsWithTimeSlots(): List<ActivePrison> = agencyVisitDayRepository.findDistinctPrisonIdByActivePrisons().map {
    ActivePrison(it)
  }
  fun getPrisonVisitTimeSlots(prisonId: String, activeOnly: Boolean): VisitTimeSlotForPrisonResponse = if (activeOnly) {
    agencyVisitTimeRepository.findByAgencyVisitTimesIdLocationIdNotExpiredIsEffective(prisonId)
  } else {
    agencyVisitTimeRepository.findByAgencyVisitTimesIdLocationId(prisonId)
  }.map { it.toVisitTimeSlotResponse() }.let {
    VisitTimeSlotForPrisonResponse(
      prisonId = prisonId,
      timeSlots = it,
    )
  }

  @Audit(auditModule = "DPS_SYNCHRONISATION_OFFICIAL_VISITS")
  fun createVisitTimeSlot(prisonId: String, dayOfWeek: WeekDay, request: CreateVisitTimeSlotRequest): VisitTimeSlotResponse {
    val location = lookupAgency(prisonId)
    val nextSequence = agencyVisitTimeRepository.getNextTimeSlotSequence(prisonId, dayOfWeek.name)

    return agencyVisitTimeRepository.saveAndFlush(
      AgencyVisitTime(
        agencyVisitTimesId = AgencyVisitTimeId(
          location = location,
          weekDay = dayOfWeek,
          timeSlotSequence = nextSequence,
        ),
        startTime = request.startTime,
        endTime = request.endTime,
        effectiveDate = request.effectiveDate,
        expiryDate = request.expiryDate,
      ),
    ).let {
      agencyVisitTimeRepository.findByIdOrNull(it.agencyVisitTimesId)
        ?: throw BadDataException("Visit time slot $prisonId, $dayOfWeek, ${it.agencyVisitTimesId.timeSlotSequence} could not be reloaded")
    }.toVisitTimeSlotResponse()
  }

  @Audit(auditModule = "DPS_SYNCHRONISATION_OFFICIAL_VISITS")
  fun createVisitSlot(prisonId: String, dayOfWeek: WeekDay, timeSlotSequence: Int, request: CreateVisitSlotRequest): VisitSlotResponse {
    val location = lookupAgency(prisonId)
    val agencyInternalLocation = lookupInternalLocation(request.internalLocationId)
    val agencyVisitTime = lookupTimeSlot(
      location = location,
      weekDay = dayOfWeek,
      timeSlotSequence = timeSlotSequence,
    )

    return agencyVisitSlotRepository.saveAndFlush(
      AgencyVisitSlot(
        location = location,
        weekDay = dayOfWeek,
        timeSlotSequence = timeSlotSequence,
        agencyVisitTime = agencyVisitTime,
        agencyInternalLocation = agencyInternalLocation,
        maxGroups = request.maxGroups,
        maxAdults = request.maxAdults,
      ),
    ).let {
      agencyVisitSlotRepository.findByIdOrNull(it.id)
        ?: throw BadDataException("Visit slot $prisonId, $dayOfWeek, $timeSlotSequence ${it.id} could not be reloaded")
    }.toVisitTimeSlotResponse()
  }

  @Audit(auditModule = "DPS_SYNCHRONISATION_OFFICIAL_VISITS")
  fun deleteVisitSlot(visitSlotId: Long) {
    agencyVisitSlotRepository.deleteById(visitSlotId)
  }

  private fun lookupAgency(prisonId: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(prisonId) ?: throw BadDataException("Prison $prisonId does not exist")
  private fun lookupInternalLocation(internalLocationId: Long): AgencyInternalLocation = agencyInternalLocationRepository.findByIdOrNull(internalLocationId) ?: throw BadDataException("Internal location $internalLocationId does not exist")
  private fun lookupTimeSlot(location: AgencyLocation, weekDay: WeekDay, timeSlotSequence: Int): AgencyVisitTime = agencyVisitTimeRepository.findByIdOrNull(
    AgencyVisitTimeId(
      location = location,
      weekDay = weekDay,
      timeSlotSequence = timeSlotSequence,
    ),
  ) ?: throw BadDataException("TimeSlot ${location.id}, $weekDay, $timeSlotSequence does not exist")
}

fun AgencyVisitTime.toVisitTimeSlotResponse() = VisitTimeSlotResponse(
  prisonId = this.agencyVisitTimesId.location.id,
  dayOfWeek = this.agencyVisitTimesId.weekDay,
  timeSlotSequence = this.agencyVisitTimesId.timeSlotSequence,
  startTime = this.startTime,
  endTime = this.endTime,
  effectiveDate = this.effectiveDate,
  expiryDate = this.expiryDate,
  audit = this.toAudit(),
  visitSlots = this.visitSlots.map { it.toVisitTimeSlotResponse() },
)

fun AgencyVisitSlot.toVisitTimeSlotResponse() = VisitSlotResponse(
  id = id,
  internalLocation = agencyInternalLocation.let { location ->
    VisitInternalLocationResponse(
      id = location.locationId,
      code = location.locationCode,
    )
  },
  maxGroups = maxGroups,
  maxAdults = maxAdults,
  audit = toAudit(),
)
