package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDayId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitDayRepository
import java.time.LocalDate
import java.time.LocalTime

@DslMarker
annotation class AgencyVisitDayDslMarker

@NomisDataDslMarker
interface AgencyVisitDayDsl {
  @AgencyVisitTimeDslMarker
  fun visitTimeSlot(
    timeSlotSequence: Int,
    startTime: LocalTime = LocalTime.now(),
    endTime: LocalTime = LocalTime.now().plusHours(1),
    effectiveDate: LocalDate = LocalDate.now(),
    expiryDate: LocalDate? = null,
    dsl: AgencyVisitTimeDsl.() -> Unit = {},
  ): AgencyVisitTime
}

@Component
class AgencyVisitDayBuilderFactory(
  private val repository: AgencyVisitDayBuilderRepository,
  private val agencyVisitTimeBuilderFactory: AgencyVisitTimeBuilderFactory,
) {
  fun builder(): AgencyVisitDayBuilder = AgencyVisitDayBuilder(repository, agencyVisitTimeBuilderFactory)
}

@Component
class AgencyVisitDayBuilderRepository(
  private val agencyVisitDayRepository: AgencyVisitDayRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun save(agencyVisitDay: AgencyVisitDay): AgencyVisitDay = agencyVisitDayRepository.save(agencyVisitDay)
  fun lookupAgency(id: String): AgencyLocation = agencyLocationRepository.findById(id).orElseThrow()
}

class AgencyVisitDayBuilder(
  private val repository: AgencyVisitDayBuilderRepository,
  private val agencyVisitTimeBuilderFactory: AgencyVisitTimeBuilderFactory,
) : AgencyVisitDayDsl {
  private lateinit var agencyVisitDay: AgencyVisitDay

  fun build(
    prisonId: String,
    weekDay: WeekDay,
  ): AgencyVisitDay = AgencyVisitDay(
    agencyVisitDayId = AgencyVisitDayId(
      location = repository.lookupAgency(prisonId),
      weekDay = weekDay,
    ),
  )
    .let { repository.save(it) }
    .also { agencyVisitDay = it }

  override fun visitTimeSlot(
    timeSlotSequence: Int,
    startTime: LocalTime,
    endTime: LocalTime,
    effectiveDate: LocalDate,
    expiryDate: LocalDate?,
    dsl: AgencyVisitTimeDsl.() -> Unit,
  ): AgencyVisitTime = agencyVisitTimeBuilderFactory.builder().let { builder ->
    builder.build(
      weekDay = agencyVisitDay,
      timeSlotSequence = timeSlotSequence,
      startTime = startTime,
      endTime = endTime,
      effectiveDate = effectiveDate,
      expiryDate = expiryDate,
    )
      .also { builder.apply(dsl) }
  }
}
