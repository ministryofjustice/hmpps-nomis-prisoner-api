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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentOffenderParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentRequirement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentStaffParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.IncidentSpecification
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
    return incidentRepository.findAll(IncidentSpecification(incidentFilter), pageRequest)
      .map { IncidentIdResponse(it.id) }
  }

  private fun Incident.toIncidentResponse(): IncidentResponse =
    IncidentResponse(
      id = id,
      title = title,
      description = description,
      status = status.code,
      type = questionnaire.code,
      lockedResponse = lockedResponse,
      incidentDateTime = LocalDateTime.of(incidentDate, incidentTime),
      reportedStaff = reportingStaff.toStaff(),
      reportedDateTime = LocalDateTime.of(reportedDate, reportedTime),
      staffParties = staffParties.map { it.toStaffParty() },
      offenderParties = offenderParties.map { it.toOffenderParty() },
      requirements = requirements.map { it.toRequirement() },
      questions = questions.map { it.toQuestionResponse() },
    )
}

private fun IncidentRequirement.toRequirement() =
  Requirement(
    staff = recordingStaff.toStaff(),
    comment = comment,
    date = recordedDate,
    prisonId = location.id,
  )

private fun IncidentQuestion.toQuestionResponse() =
  Question(
    sequence = id.questionSequence,
    question = question.questionText,
    answers = responses.map { response ->
      Response(
        id = response.answer.id,
        answer = response.answer.answerText,
        comment = response.comment,
      )
    },
  )

private fun IncidentStaffParty.toStaffParty() =
  StaffParty(
    staff = staff.toStaff(),
    role = role.toCodeDescription(),
    comment = comment,
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
  )

private fun Offender.toOffender() =
  Offender(
    offenderNo = nomsId,
    firstName = firstName,
    lastName = lastName,
  )
