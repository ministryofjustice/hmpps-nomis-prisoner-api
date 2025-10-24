package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitTimeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository
import java.time.LocalDate
import java.time.LocalTime

@DslMarker
annotation class AgencyVisitTimeDslMarker

@NomisDataDslMarker
interface AgencyVisitTimeDsl

@Component
class AgencyVisitTimeBuilderFactory(
  private val repository: AgencyVisitTimeBuilderRepository,
) {
  fun builder(): AgencyVisitTimeBuilder = AgencyVisitTimeBuilder(repository)
}

@Component
class AgencyVisitTimeBuilderRepository(
  private val agencyVisitTimeRepository: AgencyVisitTimeRepository,
) {
  fun save(agencyVisitTime: AgencyVisitTime): AgencyVisitTime = agencyVisitTimeRepository.save(agencyVisitTime)
}

class AgencyVisitTimeBuilder(
  private val repository: AgencyVisitTimeBuilderRepository,
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
}
