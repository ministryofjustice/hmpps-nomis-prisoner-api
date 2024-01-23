package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentQuestion
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncidentRequirement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isOffenderParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.isStaffParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.offenderParty
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.IncidentRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.IncidentSpecification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.staffParty
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
      id = this.id,
      title = this.title,
      description = this.description,
      status = this.status.code,
      type = this.questionnaire.code,
      lockedResponse = this.lockedResponse,
      incidentDateTime = LocalDateTime.of(this.incidentDate, this.incidentTime),
      reportedStaff = this.reportingStaff.toStaff(),
      reportedDateTime = LocalDateTime.of(this.reportedDate, this.reportedTime),
      staffParties = this.staffParties().map { it.staffParty().toStaff() },
      offenderParties = this.offenderParties().map { it.offenderParty().toOffender() },
      requirements = this.requirements.map { it.toRequirement() },
      questions = this.questions.map { it.toQuestion() },
    )
}

private fun IncidentRequirement.toRequirement() =
  Requirement(
    staff = recordingStaff.toStaff(),
    comment = comment,
    date = recordedDate,
    prisonId = location.id,
  )
private fun IncidentQuestion.toQuestion() =
  Question(question = question.question)

private fun Incident.staffParties(): List<IncidentParty> =
  this.parties.filter { it.isStaffParty() }

private fun JPAStaff.toStaff() =
  Staff(
    staffId = id,
    firstName = firstName,
    lastName = lastName,
    username = accounts.maxByOrNull { it.type }?.username ?: "unknown",
  )

private fun Incident.offenderParties(): List<IncidentParty> =
  this.parties.filter { it.isOffenderParty() }

private fun Offender.toOffender() =
  Offender(
    offenderNo = nomsId,
    firstName = firstName,
    lastName = lastName,
  )
