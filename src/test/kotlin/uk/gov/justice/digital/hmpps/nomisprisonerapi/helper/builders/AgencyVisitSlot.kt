package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitSlotRepository

@DslMarker
annotation class AgencyVisitSlotDslMarker

@NomisDataDslMarker
interface AgencyVisitSlotDsl

@Component
class AgencyVisitSlotBuilderFactory(
  private val repository: AgencyVisitSlotBuilderRepository,
) {
  fun builder(): AgencyVisitSlotBuilder = AgencyVisitSlotBuilder(repository)
}

@Component
class AgencyVisitSlotBuilderRepository(
  private val agencyVisitSlotRepository: AgencyVisitSlotRepository,
) {
  fun save(agencyVisitSlot: AgencyVisitSlot): AgencyVisitSlot = agencyVisitSlotRepository.save(agencyVisitSlot)
}

class AgencyVisitSlotBuilder(
  private val repository: AgencyVisitSlotBuilderRepository,
) : AgencyVisitSlotDsl {
  private lateinit var agencyVisitSlot: AgencyVisitSlot

  fun build(
    visitTime: AgencyVisitTime,
    agencyInternalLocation: AgencyInternalLocation,
    maxGroups: Int?,
    maxAdults: Int?,
  ): AgencyVisitSlot = AgencyVisitSlot(
    location = visitTime.agencyVisitTimesId.location,
    weekDay = visitTime.agencyVisitTimesId.weekDay,
    timeSlotSequence = visitTime.agencyVisitTimesId.timeSlotSequence,
    agencyVisitTime = visitTime,
    agencyInternalLocation = agencyInternalLocation,
    maxGroups = maxGroups,
    maxAdults = maxAdults,
  )
    .let { repository.save(it) }
    .also { agencyVisitSlot = it }
}
