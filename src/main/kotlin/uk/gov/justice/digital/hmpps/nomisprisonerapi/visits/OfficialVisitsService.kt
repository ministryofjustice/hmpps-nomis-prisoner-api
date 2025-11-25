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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import java.time.LocalDate

@Service
@Transactional
class OfficialVisitsService(
  private val visitRepository: VisitRepository,

) {
  fun getVisitIds(
    pageRequest: Pageable,
    prisonIds: List<String>,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): Page<VisitIdResponse> = if (prisonIds.isEmpty()) {
    if (fromDate == null && toDate == null) {
      visitRepository.findAllOfficialVisitsIds(pageRequest)
    } else {
      visitRepository.findAllOfficialVisitsIdsWithDateFilter(
        toDate = toDate?.atStartOfDay()?.plusDays(1),
        fromDate = fromDate?.atStartOfDay(),
        pageable = pageRequest,
      )
    }
  } else {
    visitRepository.findAllOfficialVisitsIdsWithDateAndPrisonFilter(
      toDate = toDate?.atStartOfDay()?.plusDays(1),
      fromDate = fromDate?.atStartOfDay(),
      prisonIds = prisonIds,
      pageable = pageRequest,
    )
  }.map {
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
        cancellationReason = outcomeVisitor()?.outcomeReason?.toCodeDescription(),
        prisonerAttendanceOutcome = outcomeVisitor()?.eventOutcome?.toCodeDescription(),
        prisonerSearchType = searchLevel?.toCodeDescription(),
        visitorConcernText = visitorConcernText,
        commentText = commentText,
        overrideBanStaffUsername = overrideBanStaff?.usernamePreferringGeneralAccount(),
        visitors = visitors.filter { it.person != null }.map { visitor ->
          OfficialVisitResponse.OfficialVisitor(
            id = visitor.id,
            personId = visitor.person!!.id,
            firstName = visitor.person!!.firstName,
            lastName = visitor.person!!.lastName,
            dateOfBirth = visitor.person!!.birthDate,
            leadVisitor = visitor.groupLeader,
            assistedVisit = visitor.assistedVisit,
            visitorAttendanceOutcome = visitor.eventOutcome?.toCodeDescription(),
            cancellationReason = visitor.outcomeReason?.toCodeDescription(),
            eventStatus = visitor.eventStatus?.toCodeDescription(),
            commentText = visitor.commentText,
            // TODO - look at performance of below - a better solution might be to map with filter/where clause in VisitVisitor entity rather than via Person
            relationships = visitor.person!!.contacts.filter { it.offenderBooking == this.offenderBooking }.sortedWith(latestOfficialContactFirst()).map {
              OfficialVisitResponse.OfficialVisitor.ContactRelationship(
                relationshipType = it.relationshipType.toCodeDescription(),
              )
            },
            audit = visitor.toAudit(),
          )
        },
        audit = toAudit(),
      )
    }
  }
}

private fun latestOfficialContactFirst() = compareByDescending<OffenderContactPerson> { it.relationshipType.code }.thenByDescending { it.createDatetime }
