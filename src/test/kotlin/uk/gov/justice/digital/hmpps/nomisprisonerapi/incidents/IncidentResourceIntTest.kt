package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisData
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalDateTime

class IncidentResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private lateinit var offender1: Offender
  private lateinit var offender2: Offender
  private lateinit var partyStaff1: Staff
  private lateinit var reportingStaff1: Staff
  private lateinit var reportingStaff2: Staff
  private lateinit var questionnaire1: Questionnaire
  private lateinit var questionnaire2: Questionnaire
  private lateinit var incident1: Incident

  private lateinit var incident2: Incident
  private lateinit var incident3: Incident
  private lateinit var incident4: Incident
  private lateinit var requirementRecordingStaff: Staff
  private lateinit var responseRecordingStaff: Staff
  private var bookingId2: Long = 0

  private var currentId: Long = 0

  private val upsertIncidentRequest: () -> UpsertIncidentRequest = {
    UpsertIncidentRequest(
      title = "Some title",
      description = "Some description",
      descriptionAmendments = emptyList(),
      location = "MDI",
      statusCode = "AWAN",
      typeCode = questionnaire1.code,
      incidentDateTime = LocalDateTime.parse("2025-12-20T01:02:03"),
      reportedDateTime = LocalDateTime.parse("2025-12-20T01:02:03"),
      reportedBy = reportingStaff1.accounts[0].username,
    )
  }

  @BeforeEach
  internal fun createIncidents() {
    nomisDataBuilder.build {
      setUpStaffAndOffenders()
      setUpQuestionnaires()

      incident1 = incident(
        id = ++currentId,
        title = "Fight in the cell",
        description = "Offenders were injured and furniture was damaged.",
        reportingStaff = reportingStaff1,
        questionnaire = questionnaire1,
      ) {
        staffParty(staff = partyStaff1, role = "WIT")
        staffParty(staff = reportingStaff2)
        offenderParty(offenderBooking = offender1.latestBooking(), outcome = "POR")

        requirement("Update the name", recordingStaff = requirementRecordingStaff, locationId = "MDI", recordedDate = LocalDateTime.parse("2025-12-20T01:02:03"))
        requirement("Ensure all details are added", recordingStaff = requirementRecordingStaff, locationId = "MDI")

        question(question = questionnaire1.questions[3])
        question(question = questionnaire1.questions[2]) {
          response(answer = questionnaire1.questions[2].answers[0], responseDate = LocalDate.parse("2025-12-20"), comment = "Multiple tools were found", recordingStaff = responseRecordingStaff)
        }
        question(question = questionnaire1.questions[1]) {
          response(answer = questionnaire1.questions[1].answers[0], recordingStaff = responseRecordingStaff)
          response(answer = questionnaire1.questions[1].answers[2], comment = "Large Crow bar", recordingStaff = responseRecordingStaff)
        }

        history(questionnaire = questionnaire2, changeStaff = reportingStaff1) {
          historyQuestion(question = questionnaire2.questions[2]) {
            historyResponse(answer = questionnaire2.questions[2].answers[0], responseDate = LocalDate.parse("2024-11-20"), comment = "Lots of staff", recordingStaff = reportingStaff2)
          }
          historyQuestion(question = questionnaire2.questions[1]) {
            historyResponse(answer = questionnaire2.questions[1].answers[0], comment = "A1 - Hurt Arm", recordingStaff = reportingStaff2)
            historyResponse(answer = questionnaire2.questions[1].answers[2], comment = "A3 - Hurt Head", recordingStaff = reportingStaff2)
          }
        }
      }
      // Incident and incident history with missing questionnaire answer - to mimic Nomis data
      incident2 = incident(id = ++currentId, reportingStaff = reportingStaff1, questionnaire = questionnaire1, locationId = "MDI") {
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
      incident3 = incident(id = ++currentId, reportingStaff = reportingStaff2, questionnaire = questionnaire1, incidentStatus = "CLOSE")
      incident4 = incident(id = ++currentId, reportingStaff = reportingStaff2, questionnaire = questionnaire1)
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
    offender1 = offender(nomsId = "A1234TT", firstName = "Bob", lastName = "Smith") {
      booking(agencyLocationId = "MDI")
    }
    offender2 = offender(nomsId = "A1234YY", firstName = "Bob", lastName = "Smith") {
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
    repository.deleteAllIncidents()
    repository.deleteAllQuestionnaires()
    repository.deleteStaff()
    repository.deleteOffenders()
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
        .jsonPath("$.numberOfElements").isEqualTo(4)
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
        .jsonPath("totalElements").isEqualTo(4)
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
        .jsonPath("totalElements").isEqualTo(4)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
        .jsonPath("content[0].incidentId").isEqualTo(incident3.id)
        .jsonPath("content[1].incidentId").isEqualTo(incident4.id)
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
          assertThat(it).contains("Not Found: Incident with id=999999 does not exist")
        }
    }

    @Test
    fun `will return an incident by Id`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("incidentId").isEqualTo(incident1.id)
        .jsonPath("questionnaireId").isEqualTo(questionnaire1.id)
        .jsonPath("title").isEqualTo("Fight in the cell")
        .jsonPath("description").isEqualTo("Offenders were injured and furniture was damaged.")
        .jsonPath("status.code").isEqualTo("AWAN")
        .jsonPath("status.description").isEqualTo("Awaiting Analysis")
        .jsonPath("status.listSequence").isEqualTo(1)
        .jsonPath("status.standardUser").isEqualTo(true)
        .jsonPath("status.enhancedUser").isEqualTo(true)
        .jsonPath("type").isEqualTo("ESCAPE_EST")
        .jsonPath("agency.code").isEqualTo("BXI")
        .jsonPath("agency.description").isEqualTo("BRIXTON")
        .jsonPath("followUpDate").isEqualTo("2025-05-04")
        .jsonPath("lockedResponse").isEqualTo(false)
        .jsonPath("incidentDateTime").isEqualTo("2023-12-30T13:45:00")
        .jsonPath("reportingStaff.staffId").isEqualTo(reportingStaff1.id)
        .jsonPath("reportingStaff.username").isEqualTo("FREDSTAFF")
        .jsonPath("reportingStaff.firstName").isEqualTo("FRED")
        .jsonPath("reportingStaff.lastName").isEqualTo("STAFF")
        .jsonPath("reportedDateTime").isEqualTo("2024-01-02T09:30:00")
        .jsonPath("createDateTime").isNotEmpty
        .jsonPath("createdBy").isNotEmpty
    }

    @Test
    fun `will return staff party information for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("incidentId").isEqualTo(incident1.id)
        .jsonPath("staffParties[0].staff.staffId").isEqualTo(partyStaff1.id)
        .jsonPath("staffParties[0].sequence").isEqualTo(incident1.staffParties.first.id.partySequence)
        .jsonPath("staffParties[0].staff.username").isEqualTo("JIIMPARTYSTAFF")
        .jsonPath("staffParties[0].staff.firstName").isEqualTo("JIM")
        .jsonPath("staffParties[0].staff.lastName").isEqualTo("PARTYSTAFF")
        .jsonPath("staffParties[0].role.code").isEqualTo("WIT")
        .jsonPath("staffParties[0].role.description").isEqualTo("Witness")
        .jsonPath("staffParties[1].staff.username").isEqualTo("JANESTAFF")
        .jsonPath("staffParties[1].role.code").isEqualTo("VICT")
        .jsonPath("staffParties[1].role.description").isEqualTo("Victim")
        .jsonPath("staffParties[1].comment").isEqualTo("Staff said they witnessed everything")
        .jsonPath("staffParties[0].createDateTime").isNotEmpty
        .jsonPath("staffParties[0].createdBy").isNotEmpty
    }

    @Test
    fun `will return offender party information for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("incidentId").isEqualTo(incident1.id)
        .jsonPath("offenderParties[0].offender.offenderNo").isEqualTo("A1234TT")
        .jsonPath("offenderParties[0].offender.firstName").isEqualTo("Bob")
        .jsonPath("offenderParties[0].offender.lastName").isEqualTo("Smith")
        .jsonPath("offenderParties[0].sequence").isEqualTo(incident1.offenderParties.first().id.partySequence)
        .jsonPath("offenderParties[0].outcome.code").isEqualTo("POR")
        .jsonPath("offenderParties[0].outcome.description").isEqualTo("Placed on Report")
        .jsonPath("offenderParties[0].role.code").isEqualTo("VICT")
        .jsonPath("offenderParties[0].role.description").isEqualTo("Victim")
        .jsonPath("offenderParties[0].comment").isEqualTo("Offender said they witnessed everything")
        .jsonPath("offenderParties[0].createDateTime").isNotEmpty
        .jsonPath("offenderParties[0].createdBy").isNotEmpty
    }

    @Test
    fun `will return incident requirement details for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("requirements.length()").isEqualTo(2)
        .jsonPath("incidentId").isEqualTo(incident1.id)
        .jsonPath("requirements[0].comment").isEqualTo("Update the name")
        .jsonPath("requirements[0].recordedDate").isEqualTo("2025-12-20T01:02:03")
        .jsonPath("requirements[0].sequence").isEqualTo(incident1.requirements.first().id.requirementSequence)
        .jsonPath("requirements[0].staff.firstName").isEqualTo("PETER")
        .jsonPath("requirements[0].staff.lastName").isEqualTo("STAFF")
        .jsonPath("requirements[0].agencyId").isEqualTo("MDI")
        .jsonPath("requirements[0].createDateTime").isNotEmpty
        .jsonPath("requirements[0].createdBy").isNotEmpty
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
        .jsonPath("incidentId").isEqualTo(incident1.id)
        .jsonPath("questions[0].questionId").isEqualTo(questionnaire1.questions[3].id)
        .jsonPath("questions[0].sequence").isEqualTo(incident1.questions.first.id.questionSequence)
        .jsonPath("questions[0].question").isEqualTo(questionnaire1.questions[3].questionText)
        .jsonPath("questions[0].question").isEqualTo("Q1: Were the police informed of the incident?")
        .jsonPath("questions[0].createDateTime").isNotEmpty
        .jsonPath("questions[0].createdBy").isNotEmpty
    }

    @Test
    fun `will return incident responses for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("questions.length()").isEqualTo(3)
        .jsonPath("incidentId").isEqualTo(incident1.id)
        .jsonPath("questions[0].question").isEqualTo("Q1: Were the police informed of the incident?")
        .jsonPath("questions[0].answers").isEmpty
        .jsonPath("questions[1].question").isEqualTo("Q2: Were tools used?")
        .jsonPath("questions[1].answers[0].answer").isEqualTo("Q2A1: Yes")
        .jsonPath("questions[1].answers[0].comment").isEqualTo("Multiple tools were found")
        .jsonPath("questions[1].answers[0].responseDate").isEqualTo("2025-12-20")
        .jsonPath("questions[1].answers[0].sequence").isEqualTo(0)
        .jsonPath("questions[2].question").isEqualTo("Q3: What tools were used?")
        .jsonPath("questions[2].answers[0].answer").isEqualTo("Q3A1: Wire cutters")
        .jsonPath("questions[2].answers[0].comment").doesNotExist()
        .jsonPath("questions[2].answers[0].sequence").isEqualTo(1)
        .jsonPath("questions[2].answers[1].answer").isEqualTo("Q3A3: Crow bar")
        .jsonPath("questions[2].answers[1].comment").isEqualTo("Large Crow bar")
        .jsonPath("questions[2].answers[1].sequence").isEqualTo(2)
        .jsonPath("questions[2].answers[1].createDateTime").isNotEmpty
        .jsonPath("questions[2].answers[1].createdBy").isNotEmpty
    }

    @Test
    fun `will return incident responses for an incident with missing questionnaire answers - Nomis missing data test`() {
      webTestClient.get().uri("/incidents/${incident2.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("questions.length()").isEqualTo(1)
        .jsonPath("incidentId").isEqualTo(incident2.id)
        .jsonPath("questions[0].question").isEqualTo("Q3: What tools were used?")
        .jsonPath("questions[0].answers[0].answer").doesNotExist()
        .jsonPath("questions[0].answers[0].comment").isEqualTo("Hammer")
        .jsonPath("questions[0].answers[0].sequence").isEqualTo(0)
        .jsonPath("questions[0].answers[1].questionResponseId").isEqualTo(questionnaire1.questions[1].answers[2].id)
        .jsonPath("questions[0].answers[1].sequence").isEqualTo(1)
        .jsonPath("questions[0].answers[1].answer").isEqualTo(questionnaire1.questions[1].answers[2].answerText)
        .jsonPath("questions[0].answers[1].answer").isEqualTo("Q3A3: Crow bar")
        .jsonPath("questions[0].answers[1].comment").isEqualTo("Large Crow bar")
        .jsonPath("questions[0].answers[1].recordingStaff.username").isEqualTo("ALBERTSTAFF")
        .jsonPath("questions[0].answers[1].recordingStaff.staffId").isEqualTo(responseRecordingStaff.id)
        .jsonPath("questions[0].answers[1].recordingStaff.firstName").isEqualTo("ALBERT")
        .jsonPath("questions[0].answers[1].recordingStaff.lastName").isEqualTo("STAFF")
    }

    @Test
    fun `will return incident history for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("incidentId").isEqualTo(incident1.id)
        .jsonPath("history[0].questionnaireId").isEqualTo(questionnaire2.id)
        .jsonPath("history[0].type").isEqualTo("FIRE")
        .jsonPath("history[0].description").isEqualTo("Questionnaire for fire")
        .jsonPath("history[0].createDateTime").isNotEmpty
        .jsonPath("history[0].createdBy").isNotEmpty
        .jsonPath("history[0].incidentChangeDateTime").isNotEmpty
        .jsonPath("history[0].incidentChangeStaff.username").isEqualTo("FREDSTAFF")
    }

    @Test
    fun `will return incident history questions for an incident`() {
      webTestClient.get().uri("/incidents/${incident1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("incidentId").isEqualTo(incident1.id)
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
        .jsonPath("incidentId").isEqualTo(incident1.id)
        .jsonPath("history[0].type").isEqualTo("FIRE")
        .jsonPath("history[0].description").isEqualTo("Questionnaire for fire")
        .jsonPath("history[0].questions[0].answers.length()").isEqualTo(1)
        .jsonPath("history[0].questions[0].answers[0].answer").isEqualTo("Q21A1: Yes")
        .jsonPath("history[0].questions[0].answers[0].questionResponseId").isEqualTo(questionnaire2.questions[2].answers[0].id)
        .jsonPath("history[0].questions[0].answers[0].answer").isEqualTo("Q21A1: Yes")
        .jsonPath("history[0].questions[0].answers[0].comment").isEqualTo("Lots of staff")
        .jsonPath("history[0].questions[0].answers[0].responseDate").isEqualTo("2024-11-20")
        .jsonPath("history[0].questions[0].answers[0].recordingStaff.username").isEqualTo("JANESTAFF")
        .jsonPath("history[0].questions[0].answers[0].recordingStaff.staffId").isEqualTo(reportingStaff2.id)
        .jsonPath("history[0].questions[0].answers[0].recordingStaff.firstName").isEqualTo("JANE")
        .jsonPath("history[0].questions[0].answers[0].recordingStaff.lastName").isEqualTo("STAFF")
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
        .jsonPath("incidentId").isEqualTo(incident2.id)
        .jsonPath("history[0].type").isEqualTo("FIRE")
        .jsonPath("history[0].description").isEqualTo("Questionnaire for fire")
        .jsonPath("history[0].questions[0].answers.length()").isEqualTo(1)
        .jsonPath("history[0].questions[0].answers[0].answer").doesNotExist()
        .jsonPath("history[0].questions[0].answers[0].comment").isEqualTo("one staff")
    }
  }

  @Nested
  @DisplayName("GET /incidents/booking/{bookingId}")
  inner class GetIncidentsForBooking {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/incidents/booking/${offender1.latestBooking().bookingId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/incidents/booking/${offender1.latestBooking().bookingId}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/incidents/booking/${offender1.latestBooking().bookingId}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `unknown incident should return not found`() {
      webTestClient.get().uri("/incidents/booking/999999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Prisoner with booking 999999 not found")
        }
    }

    @Test
    fun `booking with incident with no incident parties should return empty list`() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234YY") {
          bookingId2 = booking().bookingId
        }
      }

      webTestClient.get().uri("/incidents/booking/$bookingId2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(0)
    }

    @Test
    fun `booking with multiple incidents should return incidents list`() {
      nomisDataBuilder.build {
        incident1 = incident(
          id = ++currentId,
          title = "Fighting",
          description = "Offenders were causing trouble.",
          reportingStaff = reportingStaff1,
          questionnaire = questionnaire1,
        ) {
          offenderParty(offenderBooking = offender2.latestBooking(), outcome = "POR")
        }
        incident(
          id = ++currentId,
          title = "Fighting again",
          description = "causing trouble.",
          reportingStaff = reportingStaff1,
          questionnaire = questionnaire1,
        ) {
          offenderParty(offenderBooking = offender2.latestBooking(), outcome = "TRN")
        }
      }

      webTestClient.get().uri("/incidents/booking/${offender2.latestBooking().bookingId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(2)
        .jsonPath("[0].title").isEqualTo("Fighting")
        .jsonPath("[1].title").isEqualTo("Fighting again")
    }

    @Test
    fun `booking will correctly identify the incident for the booking`() {
      webTestClient.get().uri("/incidents/booking/${offender1.latestBooking().bookingId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
        .jsonPath("[0].title").isEqualTo("Fight in the cell")
    }
  }

  @Nested
  @DisplayName("GET /incidents/reconciliation/agencies")
  inner class GetIncidentAgencies {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/incidents/reconciliation/agencies")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/incidents/reconciliation/agencies")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/incidents/reconciliation/agencies")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `get all agencies for incidents`() {
      webTestClient.get().uri("/incidents/reconciliation/agencies")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(2)
        .jsonPath("[0].agencyId").isEqualTo("BXI")
        .jsonPath("[1].agencyId").isEqualTo("MDI")
    }
  }

  @Nested
  @DisplayName("GET /incidents/reconciliation/agency/{agencyId}/counts")
  inner class GetIncidentCountForAgency {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/incidents/reconciliation/agency/BXI/counts")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/incidents/reconciliation/agency/BXI/counts")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/incidents/reconciliation/agency/BXI/counts")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `get incident count at agency`() {
      webTestClient.get().uri("/incidents/reconciliation/agency/BXI/counts")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("agencyId").isEqualTo("BXI")
        .jsonPath("incidentCount.openIncidents").isEqualTo(2)
        .jsonPath("incidentCount.closedIncidents").isEqualTo(1)
    }

    @Test
    fun `get incident count at agency with none closed`() {
      webTestClient.get().uri("/incidents/reconciliation/agency/MDI/counts")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("agencyId").isEqualTo("MDI")
        .jsonPath("incidentCount.openIncidents").isEqualTo(1)
        .jsonPath("incidentCount.closedIncidents").isEqualTo(0)
    }
  }

  @Nested
  @DisplayName("GET /incidents/reconciliation/agency/{agencyId}/ids")
  inner class GetOpenIncidentIdsAtAgency {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/incidents/reconciliation/agency/MDI/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/incidents/reconciliation/agency/MDI/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/incidents/reconciliation/agency/MDI/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `get all open incident ids - no filter specified `() {
      webTestClient.get().uri {
        it.path("/incidents/reconciliation/agency/BXI/ids")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("numberOfElements").isEqualTo(2)
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/incidents/reconciliation/agency/BXI/ids")
          .queryParam("size", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(2)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(1)
        .jsonPath("size").isEqualTo(2)
        .jsonPath("content[0].incidentId").isEqualTo(incident1.id)
        .jsonPath("content[1].incidentId").isEqualTo(incident4.id)
    }

    @Test
    fun `can request a different page`() {
      webTestClient.get().uri {
        it.path("/incidents/reconciliation/agency/BXI/ids")
          .queryParam("size", "1")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(2)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(1)
    }
  }

  @Nested
  @DisplayName("PUT /incidents/{incidentId}")
  inner class UpsertIncident {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/incidents/123456")
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(upsertIncidentRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/incidents/123456")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(upsertIncidentRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/incidents/123456")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will fail if location can't be found`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                location = "UNKNOW",
              ),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Agency with id=UNKNOW does not exist")
          }
      }

      @Test
      fun `will fail if incident status code can't be found`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                statusCode = "UNKNOW",
              ),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Incident status with code=UNKNOW does not exist")
          }
      }

      @Test
      fun `will fail if questionnaire can't be found`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                typeCode = "UNKNOW",
              ),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Questionnaire with code=UNKNOW does not exist")
          }
      }

      @Test
      fun `will fail if reportedBy can't be found`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                reportedBy = "UNKNOW",
              ),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Staff user account UNKNOW not found")
          }
      }
    }

    @Nested
    inner class Create {
      @Test
      fun `will create an incident`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened",
                description = "and people had a fight",
                statusCode = "AWAN",
                typeCode = questionnaire1.code,
                location = "BXI",
                incidentDateTime = LocalDateTime.parse("2023-12-30T13:45:00"),
                reportedDateTime = LocalDateTime.parse("2024-01-02T09:30:00"),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/$currentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(currentId)
          .jsonPath("questionnaireId").isEqualTo(questionnaire1.id)
          .jsonPath("title").isEqualTo("Something happened")
          .jsonPath("description").isEqualTo("and people had a fight")
          .jsonPath("status.code").isEqualTo("AWAN")
          .jsonPath("status.description").isEqualTo("Awaiting Analysis")
          .jsonPath("status.listSequence").isEqualTo(1)
          .jsonPath("status.standardUser").isEqualTo(true)
          .jsonPath("status.enhancedUser").isEqualTo(true)
          .jsonPath("type").isEqualTo("ESCAPE_EST")
          .jsonPath("agency.code").isEqualTo("BXI")
          .jsonPath("agency.description").isEqualTo("BRIXTON")
          .jsonPath("followUpDate").doesNotExist()
          .jsonPath("lockedResponse").isEqualTo(false)
          .jsonPath("incidentDateTime").isEqualTo("2023-12-30T13:45:00")
          .jsonPath("reportingStaff.staffId").isEqualTo(reportingStaff1.id)
          .jsonPath("reportingStaff.username").isEqualTo("FREDSTAFF")
          .jsonPath("reportingStaff.firstName").isEqualTo("FRED")
          .jsonPath("reportingStaff.lastName").isEqualTo("STAFF")
          .jsonPath("reportedDateTime").isEqualTo("2024-01-02T09:30:00")
          .jsonPath("createDateTime").isNotEmpty
          .jsonPath("createdBy").isNotEmpty
      }

      @Test
      fun `will create an incident with amendments`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with amendments",
                description = "and people had a fight",
                descriptionAmendments = listOf(
                  UpsertDescriptionAmendmentRequest(
                    text = "amendment",
                    firstName = "Bob",
                    lastName = "Bloggs",
                    createdDateTime = LocalDateTime.parse("2023-12-30T13:45:00"),
                  ),
                  UpsertDescriptionAmendmentRequest(
                    text = "second amendment",
                    firstName = "Joe",
                    lastName = "Smith",
                    createdDateTime = LocalDateTime.parse("2024-12-30T13:45:00"),
                  ),
                ),
                requirements = listOf(
                  UpsertIncidentRequirementRequest(
                    comment = "missing some data",
                    date = LocalDateTime.parse("2024-12-30T13:45:00"),
                    username = reportingStaff1.accounts[0].username,
                    location = "MDI",
                  ),
                ),
                offenderParties = listOf(
                  UpsertOffenderPartyRequest(
                    prisonNumber = offender1.nomsId,
                    role = "ACTINV",
                    outcome = "ACCT",
                    comment = "This is a comment on the party",
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/$currentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(currentId)
          .jsonPath("title").isEqualTo("Something happened with amendments")
          .jsonPath("description").isEqualTo("and people had a fightUser:Bloggs,Bob Date:30-Dec-2023 13:45amendmentUser:Smith,Joe Date:30-Dec-2024 13:45second amendment")
      }

      @Test
      fun `will create an incident with requirements`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with requirements",
                requirements = listOf(
                  UpsertIncidentRequirementRequest(
                    comment = "missing some data",
                    date = LocalDateTime.parse("2024-12-30T13:45:00"),
                    username = reportingStaff1.accounts[0].username,
                    location = "MDI",
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/$currentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(currentId)
          .jsonPath("title").isEqualTo("Something happened with requirements")
          .jsonPath("requirements[0].comment").isEqualTo("missing some data")
          .jsonPath("requirements[0].recordedDate").isEqualTo("2024-12-30T13:45:00")
          .jsonPath("requirements[0].sequence").isEqualTo(0)
          .jsonPath("requirements[0].staff.firstName").isEqualTo("FRED")
          .jsonPath("requirements[0].staff.lastName").isEqualTo("STAFF")
          .jsonPath("requirements[0].agencyId").isEqualTo("MDI")
          .jsonPath("requirements[0].createDateTime").isNotEmpty
          .jsonPath("requirements[0].createdBy").isNotEmpty
          .jsonPath("requirements.length()").isEqualTo(1)
      }

      @Test
      fun `will create an incident with requirements comment over 240 chars`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with requirements",
                requirements = listOf(
                  UpsertIncidentRequirementRequest(
                    comment = "a".repeat(241),
                    date = LocalDateTime.parse("2024-12-30T13:45:00"),
                    username = reportingStaff1.accounts[0].username,
                    location = "MDI",
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/$currentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(currentId)
          .jsonPath("requirements[0].comment").isEqualTo("${"a".repeat(215)}... see DPS for full text")
      }

      @Test
      fun `will create an incident with parties`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with parties",
                offenderParties = listOf(
                  UpsertOffenderPartyRequest(
                    comment = "a party with some data",
                    prisonNumber = offender1.nomsId,
                    role = "VICT",
                    outcome = "POR",
                  ),
                ),
                staffParties = listOf(
                  UpsertStaffPartyRequest(
                    comment = "b party with some data",
                    username = reportingStaff2.accounts[0].username,
                    role = "INPOS",
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/$currentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(currentId)
          .jsonPath("title").isEqualTo("Something happened with parties")
          .jsonPath("offenderParties[0].offender.offenderNo").isEqualTo(offender1.nomsId)
          .jsonPath("offenderParties[0].offender.firstName").isEqualTo("Bob")
          .jsonPath("offenderParties[0].offender.lastName").isEqualTo("Smith")
          .jsonPath("offenderParties[0].sequence").isEqualTo(2000)
          .jsonPath("offenderParties[0].outcome.code").isEqualTo("POR")
          .jsonPath("offenderParties[0].outcome.description").isEqualTo("Placed on Report")
          .jsonPath("offenderParties[0].role.code").isEqualTo("VICT")
          .jsonPath("offenderParties[0].role.description").isEqualTo("Victim")
          .jsonPath("offenderParties[0].comment").isEqualTo("a party with some data")
          .jsonPath("offenderParties.length()").isEqualTo(1)
          .jsonPath("staffParties[0].staff.username").isEqualTo("JANESTAFF")
          .jsonPath("staffParties[0].staff.firstName").isEqualTo("JANE")
          .jsonPath("staffParties[0].staff.lastName").isEqualTo("STAFF")
          .jsonPath("staffParties[0].sequence").isEqualTo(1000)
          .jsonPath("staffParties[0].role.code").isEqualTo("INPOS")
          .jsonPath("staffParties[0].role.description").isEqualTo("In Possession")
          .jsonPath("staffParties[0].comment").isEqualTo("b party with some data")
          .jsonPath("staffParties.length()").isEqualTo(1)
      }

      @Test
      fun `will create an incident with parties comment over 240 chars`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with parties",
                offenderParties = listOf(
                  UpsertOffenderPartyRequest(
                    comment = "a".repeat(241),
                    prisonNumber = offender1.nomsId,
                    role = "VICT",
                    outcome = "POR",
                  ),
                ),
                staffParties = listOf(
                  UpsertStaffPartyRequest(
                    comment = "b".repeat(241),
                    username = reportingStaff1.accounts[0].username,
                    role = "INV",
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/$currentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(currentId)
          .jsonPath("offenderParties[0].comment").isEqualTo("${"a".repeat(215)}... see DPS for full text")
          .jsonPath("staffParties[0].comment").isEqualTo("${"b".repeat(215)}... see DPS for full text")
      }

      @Test
      fun `will create an incident with questions`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with questions",
                questions = listOf(
                  UpsertIncidentQuestionRequest(
                    questionId = questionnaire1.questions[0].id,
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/$currentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(currentId)
          .jsonPath("title").isEqualTo("Something happened with questions")
          .jsonPath("questions[0].questionId").isEqualTo(questionnaire1.questions[0].id)
          .jsonPath("questions[0].sequence").isEqualTo(1)
          .jsonPath("questions[0].question").isEqualTo(questionnaire1.questions[0].questionText)
          .jsonPath("questions[0].question").isEqualTo("Q4: Any Damage amount?")
          .jsonPath("questions.length()").isEqualTo(1)
      }

      @Test
      fun `will create an incident with responses`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with responses",
                questions = listOf(
                  UpsertIncidentQuestionRequest(
                    questionId = questionnaire1.questions[1].id,
                    responses = listOf(
                      UpsertIncidentResponseRequest(
                        answerId = questionnaire1.questions[1].answers[0].id,
                        comment = "a comment",
                        responseDate = LocalDate.parse("2010-03-02"),
                        recordingUsername = responseRecordingStaff.accounts[0].username,
                        sequence = 0,
                      ),
                      UpsertIncidentResponseRequest(
                        answerId = questionnaire1.questions[1].answers[1].id,
                        comment = null,
                        responseDate = null,
                        recordingUsername = responseRecordingStaff.accounts[0].username,
                        sequence = 1,
                      ),
                    ),
                  ),
                  UpsertIncidentQuestionRequest(
                    questionId = questionnaire1.questions[2].id,
                    responses = listOf(
                      UpsertIncidentResponseRequest(
                        answerId = questionnaire1.questions[2].answers[0].id,
                        comment = "a comment",
                        responseDate = LocalDate.parse("2010-03-02"),
                        recordingUsername = responseRecordingStaff.accounts[0].username,
                        sequence = 2,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/$currentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(currentId)
          .jsonPath("title").isEqualTo("Something happened with responses")
          .jsonPath("questions[0].questionId").isEqualTo(questionnaire1.questions[1].id)
          .jsonPath("questions[0].sequence").isEqualTo(1)
          .jsonPath("questions[0].answers[0].answer").isEqualTo("Q3A1: Wire cutters")
          .jsonPath("questions[0].answers[0].comment").isEqualTo("a comment")
          .jsonPath("questions[0].answers[0].responseDate").isEqualTo("2010-03-02")
          .jsonPath("questions[0].answers[0].sequence").isEqualTo(0)
          .jsonPath("questions[0].answers[1].answer").isEqualTo("Q3A2: Spade")
          .jsonPath("questions[0].answers[1].comment").doesNotExist()
          .jsonPath("questions[0].answers[1].responseDate").doesNotExist()
          .jsonPath("questions[0].answers[1].sequence").isEqualTo(1)
          .jsonPath("questions[1].sequence").isEqualTo(2)
          .jsonPath("questions[1].answers[0].answer").isEqualTo("Q2A1: Yes")
          .jsonPath("questions[1].answers[0].comment").isEqualTo("a comment")
          .jsonPath("questions[1].answers[0].responseDate").isEqualTo("2010-03-02")
          .jsonPath("questions[1].answers[0].sequence").isEqualTo(2)
          .jsonPath("questions.length()").isEqualTo(2)
      }

      @Test
      fun `will create an incident with responses comment over 240 chars`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with responses",
                questions = listOf(
                  UpsertIncidentQuestionRequest(
                    questionId = questionnaire1.questions[1].id,
                    responses = listOf(
                      UpsertIncidentResponseRequest(
                        answerId = questionnaire1.questions[1].answers[0].id,
                        comment = "a".repeat(241),
                        responseDate = LocalDate.parse("2010-03-02"),
                        recordingUsername = responseRecordingStaff.accounts[0].username,
                        sequence = 0,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/$currentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(currentId)
          .jsonPath("questions[0].answers[0].comment").isEqualTo("${"a".repeat(215)}... see DPS for full text")
      }

      @Test
      fun `will create an incident with history`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with history",
                history = listOf(
                  UpsertIncidentHistoryRequest(
                    typeCode = questionnaire1.code,
                    incidentChangeDateTime = LocalDateTime.parse("2012-03-01T10:20:30"),
                    incidentChangeUsername = reportingStaff2.accounts[0].username,
                    questions = listOf(
                      UpsertIncidentQuestionRequest(
                        questionId = questionnaire1.questions[1].id,
                        responses = listOf(
                          UpsertIncidentResponseRequest(
                            answerId = questionnaire1.questions[1].answers[0].id,
                            comment = "a comment",
                            responseDate = LocalDate.parse("2010-03-02"),
                            recordingUsername = responseRecordingStaff.accounts[0].username,
                            sequence = 0,
                          ),
                          UpsertIncidentResponseRequest(
                            answerId = questionnaire1.questions[1].answers[1].id,
                            comment = null,
                            responseDate = null,
                            recordingUsername = responseRecordingStaff.accounts[0].username,
                            sequence = 1,
                          ),
                        ),
                      ),
                      UpsertIncidentQuestionRequest(
                        questionId = questionnaire1.questions[2].id,
                        responses = listOf(
                          UpsertIncidentResponseRequest(
                            answerId = questionnaire1.questions[2].answers[0].id,
                            comment = "a comment",
                            responseDate = LocalDate.parse("2010-03-02"),
                            recordingUsername = responseRecordingStaff.accounts[0].username,
                            sequence = 2,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/$currentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(currentId)
          .jsonPath("title").isEqualTo("Something happened with history")
          .jsonPath("history[0].questionnaireId").isEqualTo(questionnaire1.id)
          .jsonPath("history[0].type").isEqualTo("ESCAPE_EST")
          .jsonPath("history[0].description").isEqualTo("Escape Questionnaire")
          .jsonPath("history[0].incidentChangeDateTime").isEqualTo("2012-03-01T10:20:30")
          .jsonPath("history[0].incidentChangeStaff.username").isEqualTo("JANESTAFF")
          .jsonPath("history[0].questions[0].questionId").isEqualTo(questionnaire1.questions[1].id)
          .jsonPath("history[0].questions[0].sequence").isEqualTo(0)
          .jsonPath("history[0].questions[0].answers[0].answer").isEqualTo("Q3A1: Wire cutters")
          .jsonPath("history[0].questions[0].answers[0].comment").isEqualTo("a comment")
          .jsonPath("history[0].questions[0].answers[0].responseDate").isEqualTo("2010-03-02")
          .jsonPath("history[0].questions[0].answers[1].answer").isEqualTo("Q3A2: Spade")
          .jsonPath("history[0].questions[0].answers[1].comment").doesNotExist()
          .jsonPath("history[0].questions[0].answers[1].responseDate").doesNotExist()
          .jsonPath("history[0].questions[1].answers[0].answer").isEqualTo("Q2A1: Yes")
          .jsonPath("history[0].questions[1].answers[0].comment").isEqualTo("a comment")
          .jsonPath("history[0].questions[1].answers[0].responseDate").isEqualTo("2010-03-02")
          .jsonPath("history[0].questions.length()").isEqualTo(2)
          .jsonPath("history.length()").isEqualTo(1)
      }

      @Test
      fun `will create an incident with history comment over 240 chars`() {
        webTestClient.put().uri("/incidents/${++currentId}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with history",
                history = listOf(
                  UpsertIncidentHistoryRequest(
                    typeCode = questionnaire1.code,
                    incidentChangeDateTime = LocalDateTime.parse("2012-03-01T10:20:30"),
                    incidentChangeUsername = reportingStaff2.accounts[0].username,
                    questions = listOf(
                      UpsertIncidentQuestionRequest(
                        questionId = questionnaire1.questions[1].id,
                        responses = listOf(
                          UpsertIncidentResponseRequest(
                            answerId = questionnaire1.questions[1].answers[0].id,
                            comment = "a".repeat(241),
                            responseDate = LocalDate.parse("2010-03-02"),
                            recordingUsername = responseRecordingStaff.accounts[0].username,
                            sequence = 0,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/$currentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(currentId)
          .jsonPath("title").isEqualTo("Something happened with history")
          .jsonPath("history[0].questions[0].answers[0].comment").isEqualTo("${"a".repeat(215)}... see DPS for full text")
      }
    }

    @Nested
    inner class Update {
      @Test
      fun `will update an incident`() {
        webTestClient.put().uri("/incidents/${incident4.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened",
                description = "and people had a fight",
                statusCode = "AWAN",
                typeCode = questionnaire1.code,
                location = "BXI",
                incidentDateTime = LocalDateTime.parse("2023-12-30T13:45:00"),
                reportedDateTime = LocalDateTime.parse("2024-01-02T09:30:00"),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident4.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident4.id)
          .jsonPath("questionnaireId").isEqualTo(questionnaire1.id)
          .jsonPath("title").isEqualTo("Something happened")
          .jsonPath("description").isEqualTo("and people had a fight")
          .jsonPath("status.code").isEqualTo("AWAN")
          .jsonPath("status.description").isEqualTo("Awaiting Analysis")
          .jsonPath("status.listSequence").isEqualTo(1)
          .jsonPath("status.standardUser").isEqualTo(true)
          .jsonPath("status.enhancedUser").isEqualTo(true)
          .jsonPath("type").isEqualTo("ESCAPE_EST")
          .jsonPath("agency.code").isEqualTo("BXI")
          .jsonPath("agency.description").isEqualTo("BRIXTON")
          .jsonPath("followUpDate").isEqualTo("2025-05-04") // existing data not overwritten
          .jsonPath("lockedResponse").isEqualTo(false)
          .jsonPath("incidentDateTime").isEqualTo("2023-12-30T13:45:00")
          .jsonPath("reportingStaff.staffId").isEqualTo(reportingStaff1.id)
          .jsonPath("reportingStaff.username").isEqualTo("FREDSTAFF")
          .jsonPath("reportingStaff.firstName").isEqualTo("FRED")
          .jsonPath("reportingStaff.lastName").isEqualTo("STAFF")
          .jsonPath("reportedDateTime").isEqualTo("2024-01-02T09:30:00")
          .jsonPath("createDateTime").isNotEmpty
          .jsonPath("createdBy").isNotEmpty
      }

      @Test
      fun `will update an incident with amendments`() {
        webTestClient.put().uri("/incidents/${incident2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with amendments",
                description = "and people had a fight",
                descriptionAmendments = listOf(
                  UpsertDescriptionAmendmentRequest(
                    text = "amendment",
                    firstName = "Bob",
                    lastName = "Bloggs",
                    createdDateTime = LocalDateTime.parse("2023-12-30T13:45:00"),
                  ),
                  UpsertDescriptionAmendmentRequest(
                    text = "second amendment",
                    firstName = "Joe",
                    lastName = "Smith",
                    createdDateTime = LocalDateTime.parse("2024-12-30T13:45:00"),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident2.id)
          .jsonPath("title").isEqualTo("Something happened with amendments")
          .jsonPath("description").isEqualTo("and people had a fightUser:Bloggs,Bob Date:30-Dec-2023 13:45amendmentUser:Smith,Joe Date:30-Dec-2024 13:45second amendment")
      }

      @Test
      fun `will update an incident with requirements`() {
        webTestClient.put().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with requirements",
                description = "and people had a fight",
                requirements = listOf(
                  UpsertIncidentRequirementRequest(
                    comment = "Update the name",
                    date = LocalDateTime.parse("2024-12-30T13:45:00"),
                    username = requirementRecordingStaff.accounts[0].username,
                    location = "MDI",
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident1.id)
          .jsonPath("title").isEqualTo("Something happened with requirements")
          .jsonPath("requirements[0].comment").isEqualTo("Update the name")
          .jsonPath("requirements[0].recordedDate").isEqualTo("2024-12-30T13:45:00")
          .jsonPath("requirements[0].sequence").isEqualTo(0)
          .jsonPath("requirements[0].staff.firstName").isEqualTo("PETER")
          .jsonPath("requirements[0].staff.lastName").isEqualTo("STAFF")
          .jsonPath("requirements[0].agencyId").isEqualTo("MDI")
          .jsonPath("requirements[0].createDateTime").isNotEmpty
          .jsonPath("requirements[0].createdBy").isNotEmpty
          .jsonPath("requirements.length()").isEqualTo(1)
      }

      @Test
      fun `will update an incident with requirements comment over 240 chars`() {
        webTestClient.put().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with requirements",
                description = "and people had a fight",
                requirements = listOf(
                  UpsertIncidentRequirementRequest(
                    comment = "a".repeat(241),
                    date = LocalDateTime.parse("2024-12-30T13:45:00"),
                    username = requirementRecordingStaff.accounts[0].username,
                    location = "MDI",
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident1.id)
          .jsonPath("requirements[0].comment").isEqualTo("${"a".repeat(215)}... see DPS for full text")
      }

      @Test
      fun `will update an incident with parties`() {
        webTestClient.put().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with parties",
                offenderParties = listOf(
                  UpsertOffenderPartyRequest(
                    comment = "a party with some data",
                    prisonNumber = offender2.nomsId,
                    role = "VICT",
                    outcome = "POR",
                  ),
                ),
                staffParties = listOf(
                  UpsertStaffPartyRequest(
                    comment = "b party with some data",
                    username = reportingStaff2.accounts[0].username,
                    role = "INPOS",
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident1.id)
          .jsonPath("title").isEqualTo("Something happened with parties")
          .jsonPath("offenderParties[0].offender.offenderNo").isEqualTo(offender2.nomsId)
          .jsonPath("offenderParties[0].offender.firstName").isEqualTo("Bob")
          .jsonPath("offenderParties[0].offender.lastName").isEqualTo("Smith")
          .jsonPath("offenderParties[0].sequence").isEqualTo(2000)
          .jsonPath("offenderParties[0].outcome.code").isEqualTo("POR")
          .jsonPath("offenderParties[0].outcome.description").isEqualTo("Placed on Report")
          .jsonPath("offenderParties[0].role.code").isEqualTo("VICT")
          .jsonPath("offenderParties[0].role.description").isEqualTo("Victim")
          .jsonPath("offenderParties[0].comment").isEqualTo("a party with some data")
          .jsonPath("offenderParties.length()").isEqualTo(1)
          .jsonPath("staffParties[0].staff.username").isEqualTo("JANESTAFF")
          .jsonPath("staffParties[0].staff.firstName").isEqualTo("JANE")
          .jsonPath("staffParties[0].staff.lastName").isEqualTo("STAFF")
          .jsonPath("staffParties[0].sequence").isEqualTo(1000)
          .jsonPath("staffParties[0].role.code").isEqualTo("INPOS")
          .jsonPath("staffParties[0].role.description").isEqualTo("In Possession")
          .jsonPath("staffParties[0].comment").isEqualTo("b party with some data")
          .jsonPath("staffParties.length()").isEqualTo(1)
      }

      @Test
      fun `will update an incident with parties comment over 240 chars`() {
        webTestClient.put().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with parties",
                offenderParties = listOf(
                  UpsertOffenderPartyRequest(
                    comment = "a".repeat(241),
                    prisonNumber = offender2.nomsId,
                    role = "VICT",
                    outcome = "POR",
                  ),
                ),
                staffParties = listOf(
                  UpsertStaffPartyRequest(
                    comment = "b".repeat(241),
                    username = reportingStaff1.accounts[0].username,
                    role = "INV",
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident1.id)
          .jsonPath("offenderParties[0].comment").isEqualTo("${"a".repeat(215)}... see DPS for full text")
          .jsonPath("staffParties[0].comment").isEqualTo("${"b".repeat(215)}... see DPS for full text")
      }

      @Test
      fun `will update an incident with questions`() {
        webTestClient.put().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with questions",
                questions = listOf(
                  UpsertIncidentQuestionRequest(
                    questionId = questionnaire1.questions[0].id,
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident1.id)
          .jsonPath("title").isEqualTo("Something happened with questions")
          .jsonPath("questions[0].questionId").isEqualTo(questionnaire1.questions[0].id)
          .jsonPath("questions[0].sequence").isEqualTo(1)
          .jsonPath("questions[0].question").isEqualTo(questionnaire1.questions[0].questionText)
          .jsonPath("questions[0].question").isEqualTo("Q4: Any Damage amount?")
          .jsonPath("questions.length()").isEqualTo(1)
      }

      @Test
      fun `will update an incident with responses`() {
        webTestClient.put().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with responses",
                questions = listOf(
                  UpsertIncidentQuestionRequest(
                    questionId = questionnaire1.questions[2].id,
                    responses = listOf(
                      UpsertIncidentResponseRequest(
                        answerId = questionnaire1.questions[2].answers[0].id,
                        comment = "a comment",
                        responseDate = LocalDate.parse("2010-03-02"),
                        recordingUsername = responseRecordingStaff.accounts[0].username,
                        sequence = 2,
                      ),
                    ),
                  ),
                  UpsertIncidentQuestionRequest(
                    questionId = questionnaire1.questions[1].id,
                    responses = listOf(
                      UpsertIncidentResponseRequest(
                        answerId = questionnaire1.questions[1].answers[0].id,
                        comment = "a comment",
                        responseDate = LocalDate.parse("2010-03-02"),
                        recordingUsername = responseRecordingStaff.accounts[0].username,
                        sequence = 0,
                      ),
                      UpsertIncidentResponseRequest(
                        answerId = questionnaire1.questions[1].answers[1].id,
                        comment = null,
                        responseDate = null,
                        recordingUsername = responseRecordingStaff.accounts[0].username,
                        sequence = 1,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident1.id)
          .jsonPath("title").isEqualTo("Something happened with responses")
          .jsonPath("questions[1].questionId").isEqualTo(questionnaire1.questions[1].id)
          .jsonPath("questions[1].sequence").isEqualTo(2)
          .jsonPath("questions[1].answers[0].answer").isEqualTo("Q3A1: Wire cutters")
          .jsonPath("questions[1].answers[0].comment").isEqualTo("a comment")
          .jsonPath("questions[1].answers[0].responseDate").isEqualTo("2010-03-02")
          .jsonPath("questions[1].answers[0].sequence").isEqualTo(0)
          .jsonPath("questions[1].answers[1].answer").isEqualTo("Q3A2: Spade")
          .jsonPath("questions[1].answers[1].comment").doesNotExist()
          .jsonPath("questions[1].answers[1].responseDate").doesNotExist()
          .jsonPath("questions[1].answers[1].sequence").isEqualTo(1)
          .jsonPath("questions[0].sequence").isEqualTo(1)
          .jsonPath("questions[0].answers[0].answer").isEqualTo("Q2A1: Yes")
          .jsonPath("questions[0].answers[0].comment").isEqualTo("a comment")
          .jsonPath("questions[0].answers[0].responseDate").isEqualTo("2010-03-02")
          .jsonPath("questions[0].answers[0].sequence").isEqualTo(2)
          .jsonPath("questions.length()").isEqualTo(2)
      }

      @Test
      fun `will update an incident with responses comment over 240 chars`() {
        webTestClient.put().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with responses",
                questions = listOf(
                  UpsertIncidentQuestionRequest(
                    questionId = questionnaire1.questions[2].id,
                    responses = listOf(
                      UpsertIncidentResponseRequest(
                        answerId = questionnaire1.questions[2].answers[0].id,
                        comment = "a".repeat(241),
                        responseDate = LocalDate.parse("2010-03-02"),
                        recordingUsername = responseRecordingStaff.accounts[0].username,
                        sequence = 0,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident1.id)
          .jsonPath("questions[0].answers[0].comment").isEqualTo("${"a".repeat(215)}... see DPS for full text")
      }

      @Test
      fun `will update an incident with history`() {
        webTestClient.put().uri("/incidents/${incident2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with history",
                history = listOf(
                  UpsertIncidentHistoryRequest(
                    typeCode = questionnaire2.code,
                    incidentChangeDateTime = LocalDateTime.parse("2012-03-02T10:20:30"),
                    incidentChangeUsername = reportingStaff2.accounts[0].username,
                    questions = listOf(),
                  ),
                  UpsertIncidentHistoryRequest(
                    typeCode = questionnaire1.code,
                    incidentChangeDateTime = LocalDateTime.parse("2012-03-01T10:20:30"),
                    incidentChangeUsername = reportingStaff2.accounts[0].username,
                    questions = listOf(
                      UpsertIncidentQuestionRequest(
                        questionId = questionnaire1.questions[1].id,
                        responses = listOf(
                          UpsertIncidentResponseRequest(
                            answerId = questionnaire1.questions[1].answers[0].id,
                            comment = "a comment",
                            responseDate = LocalDate.parse("2010-03-02"),
                            recordingUsername = responseRecordingStaff.accounts[0].username,
                            sequence = 0,
                          ),
                          UpsertIncidentResponseRequest(
                            answerId = questionnaire1.questions[1].answers[1].id,
                            comment = null,
                            responseDate = null,
                            recordingUsername = responseRecordingStaff.accounts[0].username,
                            sequence = 1,
                          ),
                        ),
                      ),
                      UpsertIncidentQuestionRequest(
                        questionId = questionnaire1.questions[2].id,
                        responses = listOf(
                          UpsertIncidentResponseRequest(
                            answerId = questionnaire1.questions[2].answers[0].id,
                            comment = "a comment",
                            responseDate = LocalDate.parse("2010-03-02"),
                            recordingUsername = responseRecordingStaff.accounts[0].username,
                            sequence = 2,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident2.id)
          .jsonPath("title").isEqualTo("Something happened with history")
          // history 0 unchanged
          .jsonPath("history[0].type").isEqualTo("FIRE")
          .jsonPath("history[0].description").isEqualTo("Questionnaire for fire")
          .jsonPath("history[0].questions[0].answers.length()").isEqualTo(1)
          .jsonPath("history[0].questions[0].answers[0].answer").doesNotExist()
          .jsonPath("history[0].questions[0].answers[0].comment").isEqualTo("one staff")
          .jsonPath("history[0].questionnaireId").isEqualTo(questionnaire2.id)
          // history 1 is new data
          .jsonPath("history[1].questionnaireId").isEqualTo(questionnaire1.id)
          .jsonPath("history[1].type").isEqualTo("ESCAPE_EST")
          .jsonPath("history[1].description").isEqualTo("Escape Questionnaire")
          .jsonPath("history[1].incidentChangeDateTime").isEqualTo("2012-03-01T10:20:30")
          .jsonPath("history[1].incidentChangeStaff.username").isEqualTo("JANESTAFF")
          .jsonPath("history[1].questions[0].questionId").isEqualTo(questionnaire1.questions[1].id)
          .jsonPath("history[1].questions[0].sequence").isEqualTo(0)
          .jsonPath("history[1].questions[0].answers[0].answer").isEqualTo("Q3A1: Wire cutters")
          .jsonPath("history[1].questions[0].answers[0].comment").isEqualTo("a comment")
          .jsonPath("history[1].questions[0].answers[0].responseDate").isEqualTo("2010-03-02")
          .jsonPath("history[1].questions[0].answers[1].answer").isEqualTo("Q3A2: Spade")
          .jsonPath("history[1].questions[0].answers[1].comment").doesNotExist()
          .jsonPath("history[1].questions[0].answers[1].responseDate").doesNotExist()
          .jsonPath("history[1].questions[1].answers[0].answer").isEqualTo("Q2A1: Yes")
          .jsonPath("history[1].questions[1].answers[0].comment").isEqualTo("a comment")
          .jsonPath("history[1].questions[1].answers[0].responseDate").isEqualTo("2010-03-02")
          .jsonPath("history[1].questions.length()").isEqualTo(2)
          .jsonPath("history.length()").isEqualTo(2)
      }

      @Test
      fun `will update an incident with history comment over 240 chars`() {
        webTestClient.put().uri("/incidents/${incident2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with history",
                history = listOf(
                  UpsertIncidentHistoryRequest(
                    typeCode = questionnaire2.code,
                    incidentChangeDateTime = LocalDateTime.parse("2012-03-02T10:20:30"),
                    incidentChangeUsername = reportingStaff2.accounts[0].username,
                    questions = listOf(),
                  ),
                  UpsertIncidentHistoryRequest(
                    typeCode = questionnaire1.code,
                    incidentChangeDateTime = LocalDateTime.parse("2012-03-01T10:20:30"),
                    incidentChangeUsername = reportingStaff2.accounts[0].username,
                    questions = listOf(
                      UpsertIncidentQuestionRequest(
                        questionId = questionnaire1.questions[1].id,
                        responses = listOf(
                          UpsertIncidentResponseRequest(
                            answerId = questionnaire1.questions[1].answers[0].id,
                            comment = "a".repeat(241),
                            responseDate = LocalDate.parse("2010-03-02"),
                            recordingUsername = responseRecordingStaff.accounts[0].username,
                            sequence = 0,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident2.id)
          // history 1 is new data
          .jsonPath("history[1].questions[0].answers[0].comment").isEqualTo("${"a".repeat(215)}... see DPS for full text")
      }

      @Test
      fun `will do nothing if an incident with history stays the same size`() {
        webTestClient.put().uri("/incidents/${incident2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .body(
            BodyInserters.fromValue(
              upsertIncidentRequest().copy(
                title = "Something happened with history",
                history = listOf(
                  UpsertIncidentHistoryRequest(
                    typeCode = questionnaire1.code,
                    incidentChangeDateTime = LocalDateTime.parse("2012-03-02T10:20:30"),
                    incidentChangeUsername = reportingStaff2.accounts[0].username,
                    questions = listOf(),
                  ),
                ),
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incidents/${incident2.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectBody()
          .jsonPath("incidentId").isEqualTo(incident2.id)
          .jsonPath("title").isEqualTo("Something happened with history")
          // history 0 unchanged
          .jsonPath("history[0].type").isEqualTo("FIRE")
          .jsonPath("history[0].description").isEqualTo("Questionnaire for fire")
          .jsonPath("history[0].questions[0].answers.length()").isEqualTo(1)
          .jsonPath("history[0].questions[0].answers[0].answer").doesNotExist()
          .jsonPath("history[0].questions[0].answers[0].comment").isEqualTo("one staff")
          .jsonPath("history[0].questionnaireId").isEqualTo(questionnaire2.id)
          .jsonPath("history.length()").isEqualTo(1)
      }
    }
  }

  @DisplayName("DELETE /incidents/{incidentId}")
  @Nested
  inner class DeleteIncident {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/incidents/${incident1.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class NoValidation {
      @Test
      fun `return 204 even when does not exist`() {
        webTestClient.delete().uri("/incidents/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectStatus().isNoContent
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will delete the incident`() {
        webTestClient.get().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectStatus()
          .isOk
        webTestClient.delete().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectStatus()
          .isNoContent
        webTestClient.get().uri("/incidents/${incident1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
          .exchange()
          .expectStatus()
          .isNotFound
      }
    }
  }
}
