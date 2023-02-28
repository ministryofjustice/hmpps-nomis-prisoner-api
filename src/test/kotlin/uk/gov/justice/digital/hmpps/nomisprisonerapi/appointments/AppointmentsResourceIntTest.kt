package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventClass
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val PRISON_ID = "MDI"
private const val MDI_ROOM_ID: Long = -46 // random location from R__3_2__AGENCY_INTERNAL_LOCATIONS.sql

class AppointmentsResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  lateinit var offenderAtMoorlands: Offender
  lateinit var offenderAtOtherPrison: Offender

  @BeforeEach
  internal fun createPrisoner() {
    offenderAtMoorlands = repository.save(
      OffenderBuilder(nomsId = "A1234TT")
        .withBooking(OffenderBookingBuilder(agencyLocationId = PRISON_ID))
    )
    offenderAtOtherPrison = repository.save(
      OffenderBuilder(nomsId = "A1234TU")
        .withBooking(OffenderBookingBuilder(agencyLocationId = "LEI"))
    )
  }

  @AfterEach
  internal fun deletePrisoner() {
    repository.delete(offenderAtMoorlands)
    repository.delete(offenderAtOtherPrison)
  }

  @Nested
  inner class CreateAppointment {

    private val createAppointmentRequest: () -> CreateAppointmentRequest = {
      CreateAppointmentRequest(
        bookingId = offenderAtMoorlands.latestBooking().bookingId,
        eventDate = LocalDate.parse("2022-10-31"),
        startTime = LocalTime.of(8, 0),
        endTime = LocalTime.of(11, 0),
        internalLocationId = MDI_ROOM_ID,
        eventSubType = "ACTI",
      )
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/appointments")
        .body(BodyInserters.fromValue(createAppointmentRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/appointments")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createAppointmentRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.post().uri("/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createAppointmentRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access with booking not found`() {
      webTestClient.post().uri("/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(createAppointmentRequest().copy(bookingId = 999997)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Booking with id=999997 not found")
        }
    }

    @Test
    fun `access with room not found`() {
      webTestClient.post().uri("/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(createAppointmentRequest().copy(internalLocationId = 999998)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Room with id=999998 does not exist")
        }
    }

    @Test
    fun `prisoner and room not in same prison`() {
      webTestClient.post().uri("/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(createAppointmentRequest().copy(bookingId = offenderAtOtherPrison.latestBooking().bookingId)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Room with id=$MDI_ROOM_ID is in MDI, not in the offender's prison: LEI")
        }
    }

    @Test
    fun `EventSubType does not exist`() {
      webTestClient.post().uri("/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(createAppointmentRequest().copy(eventSubType = "INVALID")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("EventSubType with code=INVALID does not exist")
        }
    }

    @Test
    fun `end time before start`() {
      webTestClient.post().uri("/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(createAppointmentRequest().copy(endTime = LocalTime.of(7, 0))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("End time must be after start time")
        }
    }

    @Test
    fun `invalid start time should return bad request`() {
      val invalidSchedule = validJsonRequest().replace(""""startTime"          : "10:40"""", """"startTime": "11:65",""")
      webTestClient.post().uri("/appointments")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(invalidSchedule))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("11:65")
        }
    }

    @Test
    fun `invalid end time should return bad request`() {
      val invalidSchedule = validJsonRequest().replace(""""endTime"            : "12:10"""", """"endTime": "12:65"""")
      webTestClient.post().uri("/appointments")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(invalidSchedule))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("12:65")
        }
    }

    @Test
    fun `invalid date should return bad request`() {
      val invalidSchedule = validJsonRequest().replace(""""eventDate"          : "2023-02-27"""", """"eventDate": "2022-13-31",""")
      webTestClient.post().uri("/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(invalidSchedule))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("2022-13-31")
        }
    }

    @Test
    fun `will create appointment with correct details`() {

      val id = callCreateEndpoint()

      // Check the database
      val offenderIndividualSchedule = repository.lookupAppointment(id)!!

      assertThat(offenderIndividualSchedule.eventId).isEqualTo(id)
      assertThat(offenderIndividualSchedule.offenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
      assertThat(offenderIndividualSchedule.eventDate).isEqualTo(LocalDate.parse("2023-02-27"))
      assertThat(offenderIndividualSchedule.startTime).isEqualTo(LocalDateTime.parse("2023-02-27T10:40"))
      assertThat(offenderIndividualSchedule.endTime).isEqualTo(LocalDateTime.parse("2023-02-27T12:10"))
      assertThat(offenderIndividualSchedule.eventClass).isEqualTo(EventClass.INT_MOV)
      assertThat(offenderIndividualSchedule.eventType).isEqualTo("APP")
      assertThat(offenderIndividualSchedule.eventSubType.code).isEqualTo("ACTI")
      assertThat(offenderIndividualSchedule.eventStatus.code).isEqualTo("SCH")
      assertThat(offenderIndividualSchedule.prison?.id).isEqualTo("MDI")
      assertThat(offenderIndividualSchedule.internalLocation?.locationId).isEqualTo(MDI_ROOM_ID)
    }

    private fun callCreateEndpoint(): Long {
      val response = webTestClient.post().uri("/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(validJsonRequest()))
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateAppointmentResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.eventId).isGreaterThan(0)
      return response!!.eventId
    }

    private fun validJsonRequest() = """{
            "bookingId"          : ${offenderAtMoorlands.latestBooking().bookingId},
            "eventDate"          : "2023-02-27",
            "startTime"          : "10:40",
            "endTime"            : "12:10",
            "internalLocationId" : $MDI_ROOM_ID,
            "eventSubType"       : "ACTI"
          }
    """.trimIndent()
  }
}
