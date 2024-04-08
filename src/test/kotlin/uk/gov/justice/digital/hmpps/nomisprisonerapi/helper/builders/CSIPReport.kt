package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPAreaOfWork
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPIncidentLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPIncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPPlan
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReportRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository

@DslMarker
annotation class CSIPReportDslMarker

@NomisDataDslMarker
interface CSIPReportDsl {
  @CSIPPlanDslMarker
  fun plan(
    identifiedNeed: String = "They need help",
    intervention: String = "Support their work",
    progression: String? = null,
    referredBy: String = "Fred Bloggs",
    dsl: CSIPPlanDsl.() -> Unit = {},
  ): CSIPPlan
}

@Component
class CSIPReportBuilderFactory(
  private val repository: CSIPReportBuilderRepository,
  private val csipPlanBuilderFactory: CSIPPlanBuilderFactory,
) {
  fun builder(): CSIPReportBuilder {
    return CSIPReportBuilder(
      repository,
      csipPlanBuilderFactory,
    )
  }
}

@Component
class CSIPReportBuilderRepository(
  private val repository: CSIPReportRepository,
  val typeRepository: ReferenceCodeRepository<CSIPIncidentType>,
  val locationRepository: ReferenceCodeRepository<CSIPIncidentLocation>,
  val areaOfWorkRepository: ReferenceCodeRepository<CSIPAreaOfWork>,
) {
  fun save(csipReport: CSIPReport): CSIPReport = repository.save(csipReport)
  fun lookupType(code: String) = typeRepository.findByIdOrNull(CSIPIncidentType.pk(code))!!
  fun lookupLocation(code: String) = locationRepository.findByIdOrNull(CSIPIncidentLocation.pk(code))!!
  fun lookupAreaOfWork(code: String) = areaOfWorkRepository.findByIdOrNull(CSIPAreaOfWork.pk(code))!!
}

class CSIPReportBuilder(
  private val repository: CSIPReportBuilderRepository,
  private val csipPlanBuilderFactory: CSIPPlanBuilderFactory,

) : CSIPReportDsl {
  private lateinit var csipReport: CSIPReport

  fun build(
    offenderBooking: OffenderBooking,
    type: String,
    location: String,
    areaOfWork: String,
    reportedBy: String,
  ): CSIPReport =
    CSIPReport(
      offenderBooking = offenderBooking,
      rootOffenderId = offenderBooking.offender.rootOffenderId ?: offenderBooking.offender.id,
      type = repository.lookupType(type),
      location = repository.lookupLocation(location),
      areaOfWork = repository.lookupAreaOfWork(areaOfWork),
      reportedBy = reportedBy,
    )
      .let { repository.save(it) }
      .also { csipReport = it }

  override fun plan(
    identifiedNeed: String,
    intervention: String,
    progression: String?,
    referredBy: String,
    dsl: CSIPPlanDsl.() -> Unit,
  ): CSIPPlan = csipPlanBuilderFactory.builder()
    .let { builder ->
      builder.build(
        csipReport = csipReport,
        identifiedNeed = identifiedNeed,
        intervention = intervention,
        progression = progression,
        referredBy = referredBy,
      )
        .also { csipReport.plans += it }
        .also { builder.apply(dsl) }
    }
}
