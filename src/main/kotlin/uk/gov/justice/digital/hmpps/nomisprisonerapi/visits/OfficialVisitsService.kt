package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.usernamePreferringGeneralAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository

@Service
@Transactional
class OfficialVisitsService(
  private val visitRepository: VisitRepository,

) {
  fun getVisitIds(pageRequest: Pageable): Page<VisitIdResponse> = visitRepository.findAllOfficialVisitsIds(pageRequest).map {
    VisitIdResponse(
      visitId = it.id,
    )
  }

  @Suppress("unused")
  fun getVisit(visitId: Long): OfficialVisitResponse {
    val officialVisit = (visitRepository.findByIdOrNull(visitId) ?: throw NotFoundException("Visit with id $visitId not found")).takeIf { it.visitType.isOfficial() } ?: throw BadDataException("Visit with id $visitId is not an official visit")

    return with(officialVisit) {
      OfficialVisitResponse(
        visitId = id,
        // only one social visit has no slot
        visitSlotId = agencyVisitSlot!!.id,
        prisonId = location.id,
        offenderNo = offenderBooking.offender.nomsId,
        bookingId = offenderBooking.bookingId,
        currentTerm = offenderBooking.bookingSequence == 1,
        startDateTime = startDateTime,
        endDateTime = endDateTime,
        // only one social visit has no location
        internalLocationId = agencyInternalLocation!!.locationId,
        visitStatus = visitStatus.toCodeDescription(),
        visitOutcome = outcomeVisitor()?.eventStatus?.toCodeDescription(),
        outcomeReason = outcomeVisitor()?.outcomeReason?.toCodeDescription(),
        prisonerSearchType = searchLevel?.toCodeDescription(),
        visitorConcernText = visitorConcernText,
        commentText = commentText,
        overrideBanStaffUsername = overrideBanStaff?.usernamePreferringGeneralAccount(),
        visitors = emptyList(),
        audit = toAudit(),
      )
    }
  }
}
