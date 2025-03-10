package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingNotification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingResult
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AdjudicationHearingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class AdjudicationHearingDslMarker

@NomisDataDslMarker
interface AdjudicationHearingDsl {
  @AdjudicationHearingResultDslMarker
  fun result(
    charge: AdjudicationIncidentCharge,
    pleaFindingCode: String = "NOT_GUILTY",
    findingCode: String = "PROVED",
    dsl: AdjudicationHearingResultDsl.() -> Unit = {},
  ): AdjudicationHearingResult

  @AdjudicationHearingNotificationDslMarker
  fun notification(
    staff: Staff,
    deliveryDateTime: LocalDateTime = LocalDateTime.now(),
    deliveryDate: LocalDate = LocalDate.now(),
    comment: String? = null,
    dsl: AdjudicationHearingNotificationDsl.() -> Unit = {},
  ): AdjudicationHearingNotification
}

@Component
class AdjudicationHearingBuilderFactory(
  private val adjudicationHearingResultBuilderFactory: AdjudicationHearingResultBuilderFactory,
  private val adjudicationHearingNotificationBuilderFactory: AdjudicationHearingNotificationBuilderFactory,
  private val repository: AdjudicationHearingBuilderRepository,
) {
  fun builder(): AdjudicationHearingBuilder = AdjudicationHearingBuilder(adjudicationHearingResultBuilderFactory, adjudicationHearingNotificationBuilderFactory, repository)
}

@Component
class AdjudicationHearingBuilderRepository(
  val adjudicationHearingRepository: AdjudicationHearingRepository,
  val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  val hearingTypeRepository: ReferenceCodeRepository<AdjudicationHearingType>,
  val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
) {
  fun lookupHearingType(code: String): AdjudicationHearingType = hearingTypeRepository.findByIdOrNull(AdjudicationHearingType.pk(code))!!

  fun lookupAgencyInternalLocation(locationId: Long): AgencyInternalLocation? = agencyInternalLocationRepository.findByIdOrNull(locationId)

  fun lookupEventStatusCode(code: String): EventStatus = eventStatusRepository.findByIdOrNull(EventStatus.pk(code))!!
}

class AdjudicationHearingBuilder(
  private val adjudicationHearingResultBuilderFactory: AdjudicationHearingResultBuilderFactory,
  private val adjudicationHearingNotificationBuilderFactory: AdjudicationHearingNotificationBuilderFactory,
  private val repository: AdjudicationHearingBuilderRepository,
) : AdjudicationHearingDsl {
  private lateinit var adjudicationHearing: AdjudicationHearing

  fun build(
    hearingDate: LocalDate?,
    hearingDateTime: LocalDateTime?,
    scheduledDate: LocalDate?,
    scheduledDateTime: LocalDateTime?,
    hearingStaff: Staff? = null,
    comment: String,
    representativeText: String,
    agencyInternalLocationId: Long?,
    hearingTypeCode: String,
    eventStatusCode: String,
    incidentParty: AdjudicationIncidentParty,
  ): AdjudicationHearing = AdjudicationHearing(
    hearingParty = incidentParty,
    adjudicationNumber = incidentParty.adjudicationNumber!!,
    hearingDate = hearingDate,
    hearingDateTime = hearingDateTime,
    scheduleDate = scheduledDate,
    scheduleDateTime = scheduledDateTime,
    hearingStaff = hearingStaff,
    hearingType = repository.lookupHearingType(hearingTypeCode),
    agencyInternalLocation = repository.lookupAgencyInternalLocation(agencyInternalLocationId!!),
    eventStatus = repository.lookupEventStatusCode(eventStatusCode),
    // undecided what we are doing with this yet
    eventId = 1,
    comment = comment,
    representativeText = representativeText,
  )
    .also { repository.adjudicationHearingRepository.save(it) }
    .also { adjudicationHearing = it }

  override fun result(
    charge: AdjudicationIncidentCharge,
    pleaFindingCode: String,
    findingCode: String,
    dsl: AdjudicationHearingResultDsl.() -> Unit,
  ) = adjudicationHearingResultBuilderFactory.builder().let { builder ->
    builder.build(
      hearing = adjudicationHearing,
      charge = charge,
      pleaFindingCode = pleaFindingCode,
      findingCode = findingCode,
      index = adjudicationHearing.hearingResults.size + 1,
    )
      .also { adjudicationHearing.hearingResults += it }
      .also { builder.apply(dsl) }
  }

  override fun notification(
    staff: Staff,
    deliveryDateTime: LocalDateTime,
    deliveryDate: LocalDate,
    comment: String?,
    dsl: AdjudicationHearingNotificationDsl.() -> Unit,
  ) = adjudicationHearingNotificationBuilderFactory.builder().let { builder ->
    builder.build(
      hearing = adjudicationHearing,
      staff = staff,
      deliveryDateTime = deliveryDateTime,
      deliveryDate = deliveryDate,
      comment = comment,
      index = adjudicationHearing.hearingNotifications.size + 1,
    )
      .also { adjudicationHearing.hearingNotifications += it }
      .also { builder.apply(dsl) }
  }
}
