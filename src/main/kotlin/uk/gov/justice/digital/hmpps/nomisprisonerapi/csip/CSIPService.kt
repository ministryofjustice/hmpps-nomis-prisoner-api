package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.DocumentService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReportRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import java.time.LocalDateTime

@Service
@Transactional
class CSIPService(
  private val csipRepository: CSIPReportRepository,
  private val documentService: DocumentService,
  private val offenderRepository: OffenderRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val csipTemplates = listOf("CSIPA1_HMP", "CSIPA1_FNP", "CSIPA2_HMP", "CSIPA2_FNP", "CSIPA3_HMP", "CSIPA3_FNP")
  }

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
}
