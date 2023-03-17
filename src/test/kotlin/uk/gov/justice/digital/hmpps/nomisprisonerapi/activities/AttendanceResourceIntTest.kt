package uk.gov.justice.digital.hmpps.nomisprisonerapi.activities

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ConflictException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase

class AttendanceResourceIntTest : IntegrationTestBase() {

  // TODO SDIT-689 Remove spy bean once the service has been implemented
  @SpyBean
  private lateinit var attendanceService: AttendanceService

  @Nested
  inner class CreateAttendance {

    private val validJsonRequest = """
      {
        "eventStatusCode": "COMP",
        "eventOutcomeCode": "ATT",
        "comments": "Engaged",
        "unexcusedAbsence": false,
        "authorisedAbsence": "false",
        "paid": "true",
        "bonusPay": 1.5
      }
    """.trimIndent()

    @Nested
    inner class Api {
      @Test
      fun `should return unauthorised if no token`() {
        webTestClient.post().uri("/schedules/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden if no role`() {
        webTestClient.post().uri("/schedules/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf()))
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden with wrong role`() {
        webTestClient.post().uri("/schedules/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return conflict`() {
        doThrow(ConflictException("Attendance record already exists"))
          .whenever(attendanceService).createAttendance(anyLong(), anyLong(), any())

        webTestClient.post().uri("/schedules/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return not found`() {
        doThrow(NotFoundException("Schedule id 1 does not exist"))
          .whenever(attendanceService).createAttendance(anyLong(), anyLong(), any())

        webTestClient.post().uri("/schedules/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `should return bad request`() {
        webTestClient.post().uri("/schedules/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(
            BodyInserters.fromValue(
              validJsonRequest.replace(""""unexcusedAbsence": false,""", """"unexcusedAbsence": INVALID,"""),
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("INVALID")
          }
      }

      @Test
      fun `should return created for valid request`() {
        webTestClient.post().uri("/schedules/1/booking/2/attendance")
          .contentType(MediaType.APPLICATION_JSON)
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
          .body(BodyInserters.fromValue(validJsonRequest))
          .exchange()
          .expectStatus().isCreated
      }
    }
  }
}
