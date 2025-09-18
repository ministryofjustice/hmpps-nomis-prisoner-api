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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivity
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseSchedule
import java.time.LocalDate

class ScheduleResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Nested
  inner class UpdateCourseSchedule {

    private lateinit var courseActivity: CourseActivity
    private lateinit var courseSchedule: CourseSchedule

    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1)
    private val tomorrow = today.plusDays(1)

    private fun validJsonRequest() = """
        {
          "id": ${courseSchedule.courseScheduleId},
          "date": "$today",
          "startTime": "08:00",
          "endTime": "11:00",
          "cancelled": true
        }
    """.trimIndent()

    private fun String.withId(id: Long) = replace(""""id": ${courseSchedule.courseScheduleId},""", """"id": $id,""")

    private fun String.withDate(date: String) = replace(""""date": "$today",""", """"date": "$date",""")

    private fun String.withStartTime(startTime: String) = replace(""""startTime": "08:00",""", """"startTime": "$startTime",""")

    private fun String.withEndTime(endTime: String) = replace(""""endTime": "11:00",""", """"endTime": "$endTime",""")

    private fun String.withCancelled(cancelled: String) = replace(""""cancelled": true""", """"cancelled": ${cancelled.toBooleanStrictOrNull() ?: ('"' + cancelled + '"')}""")

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        programService {
          courseActivity = courseActivity {
            payRate()
            courseSchedule = courseSchedule(scheduleDate = today.toString())
            courseScheduleRule()
          }
        }
      }
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().withDate("2022-11-61"),
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().withStartTime("29:00"),
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().withEndTime("11:70"),
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().withCancelled("INVALID"),
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().withId(99999),
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("Course schedule 99999 does not exist")
          }
      }
    }

    @Nested
    inner class Update {

      @Test
      fun `should return OK if updated to cancelled`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(BodyInserters.fromValue(validJsonRequest()))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseScheduleId").isEqualTo(courseSchedule.courseScheduleId)

        val saved = repository.getSchedule(courseSchedule.courseScheduleId)
        with(saved) {
          assertThat(scheduleDate).isEqualTo("$today")
          assertThat(startTime).isEqualTo("${today}T08:00")
          assertThat(endTime).isEqualTo("${today}T11:00")
          assertThat(scheduleStatus).isEqualTo("CANC")
        }
      }

      @Test
      fun `should return OK if updated to not cancelled`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity {
              payRate()
              courseSchedule = courseSchedule(scheduleDate = today.toString(), scheduleStatus = "CANC")
              courseScheduleRule()
            }
          }
        }

        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().withCancelled("false"),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseScheduleId").isEqualTo(courseSchedule.courseScheduleId)

        val saved = repository.getSchedule(courseSchedule.courseScheduleId)
        with(saved) {
          assertThat(scheduleStatus).isEqualTo("SCH")
        }
      }

      @Test
      fun `should return OK if already at cancelled status`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity {
              payRate()
              courseSchedule = courseSchedule(scheduleDate = today.toString(), scheduleStatus = "CANC")
              courseScheduleRule()
            }
          }
        }

        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().withDate(tomorrow.toString()),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseScheduleId").isEqualTo(courseSchedule.courseScheduleId)

        val saved = repository.getSchedule(courseSchedule.courseScheduleId)
        with(saved) {
          assertThat(scheduleStatus).isEqualTo("CANC")
        }
      }

      @Test
      fun `should return bad request if updating old schedule`() {
        nomisDataBuilder.build {
          programService {
            courseActivity = courseActivity {
              payRate()
              courseSchedule = courseSchedule(scheduleDate = yesterday.toString())
              courseScheduleRule()
            }
          }
        }

        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().withDate(yesterday.toString()),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Cannot change schedule id=${courseSchedule.courseScheduleId} because it is immutable")
          }

        val saved = repository.getSchedule(courseSchedule.courseScheduleId)
        with(saved) {
          assertThat(scheduleDate).isEqualTo("$yesterday")
          assertThat(startTime).isEqualTo("${yesterday}T08:00")
          assertThat(endTime).isEqualTo("${yesterday}T11:00")
        }
      }

      @Test
      fun `should return bad request if dates out of order`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().withStartTime("13:00").withEndTime("12:59"),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("userMessage").value<String> {
            assertThat(it).contains("Schedule for date $today has times out of order - 13:00 to 12:59")
          }

        val saved = repository.getSchedule(courseSchedule.courseScheduleId)
        with(saved) {
          assertThat(scheduleDate).isEqualTo("$today")
          assertThat(startTime).isEqualTo("${today}T08:00")
          assertThat(endTime).isEqualTo("${today}T11:00")
        }
      }

      @Test
      fun `should return OK if updating dates and times`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest().withStartTime("13:00").withEndTime("15:00"),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("courseScheduleId").isEqualTo(courseSchedule.courseScheduleId)

        val saved = repository.getSchedule(courseSchedule.courseScheduleId)
        with(saved) {
          assertThat(scheduleDate).isEqualTo("$today")
          assertThat(startTime).isEqualTo("${today}T13:00")
          assertThat(endTime).isEqualTo("${today}T15:00")
        }
      }

      @Test
      fun `should publish telemetry`() {
        webTestClient.put().uri("/activities/${courseActivity.courseActivityId}/schedule")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .body(BodyInserters.fromValue(validJsonRequest()))
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("activity-course-schedule-updated"),
          check<MutableMap<String, String>> {
            assertThat(it["nomisCourseScheduleId"]).isEqualTo("${courseSchedule.courseScheduleId}")
            assertThat(it["nomisCourseActivityId"]).isEqualTo("${courseSchedule.courseActivity.courseActivityId}")
            assertThat(it["prisonId"]).isEqualTo(courseSchedule.courseActivity.prison.id)
          },
          isNull(),
        )
      }
    }
  }
}
