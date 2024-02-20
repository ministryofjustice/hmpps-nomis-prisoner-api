package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisData
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

class IncidentResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private lateinit var offenderParty: Offender
  private lateinit var partyStaff1: Staff
  private lateinit var reportingStaff1: Staff
  private lateinit var reportingStaff2: Staff
  private lateinit var questionnaire1: Questionnaire
  private lateinit var questionnaire2: Questionnaire
  private lateinit var incident1: Incident

  private lateinit var incident2: Incident
  private lateinit var incident3: Incident
  private lateinit var requirementRecordingStaff: Staff
  private lateinit var responseRecordingStaff: Staff

  @BeforeEach
  internal fun createIncidents() {
    nomisDataBuilder.build {
      setUpStaffAndOffenders()
      setUpQuestionnaires()

      incident1 = incident(
        title = "Fight in the cell",
        description = "Offenders were injured and furniture was damaged.",
        reportingStaff = reportingStaff1,
        questionnaire = questionnaire1,
      ) {
        staffParty(staff = partyStaff1, role = "WIT")
        staffParty(staff = reportingStaff2)
        offenderParty(offenderBooking = offenderParty.latestBooking(), outcome = "POR")

        requirement("Update the name", recordingStaff = requirementRecordingStaff, prisonId = "MDI")
        requirement("Ensure all details are added", recordingStaff = requirementRecordingStaff, prisonId = "MDI")

        question(question = questionnaire1.questions[3])
        question(question = questionnaire1.questions[2]) {
          response(answer = questionnaire1.questions[2].answers[0], comment = "Multiple tools were found", recordingStaff = responseRecordingStaff)
        }
        question(question = questionnaire1.questions[1]) {
          response(answer = questionnaire1.questions[1].answers[0], recordingStaff = responseRecordingStaff)
          response(answer = questionnaire1.questions[1].answers[2], comment = "Large Crow bar", recordingStaff = responseRecordingStaff)
        }

        history(questionnaire = questionnaire2, changeStaff = reportingStaff1) {
          historyQuestion(question = questionnaire2.questions[2]) {
            historyResponse(answer = questionnaire2.questions[2].answers[0], comment = "Lots of staff", recordingStaff = reportingStaff2)
          }
          historyQuestion(question = questionnaire2.questions[1]) {
            historyResponse(answer = questionnaire2.questions[1].answers[0], comment = "A1 - Hurt Arm", recordingStaff = reportingStaff2)
            historyResponse(answer = questionnaire2.questions[1].answers[2], comment = "A3 - Hurt Head", recordingStaff = reportingStaff2)
          }
        }
      }
      // Incident and incident history with missing questionnaire answer - to mimic Nomis data
      incident2 = incident(reportingStaff = reportingStaff1, questionnaire = questionnaire1) {
        question(question = questionnaire1.questions[1]) {
          response(recordingStaff = responseRecordingStaff, comment = "Hammer")
          response(answer = questionnaire1.questions[1].answers[2], comment = "Large Crow bar", recordingStaff = responseRecordingStaff)
        }
        history(questionnaire = questionnaire2, changeStaff = reportingStaff1) {
          historyQuestion(question = questionnaire2.questions[2]) {
            historyResponse(comment = "one staff", recordingStaff = reportingStaff2)
          }
        }
      }
      incident3 = incident(reportingStaff = reportingStaff2, questionnaire = questionnaire1)
    }
  }

  fun NomisData.setUpStaffAndOffenders() {
    partyStaff1 = staff(firstName = "JIM", lastName = "PARTYSTAFF") {
      account(username = "JIIMPARTYSTAFF")
    }
    reportingStaff1 = staff(firstName = "FRED", lastName = "STAFF") {
      account(username = "FREDSTAFF")
    }
    reportingStaff2 = staff(firstName = "JANE", lastName = "STAFF") {
      account(username = "JANESTAFF")
    }
    requirementRecordingStaff = staff(firstName = "PETER", lastName = "STAFF") {
      account(username = "PETERSTAFF")
    }
    responseRecordingStaff = staff(firstName = "ALBERT", lastName = "STAFF") {
      account(username = "ALBERTSTAFF")
    }
    offenderParty = offender(nomsId = "A1234TT", firstName = "Bob", lastName = "Smith") {
      booking(agencyLocationId = "MDI")
    }
  }

  fun NomisData.setUpQuestionnaires() {
    questionnaire1 = questionnaire(code = "ESCAPE_EST", description = "Escape Questionnaire") {
      val question4 = questionnaireQuestion(question = "Q4: Any Damage amount?") {
        questionnaireAnswer(answer = "Q4A1: Enter Damage Amount in Pounds")
      }
      val question13 = questionnaireQuestion(question = "Q3: What tools were used?", multipleAnswers = true) {
        questionnaireAnswer(answer = "Q3A1: Wire cutters", nextQuestion = question4)
        questionnaireAnswer(answer = "Q3A2: Spade", nextQuestion = question4)
        questionnaireAnswer(answer = "Q3A3: Crow bar", nextQuestion = question4)
      }
      val question12 = questionnaireQuestion(question = "Q2: Were tools used?") {
        questionnaireAnswer(answer = "Q2A1: Yes", nextQuestion = question13)
        questionnaireAnswer(answer = "Q2A2: No")
      }
      questionnaireQuestion(question = "Q1: Were the police informed of the incident?") {
        questionnaireAnswer(answer = "Q1A1: Yes", nextQuestion = question12)
        questionnaireAnswer(answer = "Q1A2: No", nextQuestion = question12)
      }
    }

    questionnaire2 = questionnaire(code = "FIRE", description = "Questionnaire for fire", active = false, listSequence = 2) {
      val question23 = questionnaireQuestion(question = "Q23: Were prisoners involved?")
      offenderRole("ESC")
      offenderRole("FIGHT")
      val question22 = questionnaireQuestion(question = "Q22: Body parts injured") {
        questionnaireAnswer(answer = "Q22A1: Arm", nextQuestion = question23)
        questionnaireAnswer(answer = "Q22A2: Leg")
        questionnaireAnswer(answer = "Q22A2: Head")
      }
      questionnaireQuestion(question = "Q21: Were staff involved?") {
        questionnaireAnswer(answer = "Q21A1: Yes", nextQuestion = question22)
        questionnaireAnswer(answer = "Q21A2: No")
      }
    }
  }

  @AfterEach
  internal fun deleteIncidents() {
    repository.delete(incident1)
    repository.delete(incident2)
    repository.delete(incident3)

    repository.delete(questionnaire1)
    repository.delete(questionnaire2)

    repository.delete(requirementRecordingStaff)
    repository.delete(responseRecordingStaff)
    repository.delete(partyStaff1)
    repository.delete(offenderParty)
    repository.delete(reportingStaff1)
    repository.delete(reportingStaff2)
  }

  @Nested
  @DisplayName("GET /incidents/ids")
  inner class GetIncidentIds {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/incidents/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/incidents/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/incidents/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `get all incident ids - no filter specified`() {
      webTestClient.get().uri("/incidents/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(3)
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/incidents/ids")
          .queryParam("size", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `can request a different page`() {
      webTestClient.get().uri {
        it.path("/incidents/ids")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }
  }

  @Nested
  @DisplayName("GET /incidents/{incidentId}")
  inner class GetIncident {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/incidents/${incident1.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `unknown incident should return not found`() {
      webTestClient.get().uri("/incidents/999999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          Assertions.assertThat(it).contains("Not Found: Incident with id=999999 does not exist")
        }
    }

    @Test
    fun `will return an incident by Id`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(incident1.id)
        .jsonPath("title").isEqualTo("Fight in the cell")
        .jsonPath("description").isEqualTo("Offenders were injured and furniture was damaged.")
        .jsonPath("status").isEqualTo("AWAN")
        .jsonPath("type").isEqualTo("ESCAPE_EST")
        .jsonPath("prison.code").isEqualTo("BXI")
        .jsonPath("prison.description").isEqualTo("BRIXTON")
        .jsonPath("lockedResponse").isEqualTo(false)
        .jsonPath("incidentDateTime").isEqualTo("2023-12-30T13:45:00")
        .jsonPath("reportedStaff.staffId").isEqualTo(reportingStaff1.id)
        .jsonPath("reportedStaff.username").isEqualTo("FREDSTAFF")
        .jsonPath("reportedStaff.firstName").isEqualTo("FRED")
        .jsonPath("reportedStaff.lastName").isEqualTo("STAFF")
        .jsonPath("reportedDateTime").isEqualTo("2024-01-02T09:30:00")
    }

    @Test
    fun `will return staff party information for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(incident1.id)
        .jsonPath("staffParties[0].staff.staffId").isEqualTo(partyStaff1.id)
        .jsonPath("staffParties[0].staff.username").isEqualTo("JIIMPARTYSTAFF")
        .jsonPath("staffParties[0].staff.firstName").isEqualTo("JIM")
        .jsonPath("staffParties[0].staff.lastName").isEqualTo("PARTYSTAFF")
        .jsonPath("staffParties[0].role.code").isEqualTo("WIT")
        .jsonPath("staffParties[0].role.description").isEqualTo("Witness")
        .jsonPath("staffParties[1].staff.username").isEqualTo("JANESTAFF")
        .jsonPath("staffParties[1].role.code").isEqualTo("VICT")
        .jsonPath("staffParties[1].role.description").isEqualTo("Victim")
    }

    @Test
    fun `will return offender party information for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(incident1.id)
        .jsonPath("offenderParties[0].offender.offenderNo").isEqualTo("A1234TT")
        .jsonPath("offenderParties[0].offender.firstName").isEqualTo("Bob")
        .jsonPath("offenderParties[0].offender.lastName").isEqualTo("Smith")
        .jsonPath("offenderParties[0].outcome.code").isEqualTo("POR")
        .jsonPath("offenderParties[0].outcome.description").isEqualTo("Placed on Report")
        .jsonPath("offenderParties[0].role.code").isEqualTo("VICT")
        .jsonPath("offenderParties[0].role.description").isEqualTo("Victim")
    }

    @Test
    fun `will return incident requirement details for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("requirements.length()").isEqualTo(2)
        .jsonPath("id").isEqualTo(incident1.id)
        .jsonPath("requirements[0].comment").isEqualTo("Update the name")
        .jsonPath("requirements[0].staff.firstName").isEqualTo("PETER")
        .jsonPath("requirements[0].staff.lastName").isEqualTo("STAFF")
        .jsonPath("requirements[0].prisonId").isEqualTo("MDI")
        .jsonPath("requirements[1].comment").isEqualTo("Ensure all details are added")
    }

    @Test
    fun `will return incident questions for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("questions.length()").isEqualTo(3)
        .jsonPath("id").isEqualTo(incident1.id)
        .jsonPath("questions[0].question").isEqualTo("Q1: Were the police informed of the incident?")
    }

    @Test
    fun `will return incident responses for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("questions.length()").isEqualTo(3)
        .jsonPath("id").isEqualTo(incident1.id)
        .jsonPath("questions[0].question").isEqualTo("Q1: Were the police informed of the incident?")
        .jsonPath("questions[0].answers").isEmpty
        .jsonPath("questions[1].question").isEqualTo("Q2: Were tools used?")
        .jsonPath("questions[1].answers[0].answer").isEqualTo("Q2A1: Yes")
        .jsonPath("questions[1].answers[0].comment").isEqualTo("Multiple tools were found")
        .jsonPath("questions[2].question").isEqualTo("Q3: What tools were used?")
        .jsonPath("questions[2].answers[0].answer").isEqualTo("Q3A1: Wire cutters")
        .jsonPath("questions[2].answers[0].comment").doesNotExist()
        .jsonPath("questions[2].answers[1].answer").isEqualTo("Q3A3: Crow bar")
        .jsonPath("questions[2].answers[1].comment").isEqualTo("Large Crow bar")
    }

    @Test
    fun `will return incident responses for an incident with missing questionnaire answers - Nomis missing data test`() {
      webTestClient.get().uri("/incidents/${incident2.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("questions.length()").isEqualTo(1)
        .jsonPath("id").isEqualTo(incident2.id)
        .jsonPath("questions[0].question").isEqualTo("Q3: What tools were used?")
        .jsonPath("questions[0].answers[0].answer").doesNotExist()
        .jsonPath("questions[0].answers[0].comment").isEqualTo("Hammer")
        .jsonPath("questions[0].answers[1].answer").isEqualTo("Q3A3: Crow bar")
        .jsonPath("questions[0].answers[1].comment").isEqualTo("Large Crow bar")
    }

    @Test
    fun `will return incident history for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(incident1.id)
        .jsonPath("history[0].questionnaireId").isEqualTo(questionnaire2.id)
        .jsonPath("history[0].questionnaire").isEqualTo("FIRE")
        .jsonPath("history[0].description").isEqualTo("Questionnaire for fire")
    }

    @Test
    fun `will return incident history questions for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(incident1.id)
        .jsonPath("history[0].questions.length()").isEqualTo(2)
        .jsonPath("history[0].questions[0].question").isEqualTo("Q21: Were staff involved?")
    }

    @Test
    fun `will return incident history responses for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(incident1.id)
        .jsonPath("history[0].questionnaire").isEqualTo("FIRE")
        .jsonPath("history[0].description").isEqualTo("Questionnaire for fire")
        .jsonPath("history[0].questions[0].answers.length()").isEqualTo(1)
        .jsonPath("history[0].questions[0].answers[0].answer").isEqualTo("Q21A1: Yes")
        .jsonPath("history[0].questions[0].answers[0].comment").isEqualTo("Lots of staff")
        .jsonPath("history[0].questions[1].answers.length()").isEqualTo(2)
        .jsonPath("history[0].questions[1].answers[0].answer").isEqualTo("Q22A1: Arm")
        .jsonPath("history[0].questions[1].answers[0].comment").isEqualTo("A1 - Hurt Arm")
        .jsonPath("history[0].questions[1].answers[1].answer").isEqualTo("Q22A2: Head")
        .jsonPath("history[0].questions[1].answers[1].comment").isEqualTo("A3 - Hurt Head")
    }

    @Test
    fun `will return incident history responses with missing questionnaire answers - Nomis missing data test`() {
      webTestClient.get().uri("/incidents/${incident2.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(incident2.id)
        .jsonPath("history[0].questionnaire").isEqualTo("FIRE")
        .jsonPath("history[0].description").isEqualTo("Questionnaire for fire")
        .jsonPath("history[0].questions[0].answers.length()").isEqualTo(1)
        .jsonPath("history[0].questions[0].answers[0].answer").doesNotExist()
        .jsonPath("history[0].questions[0].answers[0].comment").isEqualTo("one staff")
    }
  }
}
