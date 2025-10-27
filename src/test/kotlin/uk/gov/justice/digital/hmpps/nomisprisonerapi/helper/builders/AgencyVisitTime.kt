package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTimeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository
import java.time.LocalDate
import java.time.LocalTime

@DslMarker
annotation class AgencyVisitTimeDslMarker

@NomisDataDslMarker
interface AgencyVisitTimeDsl {
  @AgencyVisitSlotDslMarker
  fun visitSlot(
    agencyInternalLocation: AgencyInternalLocation,
    maxGroups: Int? = null,
    maxAdults: Int? = null,
    dsl: AgencyVisitSlotDsl.() -> Unit = {},
  ): AgencyVisitSlot
}

@Component
class AgencyVisitTimeBuilderFactory(
  private val repository: AgencyVisitTimeBuilderRepository,
  private val agencyVisitSlotBuilderFactory: AgencyVisitSlotBuilderFactory,
) {
  fun builder(): AgencyVisitTimeBuilder = AgencyVisitTimeBuilder(repository, agencyVisitSlotBuilderFactory)
}

@Component
class AgencyVisitTimeBuilderRepository(
  private val agencyVisitTimeRepository: AgencyVisitTimeRepository,
) {
  fun save(agencyVisitTime: AgencyVisitTime): AgencyVisitTime = agencyVisitTimeRepository.save(agencyVisitTime)
}

class AgencyVisitTimeBuilder(
  private val repository: AgencyVisitTimeBuilderRepository,
  private val agencyVisitSlotBuilderFactory: AgencyVisitSlotBuilderFactory,
) : AgencyVisitTimeDsl {
  private lateinit var agencyVisitTime: AgencyVisitTime

  fun build(
    weekDay: AgencyVisitDay,
    timeSlotSequence: Int,
    startTime: LocalTime,
    endTime: LocalTime,
    effectiveDate: LocalDate,
    expiryDate: LocalDate?,
  ): AgencyVisitTime = AgencyVisitTime(
    agencyVisitTimesId = AgencyVisitTimeId(
      location = weekDay.agencyVisitDayId.location,
      weekDay = weekDay.agencyVisitDayId.weekDay,
      timeSlotSequence = timeSlotSequence,
    ),
    startTime = startTime,
    endTime = endTime,
    effectiveDate = effectiveDate,
    expiryDate = expiryDate,
  )
    .let { repository.save(it) }
    .also { agencyVisitTime = it }

  override fun visitSlot(
    agencyInternalLocation: AgencyInternalLocation,
    maxGroups: Int?,
    maxAdults: Int?,
    dsl: AgencyVisitSlotDsl.() -> Unit,
  ): AgencyVisitSlot = agencyVisitSlotBuilderFactory.builder().let { builder ->
    builder.build(
      visitTime = agencyVisitTime,
      agencyInternalLocation = agencyInternalLocation,
      maxGroups = maxGroups,
      maxAdults = maxAdults,
    )
      .also { agencyVisitTime.visitSlots += it }
      .also { builder.apply(dsl) }
  }
}
