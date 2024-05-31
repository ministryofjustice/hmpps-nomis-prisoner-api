package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.toCodeDescription
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentHistory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentOffenderParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentRequirement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStaffParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStatus.Companion.closedStatusValues
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStatus.Companion.openStatusValues
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentRepository
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff as JPAStaff

@Service
@Transactional
class IncidentService(
  private val incidentRepository: IncidentRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getIncident(incidentId: Long): IncidentResponse? {
    return incidentRepository.findByIdOrNull(incidentId)?.toIncidentResponse()
      ?: throw NotFoundException("Incident with id=$incidentId does not exist")
  }

  fun findIdsByFilter(pageRequest: Pageable, incidentFilter: IncidentFilter): Page<IncidentIdResponse> {
    log.info("Incident Id filter request : $incidentFilter with page request $pageRequest")
    return findAllIds(
      fromDate = incidentFilter.fromDate?.atStartOfDay(),
      toDate = incidentFilter.toDate?.plusDays(1)?.atStartOfDay(),
      pageRequest,
    ).map { IncidentIdResponse(it) }
  }

  fun findAllIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageRequest: Pageable,
  ) =
    if (fromDate == null && toDate == null) {
      incidentRepository.findAllIncidentIds(pageRequest)
    } else {
      // optimisation: only do the complex SQL if we have a filter
      // typically we won't when run in production
      incidentRepository.findAllIncidentIds(fromDate, toDate, pageRequest)
    }

  fun findAllIncidentAgencies() =
    incidentRepository.findAllIncidentAgencies().map { IncidentAgencyId(it) }

  fun getIncidentCountsForReconciliation(agencyId: String): IncidentsReconciliationResponse =
    IncidentsReconciliationResponse(
      agencyId = agencyId,
      incidentCount = incidentRepository.countsByAgency(agencyId, openStatusValues, closedStatusValues),
    )

  fun getOpenIncidentIdsForReconciliation(agencyId: String, pageRequest: Pageable) =
    incidentRepository.findAllIncidentIdsByAgencyAndStatus(agencyId, openStatusValues, pageRequest)
}

private fun Incident.toIncidentResponse(): IncidentResponse =
  IncidentResponse(
    incidentId = id,
    questionnaireId = questionnaire.id,
    title = title,
    description = description,
    status = status,
    type = questionnaire.code,
    agency = agency.toCodeDescription(),
    lockedResponse = lockedResponse,
    incidentDateTime = LocalDateTime.of(incidentDate, incidentTime),
    followUpDate = followUpDate,
    reportingStaff = reportingStaff.toStaff(),
    reportedDateTime = LocalDateTime.of(reportedDate, reportedTime),
    staffParties = staffParties.map { it.toStaffParty() },
    offenderParties = offenderParties.map { it.toOffenderParty() },
    requirements = requirements.map { it.toRequirement() },
    questions = questions.map { it.toQuestionResponse() },
    history = incidentHistory.map { it.toHistoryResponse() },
    createDateTime = createDatetime,
    createdBy = createUsername,
    lastModifiedDateTime = lastModifiedDateTime,
    lastModifiedBy = lastModifiedUsername,
  )

private fun IncidentRequirement.toRequirement() =
  Requirement(
    staff = recordingStaff.toStaff(),
    comment = comment,
    date = recordedDate,
    agencyId = agency.id,
    createDateTime = createDatetime,
    createdBy = createUsername,
    lastModifiedDateTime = lastModifiedDateTime,
    lastModifiedBy = lastModifiedUsername,
  )

private fun IncidentQuestion.toQuestionResponse() =
  Question(
    questionId = question.id,
    sequence = id.questionSequence,
    question = question.questionText,
    createDateTime = createDatetime,
    createdBy = createUsername,
    answers = responses.map { response ->
      Response(
        questionResponseId = response.answer?.id,
        sequence = response.id.responseSequence,
        answer = response.answer?.answerText,
        comment = response.comment,
        responseDate = response.responseDate,
        recordingStaff = response.recordingStaff.toStaff(),
        createDateTime = response.createDatetime,
        createdBy = response.createUsername,
        lastModifiedDateTime = response.lastModifiedDateTime,
        lastModifiedBy = response.lastModifiedUsername,
      )
    },
  )

private fun IncidentHistory.toHistoryResponse() =
  History(
    questionnaireId = questionnaire.id,
    type = questionnaire.code,
    description = questionnaire.description,
    incidentChangeDate = incidentChangeDate,
    incidentChangeStaff = incidentChangeStaff.toStaff(),
    createDateTime = createDatetime,
    createdBy = createUsername,
    questions = questions.map { historyQuestion ->
      HistoryQuestion(
        questionId = historyQuestion.question.id,
        sequence = historyQuestion.id.questionSequence,
        question = historyQuestion.question.questionText,
        answers = historyQuestion.responses.map { response ->
          HistoryResponse(
            questionResponseId = response.answer?.id,
            responseSequence = response.id.responseSequence,
            answer = response.answer?.answerText,
            responseDate = response.responseDate,
            comment = response.comment,
            recordingStaff = response.recordingStaff.toStaff(),
          )
        },
      )
    },
  )

private fun IncidentStaffParty.toStaffParty() =
  StaffParty(
    staff = staff.toStaff(),
    role = role.toCodeDescription(),
    comment = comment,
    createDateTime = createDatetime,
    createdBy = createUsername,
    lastModifiedDateTime = lastModifiedDateTime,
    lastModifiedBy = lastModifiedUsername,
  )

private fun JPAStaff.toStaff() =
  Staff(
    staffId = id,
    firstName = firstName,
    lastName = lastName,
    username = accounts.maxByOrNull { it.type }?.username ?: "unknown",
  )

private fun IncidentOffenderParty.toOffenderParty() =
  OffenderParty(
    offender = offenderBooking.offender.toOffender(),
    role = role.toCodeDescription(),
    outcome = outcome?.toCodeDescription(),
    comment = comment,
    createDateTime = createDatetime,
    createdBy = createUsername,
    lastModifiedDateTime = lastModifiedDateTime,
    lastModifiedBy = lastModifiedUsername,
  )

private fun Offender.toOffender() =
  Offender(
    offenderNo = nomsId,
    firstName = firstName,
    lastName = lastName,
  )
