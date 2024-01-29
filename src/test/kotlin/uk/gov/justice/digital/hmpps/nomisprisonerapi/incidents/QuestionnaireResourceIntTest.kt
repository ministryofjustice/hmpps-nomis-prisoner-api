package uk.gov.justice.digital.hmpps.nomisprisonerapi.incidents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire

class QuestionnaireResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  private lateinit var questionnaire1: Questionnaire
  private lateinit var questionnaire2: Questionnaire
  private lateinit var questionnaire3: Questionnaire

  @BeforeEach
  internal fun createQuestionnaires() {
    nomisDataBuilder.build {
      questionnaire1 = questionnaire(code = "ESCAPE_EST", description = "Escape Questionnaire") {
        val question14 = questionnaireQuestion(question = "Q4: Any Damage amount?") {
          questionnaireAnswer(answer = "Q4A1: Enter Damage Amount in Pounds")
        }
        val question13 = questionnaireQuestion(question = "Q3: What tools were used?", multipleAnswers = true) {
          questionnaireAnswer(answer = "Q3A1: Wire cutters", nextQuestion = question14)
          questionnaireAnswer(answer = "Q3A2: Spade", nextQuestion = question14)
          questionnaireAnswer(answer = "Q3A3: Crow bar", nextQuestion = question14)
        }
        val question12 = questionnaireQuestion(question = "Q2: Were tools used?") {
          questionnaireAnswer(answer = "Q2A1: Yes", nextQuestion = question13)
          questionnaireAnswer(answer = "Q2A2: No")
        }
        questionnaireQuestion(question = "Q1: Were the police informed of the incident?") {
          questionnaireAnswer(answer = "Q1A1: Yes", nextQuestion = question12)
          questionnaireAnswer(answer = "Q1A2: No", nextQuestion = question12)
        }
        offenderRole("ABS")
        offenderRole("ESC")
        offenderRole("FIGHT")
        offenderRole("PERP")
      }
      questionnaire2 = questionnaire(code = "FIRE", active = false, listSequence = 2) {
        questionnaireQuestion(question = "Q1A: Were staff involved?")
        questionnaireQuestion(question = "Q2A: Were prisoners involved?")
        offenderRole("ESC")
        offenderRole("FIGHT")
      }
      questionnaire3 = questionnaire(code = "MISC", listSequence = 3) {
        offenderRole("ESC")
      }
    }
  }

  @AfterEach
  internal fun deleteQuestionnaires() {
    repository.delete(questionnaire1)
    repository.delete(questionnaire2)
    repository.delete(questionnaire3)
  }

  @Nested
  @DisplayName("GET /questionnaires/ids")
  inner class GetQuestionnaireIds {

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/questionnaires/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/questionnaires/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/questionnaires/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `get all question ids - no filter specified`() {
      webTestClient.get().uri("/questionnaires/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(3)
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/questionnaires/ids")
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
        it.path("/questionnaires/ids")
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
  @DisplayName("GET /questionnaires/{questionnaireId}")
  inner class GetQuestionnaire {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/questionnaires/${questionnaire1.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/questionnaires/${questionnaire1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/questionnaires/${questionnaire1.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `unknown questionnaire should return not found`() {
      webTestClient.get().uri("/questionnaires/999999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Questionnaire with id=999999 does not exist")
        }
    }

    @Test
    fun `will return the questionnaire by Id`() {
      webTestClient.get().uri("/questionnaires/${questionnaire1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(questionnaire1.id)
        .jsonPath("code").isEqualTo("ESCAPE_EST")
        .jsonPath("active").isEqualTo(true)
        .jsonPath("description").isEqualTo("Escape Questionnaire")
    }

    @Test
    fun `will return the offender roles for this questionnaire`() {
      webTestClient.get().uri("/questionnaires/${questionnaire1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(questionnaire1.id)
        .jsonPath("offenderRoles.length()").isEqualTo(4)
        .jsonPath("offenderRoles[0]").isEqualTo("ABS")
        .jsonPath("offenderRoles[*]").value<List<String>>
        { assertThat(it).containsExactlyElementsOf(listOf("ABS", "ESC", "FIGHT", "PERP")) }
    }

    @Test
    fun `will return the questions and answers for a questionnaire`() {
      val persistedQuestionnaire1 = webTestClient.get().uri("/questionnaires/${questionnaire1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody(QuestionnaireResponse::class.java)
        .returnResult().responseBody!!

      with(persistedQuestionnaire1.questions[3]) {
        assertThat(question).isEqualTo("Q1: Were the police informed of the incident?")
        assertThat(questionSequence).isEqualTo(questionnaire1.questions[3].questionSequence)
        assertThat(listSequence).isEqualTo(questionnaire1.questions[3].listSequence)
        assertThat(multipleAnswers).isEqualTo(false)
        assertThat(active).isEqualTo(true)
        assertThat(answers.size).isEqualTo(2)
        with(answers[0]) {
          assertThat(answer).isEqualTo("Q1A1: Yes")
          assertThat(nextQuestion!!.question).isEqualTo("Q2: Were tools used?")
          assertThat(nextQuestion!!.id).isEqualTo(questionnaire1.questions[2].id)
          assertThat(answerSequence).isEqualTo(questionnaire1.questions[3].answers[0].answerSequence)
          assertThat(listSequence).isEqualTo(questionnaire1.questions[3].answers[0].listSequence)
          assertThat(active).isEqualTo(true)
          assertThat(commentRequired).isEqualTo(false)
          assertThat(dateRequired).isEqualTo(false)
        }
        assertThat(answers[1].answer).isEqualTo("Q1A2: No")
        assertThat(answers[1].nextQuestion!!.question).isEqualTo("Q2: Were tools used?")
      }

      with(persistedQuestionnaire1.questions[2]) {
        assertThat(question).isEqualTo("Q2: Were tools used?")
        assertThat(answers[0].answer).isEqualTo("Q2A1: Yes")
        assertThat(answers[0].nextQuestion!!.question).isEqualTo("Q3: What tools were used?")
        assertThat(answers[0].nextQuestion!!.id).isEqualTo(questionnaire1.questions[1].id)
        assertThat(answers[1].answer).isEqualTo("Q2A2: No")
        assertThat(answers[1].nextQuestion).isNull()
      }

      with(persistedQuestionnaire1.questions[1]) {
        assertThat(question).isEqualTo("Q3: What tools were used?")
        assertThat(multipleAnswers).isEqualTo(true)
        assertThat(answers[0].nextQuestion!!.question).isEqualTo("Q4: Any Damage amount?")
        assertThat(answers[0].answer).isEqualTo("Q3A1: Wire cutters")
        assertThat(answers[0].nextQuestion!!.question).isEqualTo("Q4: Any Damage amount?")
        assertThat(answers[1].answer).isEqualTo("Q3A2: Spade")
        assertThat(answers[2].nextQuestion!!.question).isEqualTo("Q4: Any Damage amount?")
        assertThat(answers[2].answer).isEqualTo("Q3A3: Crow bar")
        assertThat(answers[2].nextQuestion!!.question).isEqualTo("Q4: Any Damage amount?")
      }

      with(persistedQuestionnaire1.questions[0]) {
        assertThat(question).isEqualTo("Q4: Any Damage amount?")
        assertThat(answers[0].answer).isEqualTo("Q4A1: Enter Damage Amount in Pounds")
        assertThat(answers[0].nextQuestion).isNull()
      }
    }

    @Test
    fun `can request an inactive questionnaire by Id`() {
      webTestClient.get().uri("/questionnaires/${questionnaire2.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCIDENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(questionnaire2.id)
        .jsonPath("code").isEqualTo("FIRE")
        .jsonPath("active").isEqualTo(false)
        .jsonPath("description").isEqualTo("This is a questionnaire")
    }
  }
}
