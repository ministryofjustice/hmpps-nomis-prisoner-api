package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.DocumentService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.trackEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPAreaOfWork
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPIncidentLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPIncidentType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReportRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@Service
@Transactional
class CSIPService(
  private val csipRepository: CSIPReportRepository,
  private val documentService: DocumentService,
  private val offenderRepository: OffenderRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
  private val telemetryClient: TelemetryClient,
  val typeRepository: ReferenceCodeRepository<CSIPIncidentType>,
  val locationRepository: ReferenceCodeRepository<CSIPIncidentLocation>,
  val areaOfWorkRepository: ReferenceCodeRepository<CSIPAreaOfWork>,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val csipTemplates = listOf("CSIPA1_HMP", "CSIPA1_FNP", "CSIPA2_HMP", "CSIPA2_FNP", "CSIPA3_HMP", "CSIPA3_FNP")
  }

  fun upsertCSIP(request: UpsertCSIPRequest): UpsertCSIPResponse {
    log.debug("Received upsert request {}", request)
    val created = request.id == 0L
    val csipReport = if (created) {
      request.toCreateCSIPReport()
    } else {
      updateCSIPReport(request)
    }

    return csipRepository.save(csipReport)
      .let {
        UpsertCSIPResponse(
          nomisCSIPReportId = it.id,
          offenderNo = it.offenderBooking.offender.nomsId,
          created = created,
        )
      }.also {
        telemetryClient.trackEvent(
          "csip-${if (created) "created" else "updated"}",
          mapOf(
            "nomisCSIPReportId" to it.nomisCSIPReportId,
            "offenderNo" to request.offenderNo,
            "upsert" to if (created) "created" else "updated",
          ),
        )
      }
  }

  private fun findLatestBooking(offenderNo: String): OffenderBooking =
    offenderBookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("Prisoner $offenderNo not found or has no bookings")

  fun getCSIPs(offenderNo: String): PrisonerCSIPsResponse =
    offenderRepository.findByNomsId(offenderNo).takeIf { it.isNotEmpty() }
      ?.let {
        PrisonerCSIPsResponse(
          csipRepository.findAllByOffenderBookingOffenderNomsId(offenderNo).map { it.toCSIPResponse() },
        )
      } ?: throw NotFoundException("Prisoner with offender no $offenderNo not found with any csips")

  fun getCSIP(csipId: Long, includeDocumentIds: Boolean): CSIPResponse? {
    val csip = csipRepository.findByIdOrNull(csipId)
      ?: throw NotFoundException("CSIP with id=$csipId does not exist")

    val documentIds = if (includeDocumentIds) {
      documentService.findAllIds(csip.offenderBooking.bookingId, csipTemplates)
    } else {
      null
    }
    return csip.toCSIPResponse(documentIds)
  }

  fun findIdsByFilter(pageRequest: Pageable, csipFilter: CSIPFilter): Page<CSIPIdResponse> {
    log.info("CSIP Id filter request : $csipFilter with page request $pageRequest")
    return findAllIds(
      fromDate = csipFilter.fromDate?.atStartOfDay(),
      toDate = csipFilter.toDate?.plusDays(1)?.atStartOfDay(),
      pageRequest,
    ).map { CSIPIdResponse(it) }
  }

  fun findAllIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageRequest: Pageable,
  ): Page<Long> =

    if (fromDate == null && toDate == null) {
      csipRepository.findAllCSIPIds(pageRequest)
    } else {
      // optimisation: only do the complex SQL if we have a filter
      // typically we won't when run in production
      csipRepository.findAllCSIPIds(fromDate, toDate, pageRequest)
    }

  fun getCSIPCount(): Long = csipRepository.count()

  fun deleteCSIP(csipId: Long) {
    csipRepository.findByIdOrNull(csipId)?.also {
      csipRepository.deleteById(csipId)
    }
      ?: log.info("CSIP deletion request for: $csipId ignored. CSIP does not exist")
  }

  private fun UpsertCSIPRequest.toCreateCSIPReport(): CSIPReport {
    val latestBooking = findLatestBooking(offenderNo)
    return CSIPReport(
      offenderBooking = latestBooking,
      originalAgencyId = latestBooking.location?.id,
      rootOffender = latestBooking.rootOffender,
      incidentDate = incidentDate,
      // TODO Check that the incident Date is also set on this field - or it is just 000000 date?
      incidentTime = incidentTime?.atDate(incidentDate),
      type = lookupIncidentType(typeCode),
      location = lookupLocation(locationCode),
      areaOfWork = lookupAreaOfWork(areaOfWorkCode),
      reportedBy = reportedBy,
      reportedDate = reportedDate,
      logNumber = logNumber,
      proActiveReferral = proActiveReferral,
      staffAssaulted = staffAssaulted,
      staffAssaultedName = staffAssaultedName,
      createUsername = createUsername,
    )
  }

  private fun updateCSIPReport(request: UpsertCSIPRequest): CSIPReport =
    csipRepository.findByIdOrNull(request.id!!)?.apply {
      incidentDate = request.incidentDate
      incidentTime = request.incidentTime?.atDate(incidentDate)
      type = lookupIncidentType(request.typeCode)
      location = lookupLocation(request.locationCode)
      areaOfWork = lookupAreaOfWork(request.areaOfWorkCode)
      reportedBy = request.reportedBy
      reportedDate = request.reportedDate
      logNumber = request.logNumber
      proActiveReferral = request.proActiveReferral
      staffAssaulted = request.staffAssaulted
      staffAssaultedName = request.staffAssaultedName
    } ?: throw NotFoundException("CSIP Report with id=${request.id} does not exist")

  fun lookupIncidentType(code: String) =
    typeRepository.findByIdOrNull(CSIPIncidentType.pk(code)) ?: throw BadDataException("Incident type $code not found")
  fun lookupLocation(code: String) =
    locationRepository.findByIdOrNull(CSIPIncidentLocation.pk(code)) ?: throw BadDataException("Location type $code not found")
  fun lookupAreaOfWork(code: String) =
    areaOfWorkRepository.findByIdOrNull(CSIPAreaOfWork.pk(code)) ?: throw BadDataException("Area of work type $code not found")
}
