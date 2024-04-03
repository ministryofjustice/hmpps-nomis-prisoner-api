package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPPlan
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPReportRepository
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff as JPAStaff

@Service
@Transactional
class CSIPService(
  private val csipRepository: CSIPReportRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getCSIP(csipId: Long): CSIPResponse? {
    return csipRepository.findByIdOrNull(csipId)?.toCSIPResponse()
      ?: throw NotFoundException("CSIP with id=$csipId does not exist")
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
}

private fun CSIPReport.toCSIPResponse(): CSIPResponse =
  CSIPResponse(
    id = id,
    offenderNo = offender.nomsId,
    bookingId = offenderBooking?.bookingId,
    type = type.toCodeDescription(),
    location = location.toCodeDescription(),
    areaOfWork = areaOfWork.toCodeDescription(),
    plans = plans.map { it.toPlanResponse() },

  )

private fun JPAStaff.toStaff() =
  Staff(
    staffId = id,
    firstName = firstName,
    lastName = lastName,
    username = accounts.maxByOrNull { it.type }?.username ?: "unknown",
  )

private fun Offender.toOffender() =
  Offender(
    offenderNo = nomsId,
    firstName = firstName,
    lastName = lastName,
  )

private fun CSIPPlan.toPlanResponse() =
  Plan(
    id = id,
    identifiedNeed = identifiedNeed,
    intervention = intervention,
  )
