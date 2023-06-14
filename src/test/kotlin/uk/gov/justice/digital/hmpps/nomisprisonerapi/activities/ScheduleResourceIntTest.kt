package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseActivityBuilderFactory
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CourseScheduleBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.ProgramServiceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule

class ScheduleResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var courseActivityBuilderFactory: CourseActivityBuilderFactory

  @Nested
  inner class UpdateCourseSchedule {

    private lateinit var courseActivity: CourseActivity
    private lateinit var courseSchedule: CourseSchedule

    private fun validJsonRequest() = """
        {
          "id": ${courseSchedule.courseScheduleId},
          "date": "2022-11-01",
          "startTime": "08:00",
          "endTime": "11:00",
          "cancelled": true
        }
    """.trimIndent()

    @BeforeEach
    fun setUp() {
      repository.save(ProgramServiceBuilder())
      courseActivity = repository.save(courseActivityBuilderFactory.builder())
      courseSchedule = courseActivity.courseSchedules.first()
    }

    @AfterEach
    fun cleanUp() {
      repository.deleteActivities()
      repository.deleteProgramServices()
    }

    @Nested
    inner class Api {
      @Test
      fun `should return unauthorised if no token`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(validJsonRequest()))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden if no role`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(validJsonRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden with wrong role`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(validJsonRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return bad request for date`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().replace(""""date": "2022-11-01",""", """"date": "2022-11-61","""),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("2022-11-61")
          }
      }

      @Test
      fun `should return bad request for start time`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().replace(""""startTime": "08:00",""", """"startTime": "29:00","""),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("29:00")
          }
      }

      @Test
      fun `should return bad request for end time`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().replace(""""endTime": "11:00",""", """"endTime": "11:70","""),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("11:70")
          }
      }

      @Test
      fun `should return bad request for cancelled`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().replace(""""cancelled": true""", """"cancelled": "INVALID""""),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("INVALID")
          }
      }

      @Test
      fun `should return not found for unknown activity`() {
        webTestClient.put().uri("/activities/9876/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(validJsonRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Course activity with id=9876 does not exist")
          }
      }

      @Test
      fun `should return not found for unknown schedule`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().replace(""""id": ${courseSchedule.courseScheduleId},""", """"id": 99999,"""),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Course schedule id=99999 not found")
          }
      }
    }

    @Nested
    inner class Update {

      @Test
      fun `should return OK if updated to cancelled`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(validJsonRequest()))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseScheduleId").isEqualTo(courseSchedule.courseScheduleId)

        val saved = repository.lookupSchedule(courseSchedule.courseScheduleId)
        with(saved) {
          assertThat(scheduleDate).isEqualTo("2022-11-01")
          assertThat(startTime).isEqualTo("2022-11-01T08:00")
          assertThat(endTime).isEqualTo("2022-11-01T11:00")
          assertThat(scheduleStatus).isEqualTo("CANC")
        }
      }

      @Test
      fun `should return OK if updated to not cancelled`() {
        courseActivity = repository.save(
          courseActivityBuilderFactory.builder(
            courseSchedules = listOf(
              CourseScheduleBuilder(
                scheduleDate = "2022-11-03",
                scheduleStatus = "CANC",
              ),
            ),
          ),
        )
        courseSchedule = courseActivity.courseSchedules.first()

        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest()
                .replace(""""date": "2022-11-01",""", """"date": "2022-11-03",""")
                .replace(""""cancelled": true""", """"cancelled": false"""),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseScheduleId").isEqualTo(courseSchedule.courseScheduleId)

        val saved = repository.lookupSchedule(courseSchedule.courseScheduleId)
        with(saved) {
          assertThat(scheduleStatus).isEqualTo("SCH")
        }
      }

      @Test
      fun `should return OK if already at cancelled status`() {
        courseActivity = repository.save(
          courseActivityBuilderFactory.builder(
            courseSchedules = listOf(
              CourseScheduleBuilder(
                scheduleDate = "2022-11-03",
                scheduleStatus = "CANC",
              ),
            ),
          ),
        )
        courseSchedule = courseActivity.courseSchedules.first()

        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().replace(""""date": "2022-11-01",""", """"date": "2022-11-03","""),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseScheduleId").isEqualTo(courseSchedule.courseScheduleId)

        val saved = repository.lookupSchedule(courseSchedule.courseScheduleId)
        with(saved) {
          assertThat(scheduleStatus).isEqualTo("CANC")
        }
      }

      @Test
      fun `should publish telemetry`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(validJsonRequest()))
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("activity-course-schedule-updated"),
          check<MutableMap<String, String>> {
            assertThat(it["nomisCourseScheduleId"]).isEqualTo("${courseSchedule.courseScheduleId}")
            assertThat(it["nomisCourseActivityId"]).isEqualTo("${courseSchedule.courseActivity.courseActivityId}")
          },
          isNull(),
        )
      }
    }
  }
}
