package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventClass
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIndividualSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val PRISON_ID = "MDI"
private const val MDI_ROOM_ID: Long = -46 // random locations from R__3_2__AGENCY_INTERNAL_LOCATIONS.sql
private const val MDI_ROOM_ID_2: Long = -47

class AppointmentsResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  lateinit var offenderAtMoorlands: Offender
  lateinit var offenderAtOtherPrison: Offender

  private fun callCreateEndpoint(hasEndTime: Boolean, inCell: Boolean = false): Long {
    val response = webTestClient.post().uri("/appointments")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(validCreateJsonRequest(hasEndTime, inCell)))
      .exchange()
      .expectStatus().isCreated
      .expectBody(CreateAppointmentResponse::class.java)
      .returnResult().responseBody
    assertThat(response?.eventId).isGreaterThan(0)
    return response!!.eventId
  }

  private fun validCreateJsonRequest(hasEndTime: Boolean, inCell: Boolean) = """{
            "bookingId"          : ${offenderAtMoorlands.latestBooking().bookingId},
            "eventDate"          : "2023-02-27",
            "startTime"          : "10:40",
${if (hasEndTime) """ "endTime"   : "12:10",""" else ""}
${if (inCell) "" else """ "internalLocationId" : $MDI_ROOM_ID,"""}
            "eventSubType"       : "ACTI"
          }
  """.trimIndent()

  private fun callCancelEndpoint(eventId: Long) {
    webTestClient.put().uri("/appointments/$eventId/cancel")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
      .exchange()
      .expectStatus().isOk
  }

  private fun callUncancelEndpoint(eventId: Long) {
    webTestClient.put().uri("/appointments/$eventId/uncancel")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
      .exchange()
      .expectStatus().isOk
  }

  @BeforeEach
  internal fun createPrisoner() {
    nomisDataBuilder.build {
      offenderAtMoorlands =
        offender(nomsId = "A1234TT") {
          booking(agencyLocationId = PRISON_ID)
        }
      offenderAtOtherPrison =
        offender(nomsId = "A1234TU") {
          booking(agencyLocationId = "LEI")
        }
    }
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
    fun `comment too long`() {
      webTestClient.post().uri("/appointments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(createAppointmentRequest().copy(comment = "x".repeat(4001))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Comment is too long (max allowed 4000 characters)")
        }
    }

    @Test
    fun `invalid start time should return bad request`() {
      val invalidSchedule =
        validCreateJsonRequest(false, false).replace(""""startTime"          : "10:40"""", """"startTime": "11:65",""")
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
      val invalidSchedule = validCreateJsonRequest(true, false).replace(""""endTime"   : "12:10"""", """"endTime": "12:65"""")
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
      val invalidSchedule = validCreateJsonRequest(false, false).replace(
        """"eventDate"          : "2023-02-27"""",
        """"eventDate": "2022-13-31",""",
      )
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
      val id = callCreateEndpoint(true, false)

      // Check the database
      val offenderIndividualSchedule = repository.getAppointment(id)!!

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

    @Test
    fun `will create appointment with correct details - no end time`() {
      val id = callCreateEndpoint(false, false)

      val offenderIndividualSchedule = repository.getAppointment(id)!!

      assertThat(offenderIndividualSchedule.eventId).isEqualTo(id)
      assertThat(offenderIndividualSchedule.startTime).isEqualTo(LocalDateTime.parse("2023-02-27T10:40"))
      assertThat(offenderIndividualSchedule.endTime).isNull()
    }

    @Test
    fun `will create appointment with correct details - in cell`() {
      val id = callCreateEndpoint(false, true)

      val offenderIndividualSchedule = repository.getAppointment(id)!!

      assertThat(offenderIndividualSchedule.eventId).isEqualTo(id)
      assertThat(offenderIndividualSchedule.internalLocation?.locationId).isEqualTo(-3009) // cell
    }
  }

  @Nested
  inner class UpdateAppointment {

    private val updateAppointmentRequest: () -> UpdateAppointmentRequest = {
      UpdateAppointmentRequest(
        eventDate = LocalDate.parse("2022-10-31"),
        startTime = LocalTime.of(8, 0),
        endTime = LocalTime.of(11, 0),
        internalLocationId = MDI_ROOM_ID,
        eventSubType = "ACTI",
      )
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/appointments/1")
        .body(BodyInserters.fromValue(updateAppointmentRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/appointments/1")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(updateAppointmentRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put().uri("/appointments/1")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(updateAppointmentRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `appointment does not exist`() {
      webTestClient.put().uri("/appointments/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(updateAppointmentRequest()))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `access with room not found`() {
      val eventId = callCreateEndpoint(false)
      webTestClient.put().uri("/appointments/$eventId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(updateAppointmentRequest().copy(internalLocationId = 999998)))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Room with id=999998 does not exist")
        }
    }

    @Test
    fun `EventSubType does not exist`() {
      val eventId = callCreateEndpoint(false)
      webTestClient.put().uri("/appointments/$eventId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(updateAppointmentRequest().copy(eventSubType = "INVALID")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("EventSubType with code=INVALID does not exist")
        }
    }

    @Test
    fun `end time before start`() {
      val eventId = callCreateEndpoint(false)
      webTestClient.put().uri("/appointments/$eventId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .body(BodyInserters.fromValue(updateAppointmentRequest().copy(endTime = LocalTime.of(7, 0))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("End time must be after start time")
        }
    }

    @Test
    fun `invalid start time should return bad request`() {
      val invalidSchedule =
        validUpdateJsonRequest(false).replace(""""startTime"          : "10:50"""", """"startTime": "11:65",""")
      val eventId = callCreateEndpoint(false)
      webTestClient.put().uri("/appointments/$eventId")
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
      val invalidSchedule = validUpdateJsonRequest(true).replace(""""endTime"   : "12:20"""", """"endTime": "12:65"""")
      val eventId = callCreateEndpoint(false)
      webTestClient.put().uri("/appointments/$eventId")
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
      val invalidSchedule = validUpdateJsonRequest(false).replace(
        """"eventDate"          : "2023-02-28"""",
        """"eventDate": "2022-13-31",""",
      )
      val eventId = callCreateEndpoint(false)
      webTestClient.put().uri("/appointments/$eventId")
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
    fun `will update appointment with correct details`() {
      val eventId = callCreateEndpoint(true)
      callUpdateEndpoint(eventId, true)

      // Check the database
      val offenderIndividualSchedule = repository.getAppointment(eventId)!!

      assertThat(offenderIndividualSchedule.eventId).isEqualTo(eventId)
      assertThat(offenderIndividualSchedule.offenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
      assertThat(offenderIndividualSchedule.eventDate).isEqualTo(LocalDate.parse("2023-02-28"))
      assertThat(offenderIndividualSchedule.startTime).isEqualTo(LocalDateTime.parse("2023-02-28T10:50"))
      assertThat(offenderIndividualSchedule.endTime).isEqualTo(LocalDateTime.parse("2023-02-28T12:20"))
      assertThat(offenderIndividualSchedule.eventSubType.code).isEqualTo("CABA")
      assertThat(offenderIndividualSchedule.prison?.id).isEqualTo("MDI")
      assertThat(offenderIndividualSchedule.comment).isEqualTo("Some comment")
      assertThat(offenderIndividualSchedule.internalLocation?.locationId).isEqualTo(MDI_ROOM_ID_2)
      assertThat(offenderIndividualSchedule.modifiedBy).isEqualTo("SA")
      assertThat(offenderIndividualSchedule.modifiedBy).isNotBlank()
    }

    @Test
    fun `will update appointment with correct details - no end time`() {
      val eventId = callCreateEndpoint(true)
      callUpdateEndpoint(eventId, false)

      // Check the database
      val offenderIndividualSchedule = repository.getAppointment(eventId)!!

      assertThat(offenderIndividualSchedule.eventId).isEqualTo(eventId)
      assertThat(offenderIndividualSchedule.eventDate).isEqualTo(LocalDate.parse("2023-02-28"))
      assertThat(offenderIndividualSchedule.startTime).isEqualTo(LocalDateTime.parse("2023-02-28T10:50"))
      assertThat(offenderIndividualSchedule.endTime).isNull()
    }

    @Test
    fun `will update appointment with correct details - in cell`() {
      val eventId = callCreateEndpoint(true, false)
      callUpdateEndpoint(eventId, false, true)

      // Check the database
      val offenderIndividualSchedule = repository.getAppointment(eventId)!!

      assertThat(offenderIndividualSchedule.eventId).isEqualTo(eventId)
      assertThat(offenderIndividualSchedule.internalLocation?.locationId).isEqualTo(-3009)
    }

    private fun callUpdateEndpoint(eventId: Long, hasEndTime: Boolean, inCell: Boolean = false) {
      webTestClient.put().uri("/appointments/$eventId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(validUpdateJsonRequest(hasEndTime, inCell)))
        .exchange()
        .expectStatus().isOk
    }

    private fun validUpdateJsonRequest(hasEndTime: Boolean, inCell: Boolean = false) = """{
            "eventDate"          : "2023-02-28",
            "startTime"          : "10:50",
${if (hasEndTime) """"endTime"   : "12:20",""" else ""}
${if (inCell) "" else """ "internalLocationId" : $MDI_ROOM_ID_2,"""}
            "comment"            : "Some comment",
            "eventSubType"       : "CABA"
          }
    """.trimIndent()
  }

  @Nested
  inner class CancelAppointment {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/appointments/1/cancel")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/appointments/1/cancel")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put().uri("/appointments/1/cancel")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `appointment does not exist`() {
      webTestClient.put().uri("/appointments/1/cancel")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `will cancel appointment correctly`() {
      val eventId = callCreateEndpoint(false)
      callCancelEndpoint(eventId)

      // Check the database
      val offenderIndividualSchedule = repository.getAppointment(eventId)!!

      assertThat(offenderIndividualSchedule.eventId).isEqualTo(eventId)
      assertThat(offenderIndividualSchedule.eventStatus.code).isEqualTo("CANC")
    }
  }

  @Nested
  inner class UncancelAppointment {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/appointments/1/uncancel")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/appointments/1/uncancel")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put().uri("/appointments/1/uncancel")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `appointment does not exist`() {
      webTestClient.put().uri("/appointments/1/uncancel")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `will uncancel appointment correctly`() {
      val eventId = callCreateEndpoint(false)
      callCancelEndpoint(eventId)
      callUncancelEndpoint(eventId)

      // Check the database
      val offenderIndividualSchedule = repository.getAppointment(eventId)!!

      assertThat(offenderIndividualSchedule.eventId).isEqualTo(eventId)
      assertThat(offenderIndividualSchedule.eventStatus.code).isEqualTo("SCH")
    }
  }

  @Nested
  inner class DeleteAppointment {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.delete().uri("/appointments/1")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.delete().uri("/appointments/1")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.delete().uri("/appointments/1")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `appointment does not exist`() {
      webTestClient.delete().uri("/appointments/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `will delete appointment correctly`() {
      val eventId = callCreateEndpoint(false)
      callDeleteEndpoint(eventId)

      // Check the database

      assertThat(repository.getAppointment(eventId)).isNull()
    }

    private fun callDeleteEndpoint(eventId: Long) {
      webTestClient.delete().uri("/appointments/$eventId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @Nested
  inner class GetAppointmentById {

    private lateinit var appointment1: OffenderIndividualSchedule

    @BeforeEach
    internal fun createAppointments() {
      appointment1 = repository.save(
        OffenderIndividualSchedule(
          offenderBooking = offenderAtMoorlands.latestBooking(),
          eventDate = LocalDate.parse("2023-01-01"),
          startTime = LocalDateTime.parse("2020-01-01T10:00"),
          endTime = LocalDateTime.parse("2020-01-01T11:00"),
          eventSubType = repository.lookupEventSubtype("MEDE"),
          eventStatus = repository.lookupEventStatusCode("SCH"),
          prison = repository.lookupAgency("MDI"),
          internalLocation = repository.lookupAgencyInternalLocation(-1L),
          comment = "hit the gym",
        ),
      )
    }

    @AfterEach
    internal fun deleteAppointments() {
      repository.delete(appointment1)
    }

    @Test
    fun `get by id`() {
      webTestClient.get().uri("/appointments/${appointment1.eventId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.bookingId").isEqualTo(appointment1.offenderBooking.bookingId)
        .jsonPath("$.offenderNo").isEqualTo(appointment1.offenderBooking.offender.nomsId)
        .jsonPath("$.prisonId").isEqualTo("MDI")
        .jsonPath("$.internalLocation").isEqualTo(appointment1.internalLocation?.locationId.toString())
        .jsonPath("$.startDateTime").isEqualTo("2023-01-01T10:00:00")
        .jsonPath("$.endDateTime").isEqualTo("2023-01-01T11:00:00")
        .jsonPath("$.comment").isEqualTo("hit the gym")
        .jsonPath("$.subtype").isEqualTo("MEDE")
        .jsonPath("$.status").isEqualTo("SCH")
        .jsonPath("$.createdDate").isNotEmpty()
        .jsonPath("$.createdBy").isEqualTo("SA")
    }

    @Test
    fun `appointments not found`() {
      webTestClient.get().uri("/appointments/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `malformed id returns bad request`() {
      webTestClient.get().uri("/appointments/stuff")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `get appointments prevents access without appropriate role`() {
      assertThat(
        webTestClient.get().uri("/appointments/1")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get appointments prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/appointments/1")
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }

  @Nested
  inner class GetAppointmentIdsByFilterRequest {

    private lateinit var appointment1: OffenderIndividualSchedule
    private lateinit var appointment2: OffenderIndividualSchedule
    private lateinit var appointment3: OffenderIndividualSchedule
    private lateinit var appointment4: OffenderIndividualSchedule

    @BeforeEach
    internal fun createAppointments() {
      appointment1 = repository.save(
        OffenderIndividualSchedule(
          offenderBooking = offenderAtMoorlands.latestBooking(),
          eventDate = LocalDate.parse("2023-01-01"),
          eventSubType = repository.lookupEventSubtype("MEDE"),
          eventStatus = repository.lookupEventStatusCode("SCH"),
          prison = repository.lookupAgency("MDI"),
        ),
      )
      appointment2 = repository.save(
        OffenderIndividualSchedule(
          offenderBooking = offenderAtOtherPrison.latestBooking(),
          eventDate = LocalDate.parse("2023-01-02"),
          eventSubType = repository.lookupEventSubtype("MEDO"),
          eventStatus = repository.lookupEventStatusCode("SCH"),
          prison = repository.lookupAgency("BXI"),
        ),
      )
      appointment3 = repository.save(
        OffenderIndividualSchedule(
          offenderBooking = offenderAtMoorlands.latestBooking(),
          eventDate = LocalDate.parse("2023-01-03"),
          eventSubType = repository.lookupEventSubtype("MEOP"),
          eventStatus = repository.lookupEventStatusCode("SCH"),
          prison = repository.lookupAgency("MDI"),
        ),
      )
      appointment4 = repository.save(
        OffenderIndividualSchedule(
          offenderBooking = offenderAtMoorlands.latestBooking(),
          eventSubType = repository.lookupEventSubtype("MEOP"),
          eventStatus = repository.lookupEventStatusCode("SCH"),
          // should never find this
          eventType = "OTHER",
        ),
      )
    }

    @AfterEach
    internal fun deleteAppointments() {
      repository.delete(appointment1)
      repository.delete(appointment2)
      repository.delete(appointment3)
      repository.delete(appointment4)
    }

    @Test
    fun `prison filter missing`() {
      webTestClient.get().uri("/appointments/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Missing request parameter")
        }
    }

    @Test
    fun `get all ids - prisons specified`() {
      webTestClient.get()
        .uri {
          it.path("/appointments/ids")
            .queryParam("prisonIds", "MDI", "SWI", "BXI")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(3)
    }

    @Test
    fun `get appointments issued within a given date range 1`() {
      webTestClient.get().uri {
        it.path("/appointments/ids")
          .queryParam("prisonIds", "MDI")
          .queryParam("fromDate", "2000-01-01")
          .queryParam("toDate", "2023-01-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(1)
        .jsonPath("$.content[0].eventId").isEqualTo(appointment1.eventId)
    }

    @Test
    fun `get appointments issued within a given date range 2`() {
      webTestClient.get().uri {
        it.path("/appointments/ids")
          .queryParam("prisonIds", "MDI")
          .queryParam("fromDate", "2023-01-03")
          .queryParam("toDate", "2026-01-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(1)
        .jsonPath("$.content[0].eventId").isEqualTo(appointment3.eventId)
    }

    @Test
    fun `get appointments issued within a given prison`() {
      webTestClient.get().uri {
        it.path("/appointments/ids")
          .queryParam("prisonIds", "MDI", "SWI")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content[0].eventId").isEqualTo(appointment1.eventId)
        .jsonPath("$.content[1].eventId").isEqualTo(appointment3.eventId)
        .jsonPath("$.numberOfElements").isEqualTo(2)
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/appointments/ids")
          .queryParam("prisonIds", "MDI", "BXI", "LEI")
          .queryParam("size", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
        .jsonPath("$.content[0].eventId").isEqualTo(appointment1.eventId)
        .jsonPath("$.content[1].eventId").isEqualTo(appointment2.eventId)
    }

    @Test
    fun `can request a different page`() {
      webTestClient.get().uri {
        it.path("/appointments/ids")
          .queryParam("prisonIds", "MDI", "BXI", "LEI")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
        .jsonPath("$.content[0].eventId").isEqualTo(appointment3.eventId)
    }

    @Test
    fun `malformed date returns bad request`() {
      webTestClient.get().uri {
        it.path("/appointments/ids")
          .queryParam("prisonIds", "MDI", "SWI", "LEI")
          .queryParam("fromDate", "202-10-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `get appointments prevents access without appropriate role`() {
      assertThat(
        webTestClient.get().uri {
          it.path("/appointments/ids")
            .queryParam("prisonIds", "MDI")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get appointments prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/appointments/ids")
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }

  @Nested
  inner class GetAppointmentCounts {

    private lateinit var appointment1: OffenderIndividualSchedule
    private lateinit var appointment2: OffenderIndividualSchedule
    private lateinit var appointment3: OffenderIndividualSchedule
    private lateinit var appointment4: OffenderIndividualSchedule
    private val today = LocalDate.now()
    private val yesterday = today.minusDays(1)
    private val tomorrow = today.plusDays(1)

    @BeforeEach
    internal fun createAppointments() {
      appointment1 = repository.save(
        OffenderIndividualSchedule(
          offenderBooking = offenderAtMoorlands.latestBooking(),
          eventDate = today,
          eventSubType = repository.lookupEventSubtype("MEDE"),
          eventStatus = repository.lookupEventStatusCode("SCH"),
          prison = repository.lookupAgency("MDI"),
        ),
      )
      appointment2 = repository.save(
        OffenderIndividualSchedule(
          offenderBooking = offenderAtOtherPrison.latestBooking(),
          eventDate = tomorrow,
          eventSubType = repository.lookupEventSubtype("MEDE"),
          eventStatus = repository.lookupEventStatusCode("SCH"),
          prison = repository.lookupAgency("BXI"),
        ),
      )
      appointment3 = repository.save(
        OffenderIndividualSchedule(
          offenderBooking = offenderAtMoorlands.latestBooking(),
          eventDate = yesterday,
          eventSubType = repository.lookupEventSubtype("MEOP"),
          eventStatus = repository.lookupEventStatusCode("SCH"),
          prison = repository.lookupAgency("MDI"),
        ),
      )
      appointment4 = repository.save(
        OffenderIndividualSchedule(
          offenderBooking = offenderAtMoorlands.latestBooking(),
          eventDate = tomorrow,
          eventSubType = repository.lookupEventSubtype("MEOP"),
          eventStatus = repository.lookupEventStatusCode("SCH"),
          prison = repository.lookupAgency("MDI"),
        ),
      )
    }

    @AfterEach
    internal fun deleteAppointments() {
      repository.delete(appointment1)
      repository.delete(appointment2)
      repository.delete(appointment3)
      repository.delete(appointment4)
    }

    @Nested
    inner class Errors {
      @Test
      fun `should return unauthorized without a token`() {
        assertThat(
          webTestClient.get().uri("/appointments/counts")
            .exchange()
            .expectStatus().isUnauthorized,
        )
      }

      @Test
      fun `should return forbidden without a role`() {
        assertThat(
          webTestClient.get()
            .uri {
              it.path("/appointments/counts")
                .queryParam("prisonIds", "MDI")
                .build()
            }
            .headers(setAuthorisation(roles = listOf("")))
            .exchange()
            .expectStatus().isUnauthorized,
        )
      }

      @Test
      fun `should return forbidden with wrong role`() {
        assertThat(
          webTestClient.get()
            .uri {
              it.path("/appointments/counts")
                .queryParam("prisonIds", "MDI")
                .build()
            }
            .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
            .exchange()
            .expectStatus().isForbidden,
        )
      }

      @Test
      fun `should return bad request if no prisons`() {
        webTestClient.get().uri("/appointments/counts")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Missing request parameter")
          }
      }

      @Test
      fun `should return bad request if invalid from date format`() {
        webTestClient.get()
          .uri {
            it.path("/appointments/counts")
              .queryParam("prisonIds", "MDI")
              .queryParam("fromDate", "1/2/2024")
              .build()
          }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("1/2/2024")
          }
      }

      @Test
      fun `should return bad request if invalid to date format`() {
        webTestClient.get()
          .uri {
            it.path("/appointments/counts")
              .queryParam("prisonIds", "MDI")
              .queryParam("toDate", "1/2/2024")
              .build()
          }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("1/2/2024")
          }
      }

      @Test
      fun `should return bad request if invalid from date`() {
        webTestClient.get()
          .uri {
            it.path("/appointments/counts")
              .queryParam("prisonIds", "MDI")
              .queryParam("fromDate", "2024-02-31")
              .build()
          }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("2024-02-31")
          }
      }

      @Test
      fun `should return bad request if invalid to date`() {
        webTestClient.get()
          .uri {
            it.path("/appointments/counts")
              .queryParam("prisonIds", "MDI")
              .queryParam("toDate", "2024-02-31")
              .build()
          }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("2024-02-31")
          }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should return correct counts`() {
        webTestClient.get()
          .uri {
            it.path("/appointments/counts")
              .queryParam("prisonIds", "MDI, BXI")
              .build()
          }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(4)
          .jsonPath("$[0].prisonId").isEqualTo("BXI")
          .jsonPath("$[0].eventSubType").isEqualTo("MEDE")
          .jsonPath("$[0].future").isEqualTo(true)
          .jsonPath("$[0].count").isEqualTo(1)
          .jsonPath("$[1].prisonId").isEqualTo("MDI")
          .jsonPath("$[1].eventSubType").isEqualTo("MEDE")
          .jsonPath("$[1].future").isEqualTo(false)
          .jsonPath("$[1].count").isEqualTo(1)
          .jsonPath("$[2].prisonId").isEqualTo("MDI")
          .jsonPath("$[2].eventSubType").isEqualTo("MEOP")
          .jsonPath("$[2].future").isEqualTo(false)
          .jsonPath("$[2].count").isEqualTo(1)
          .jsonPath("$[3].prisonId").isEqualTo("MDI")
          .jsonPath("$[3].eventSubType").isEqualTo("MEOP")
          .jsonPath("$[3].future").isEqualTo(true)
          .jsonPath("$[3].count").isEqualTo(1)
      }

      @Test
      fun `should filter by prison`() {
        webTestClient.get()
          .uri {
            it.path("/appointments/counts")
              .queryParam("prisonIds", "BXI")
              .build()
          }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(1)
          .jsonPath("$[0].prisonId").isEqualTo("BXI")
          .jsonPath("$[0].eventSubType").isEqualTo("MEDE")
          .jsonPath("$[0].future").isEqualTo(true)
          .jsonPath("$[0].count").isEqualTo(1)
      }

      @Test
      fun `should filter by from date`() {
        webTestClient.get()
          .uri {
            it.path("/appointments/counts")
              .queryParam("prisonIds", "MDI, BXI")
              .queryParam("fromDate", "$tomorrow")
              .build()
          }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(2)
          .jsonPath("$[0].prisonId").isEqualTo("BXI")
          .jsonPath("$[0].eventSubType").isEqualTo("MEDE")
          .jsonPath("$[0].future").isEqualTo(true)
          .jsonPath("$[0].count").isEqualTo(1)
          .jsonPath("$[1].prisonId").isEqualTo("MDI")
          .jsonPath("$[1].eventSubType").isEqualTo("MEOP")
          .jsonPath("$[1].future").isEqualTo(true)
          .jsonPath("$[1].count").isEqualTo(1)
      }

      @Test
      fun `should filter by to date`() {
        webTestClient.get()
          .uri {
            it.path("/appointments/counts")
              .queryParam("prisonIds", "MDI, BXI")
              .queryParam("toDate", "$today")
              .build()
          }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(2)
          .jsonPath("$[0].prisonId").isEqualTo("MDI")
          .jsonPath("$[0].eventSubType").isEqualTo("MEDE")
          .jsonPath("$[0].future").isEqualTo(false)
          .jsonPath("$[0].count").isEqualTo(1)
          .jsonPath("$[1].prisonId").isEqualTo("MDI")
          .jsonPath("$[1].eventSubType").isEqualTo("MEOP")
          .jsonPath("$[1].future").isEqualTo(false)
          .jsonPath("$[1].count").isEqualTo(1)
      }

      @Test
      fun `should filter by prison, from date and to date`() {
        webTestClient.get()
          .uri {
            it.path("/appointments/counts")
              .queryParam("prisonIds", "MDI")
              .queryParam("fromDate", "$today")
              .queryParam("toDate", "$tomorrow")
              .build()
          }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_APPOINTMENTS")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.size()").isEqualTo(2)
          .jsonPath("$[0].prisonId").isEqualTo("MDI")
          .jsonPath("$[0].eventSubType").isEqualTo("MEDE")
          .jsonPath("$[0].future").isEqualTo(false)
          .jsonPath("$[0].count").isEqualTo(1)
          .jsonPath("$[1].prisonId").isEqualTo("MDI")
          .jsonPath("$[1].eventSubType").isEqualTo("MEOP")
          .jsonPath("$[1].future").isEqualTo(true)
          .jsonPath("$[1].count").isEqualTo(1)
      }
    }
  }
}
