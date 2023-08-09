package uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIndividualSchedule
import java.time.LocalDate
import java.time.LocalDateTime

class NonAssociationsResourceIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  lateinit var offenderAtMoorlands: Offender
  lateinit var offenderAtOtherPrison: Offender

  private fun callCreateEndpoint() {
    webTestClient.post().uri("/non-associations")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
      .contentType(MediaType.APPLICATION_JSON)
      .body(BodyInserters.fromValue(validCreateJsonRequest()))
      .exchange()
      .expectStatus().isCreated
      .expectBody().isEmpty()
  }

  private fun validCreateJsonRequest() = """{
            "offenderNo"    : ${offenderAtMoorlands.nomsId},
            "nsOffenderNo"  : ${offenderAtOtherPrison.nomsId},
            "reason"        : "RIV",
            "recipReason"   : "PER",
            "type"          : "WING",
            "authorisedBy"  : "me!",
            "effectiveDate" : "2023-02-27",
            "comment"       : "this is a test!"
          }
  """.trimIndent()

  private fun callCloseEndpoint(eventId: Long) {
    webTestClient.put().uri("/non-associations/$eventId/close")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
      .exchange()
      .expectStatus().isOk
  }

  @BeforeEach
  internal fun createPrisoners() {
    nomisDataBuilder.build {
      offenderAtMoorlands =
        offender(nomsId = "A1234TT") {
          booking(agencyLocationId = "MDI")
        }
      offenderAtOtherPrison =
        offender(nomsId = "A1234TU") {
          booking(agencyLocationId = "LEI")
        }
    }
  }

  @AfterEach
  internal fun deleteData() {
    repository.deleteNonAssociation(offenderAtMoorlands, offenderAtOtherPrison)
    repository.delete(offenderAtMoorlands)
    repository.delete(offenderAtOtherPrison)
  }

  @Nested
  inner class CreateNonAssociation {

    private val createNonAssociationRequest: () -> CreateNonAssociationRequest = {
      CreateNonAssociationRequest(
        offenderNo = offenderAtMoorlands.nomsId,
        nsOffenderNo = offenderAtOtherPrison.nomsId,
        reason = "RIV",
        recipReason = "PER",
        type = "WING",
        authorisedBy = "me!",
        effectiveDate = LocalDate.parse("2023-07-27"),
        comment = "this is a test!",
      )
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/non-associations")
        .body(BodyInserters.fromValue(createNonAssociationRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createNonAssociationRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `offender not found`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(offenderNo = "A0000AA")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Offender with nomsId=A0000AA not found")
        }
    }

    @Test
    fun `nsOffender not found`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(nsOffenderNo = "A0000AA")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("NS Offender with nomsId=A0000AA not found")
        }
    }

    @Test
    fun `invalid reason`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(reason = "invalid")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Reason with code=invalid does not exist")
        }
    }

    @Test
    fun `invalid reciprocal reason`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(recipReason = "invalid")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Reciprocal reason with code=invalid does not exist")
        }
    }

    @Test
    fun `invalid type`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(type = "INVALID")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Type with code=INVALID does not exist")
        }
    }

    @Test
    fun `effectiveDate in the future`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(effectiveDate = LocalDate.now().plusDays(1))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Effective date must be in the pastafter start time")
        }
    }

    @Test
    fun `comment too long`() {
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(createNonAssociationRequest().copy(comment = "x".repeat(241))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Comment is too long (max allowed 240 characters)")
        }
    }

    @Test
    fun `invalid date should return bad request`() {
      val invalidSchedule = validCreateJsonRequest().replace(
        """"effectiveDate" : "2023-02-27"""",
        """"effectiveDate" : "2023-13-27"""",
      )
      webTestClient.post().uri("/non-associations")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(invalidSchedule))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("2023-13-27 xxx")
        }
    }

    @Test
    fun `will create appointment with correct details`() {
      callCreateEndpoint()

      // Check the database
      repository.getNonAssociation(offenderAtMoorlands, offenderAtOtherPrison).apply {

        assertThat(id.offender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(id.nsOffender.nomsId).isEqualTo(offenderAtOtherPrison.nomsId)
        assertThat(offenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nsOffenderBooking.bookingId).isEqualTo(offenderAtOtherPrison.latestBooking().bookingId)
        assertThat(nonAssociationReason?.code).isEqualTo("PER")
        assertThat(recipNonAssociationReason?.code).isEqualTo("PER")
        val nd = offenderNonAssociationDetails.first()
        assertThat(nd.id.offender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(nd.id.nsOffender.nomsId).isEqualTo(offenderAtOtherPrison.nomsId)
        assertThat(nd.id.typeSequence).isEqualTo(1)
        assertThat(nd.offenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nd.nsOffenderBooking.bookingId).isEqualTo(offenderAtOtherPrison.latestBooking().bookingId)
        assertThat(nd.nonAssociationReason.code).isEqualTo("RIV")
        assertThat(nd.recipNonAssociationReason?.code).isNull()
        assertThat(nd.effectiveDate).isEqualTo(LocalDate.parse("2023-02-27"))
        assertThat(nd.nonAssociationType.code).isEqualTo("WING")
        assertThat(nd.authorisedBy).isEqualTo("Me!")
        assertThat(nd.comment).isEqualTo("this is a test!")
        assertThat(nd.nonAssociation).isEqualTo(this)
      }
      repository.getNonAssociation(offenderAtOtherPrison, offenderAtMoorlands).apply {

        assertThat(id.offender.nomsId).isEqualTo(offenderAtOtherPrison.nomsId)
        assertThat(id.nsOffender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(offenderBooking.bookingId).isEqualTo(offenderAtOtherPrison.latestBooking().bookingId)
        assertThat(nsOffenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nonAssociationReason?.code).isEqualTo("PER")
        assertThat(recipNonAssociationReason?.code).isEqualTo("RIV")
        val nd = offenderNonAssociationDetails.first()
        assertThat(nd.id.offender.nomsId).isEqualTo(offenderAtOtherPrison.nomsId)
        assertThat(nd.id.nsOffender.nomsId).isEqualTo(offenderAtMoorlands.nomsId)
        assertThat(nd.id.typeSequence).isEqualTo(1)
        assertThat(nd.offenderBooking.bookingId).isEqualTo(offenderAtOtherPrison.latestBooking().bookingId)
        assertThat(nd.nsOffenderBooking.bookingId).isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
        assertThat(nd.nonAssociationReason.code).isEqualTo("PER")
        assertThat(nd.recipNonAssociationReason?.code).isNull()
        assertThat(nd.effectiveDate).isEqualTo(LocalDate.parse("2023-02-27"))
        assertThat(nd.nonAssociationType.code).isEqualTo("WING")
        assertThat(nd.authorisedBy).isEqualTo("Me!")
        assertThat(nd.comment).isEqualTo("this is a test!")
        assertThat(nd.nonAssociation).isEqualTo(this)
      }
    }
  }

  @Nested
  inner class UpdateNonAssociation {

    private val updateNonAssociationRequest: () -> UpdateNonAssociationRequest = {
      UpdateNonAssociationRequest(
        reason = "BUL",
        recipReason = "VIC",
        type = "LAND",
        authorisedBy = "me!",
        effectiveDate = LocalDate.parse("2023-07-30"),
        comment = "this is a modified test!",
      )
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/non-associations/1")
        .body(BodyInserters.fromValue(updateNonAssociationRequest()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/non-associations/1")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(updateNonAssociationRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put().uri("/non-associations/1")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `appointment does not exist`() {
      webTestClient.put().uri("/non-associations/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest()))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `invalid reason`() {
      callCreateEndpoint()
      webTestClient.put().uri("/non-associations/xxxx")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest().copy(reason = "invalid")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Reason with code=invalid does not exist")
        }
    }

    @Test
    fun `invalid reciprocal reason`() {
      callCreateEndpoint()
      webTestClient.put().uri("/non-associations/xxx")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest().copy(recipReason = "invalid")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Reciprocal reason with code=invalid does not exist")
        }
    }

    @Test
    fun `invalid type`() {
      callCreateEndpoint()
      webTestClient.put().uri("/non-associations/xxx")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest().copy(type = "INVALID")))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Type with code=INVALID does not exist")
        }
    }

    @Test
    fun `effectiveDate in the future`() {
      callCreateEndpoint()
      webTestClient.put().uri("/non-associations/xxx")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest().copy(effectiveDate = LocalDate.now().plusDays(1))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("11:65")
        }
    }

    @Test
    fun `comment too long`() {
      callCreateEndpoint()
      webTestClient.put().uri("/non-associations/xxx")
        .contentType(MediaType.APPLICATION_JSON)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .body(BodyInserters.fromValue(updateNonAssociationRequest().copy(comment = "x".repeat(241))))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("Comment is too long (max allowed 240 characters)")
        }
    }

    @Test
    fun `invalid date should return bad request`() {
      val invalidSchedule = validUpdateJsonRequest(false).replace(
        """"effectiveDate" : "2023-02-27"""",
        """"effectiveDate" : "2023-13-27"""",
      )
      val eventId = callCreateEndpoint()
      webTestClient.put().uri("/non-associations/$eventId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(invalidSchedule))
        .exchange()
        .expectStatus().isBadRequest
        .expectBody().jsonPath("$.userMessage").value<String> {
          assertThat(it).contains("??")
        }
    }

//    @Test
//    fun `will update appointment with correct details`() {
//      val eventId = callCreateEndpoint()
//      callUpdateEndpoint(eventId, true)
//
//      // Check the database
//      val offenderIndividualSchedule = repository.getNonAssociation(eventId)!!
//
//      assertThat(offenderIndividualSchedule.eventId).isEqualTo(eventId)
//      assertThat(offenderIndividualSchedule.offenderBooking.bookingId)
//        .isEqualTo(offenderAtMoorlands.latestBooking().bookingId)
//      assertThat(offenderIndividualSchedule.eventDate).isEqualTo(LocalDate.parse("2023-02-28"))
//      assertThat(offenderIndividualSchedule.startTime).isEqualTo(LocalDateTime.parse("2023-02-28T10:50"))
//      assertThat(offenderIndividualSchedule.endTime).isEqualTo(LocalDateTime.parse("2023-02-28T12:20"))
//      assertThat(offenderIndividualSchedule.eventSubType.code).isEqualTo("CABA")
//      assertThat(offenderIndividualSchedule.prison?.id).isEqualTo("MDI")
//      assertThat(offenderIndividualSchedule.comment).isEqualTo("Some comment")
//      assertThat(offenderIndividualSchedule.internalLocation?.locationId).isEqualTo(MDI_ROOM_ID_2)
//      assertThat(offenderIndividualSchedule.modifiedBy).isEqualTo("SA")
//      assertThat(offenderIndividualSchedule.modifiedBy).isNotBlank()
//    }
//
//    @Test
//    fun `will update appointment with correct details - no end time`() {
//      val eventId = callCreateEndpoint()
//      callUpdateEndpoint(eventId, false)
//
//      // Check the database
//      val offenderIndividualSchedule = repository.getNonAssociation(eventId)!!
//
//      assertThat(offenderIndividualSchedule.eventId).isEqualTo(eventId)
//      assertThat(offenderIndividualSchedule.eventDate).isEqualTo(LocalDate.parse("2023-02-28"))
//      assertThat(offenderIndividualSchedule.startTime).isEqualTo(LocalDateTime.parse("2023-02-28T10:50"))
//      assertThat(offenderIndividualSchedule.endTime).isNull()
//    }

    private fun callUpdateEndpoint(eventId: Long, hasEndTime: Boolean) {
      webTestClient.put().uri("/non-associations/$eventId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(validUpdateJsonRequest(hasEndTime)))
        .exchange()
        .expectStatus().isOk
    }

    private fun validUpdateJsonRequest(hasEndTime: Boolean) = """{
            "eventDate"          : "2023-02-28",
            "startTime"          : "10:50",
${if (hasEndTime) """"endTime"   : "12:20",""" else ""}
            "comment"            : "Some comment",
            "eventSubType"       : "CABA"
          }
    """.trimIndent()
  }

  @Nested
  inner class CloseNonAssociation {

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.put().uri("/non-associations/1/close")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.put().uri("/non-associations/1/close")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.put().uri("/non-associations/1/close")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `appointment does not exist`() {
      webTestClient.put().uri("/non-associations/1/close")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

//    @Test
//    fun `will close appointment correctly`() {
//      val eventId = callCreateEndpoint()
//      callCloseEndpoint(eventId)
//
//      // Check the database
//      val offenderIndividualSchedule = repository.getNonAssociation(offenderAtMoorlands, offenderAtOtherPrison)!!
//
//      assertThat(offenderIndividualSchedule.eventId).isEqualTo(eventId)
//      assertThat(offenderIndividualSchedule.eventStatus.code).isEqualTo("CANC")
//    }
  }

  @Nested
  inner class GetNonAssociationById {

    private lateinit var appointment1: OffenderIndividualSchedule

    @BeforeEach
    internal fun createNonAssociations() {
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
    internal fun deleteNonAssociations() {
      repository.delete(appointment1)
    }

    @Test
    fun `get by id`() {
      webTestClient.get().uri("/non-associations/${appointment1.eventId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
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
      webTestClient.get().uri("/non-associations/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `malformed id returns bad request`() {
      webTestClient.get().uri("/non-associations/stuff")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `get appointments prevents access without appropriate role`() {
      assertThat(
        webTestClient.get().uri("/non-associations/1")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get appointments prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/non-associations/1")
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }

  @Nested
  inner class GetNonAssociationIdsByFilterRequest {

    private lateinit var appointment1: OffenderIndividualSchedule
    private lateinit var appointment2: OffenderIndividualSchedule
    private lateinit var appointment3: OffenderIndividualSchedule
    private lateinit var appointment4: OffenderIndividualSchedule

    @BeforeEach
    internal fun createNonAssociations() {
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
          eventType = "OTHER", // should never find this
        ),
      )
    }

    @AfterEach
    internal fun deleteNonAssociations() {
      repository.delete(appointment1)
      repository.delete(appointment2)
      repository.delete(appointment3)
      repository.delete(appointment4)
    }

    @Test
    fun `get all ids - no filter specified`() {
      webTestClient.get().uri("/non-associations/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(3)
    }

    @Test
    fun `get appointments issued within a given date range 1`() {
      webTestClient.get().uri {
        it.path("/non-associations/ids")
          .queryParam("fromDate", "2000-01-01")
          .queryParam("toDate", "2023-01-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(1)
        .jsonPath("$.content[0].eventId").isEqualTo(appointment1.eventId)
    }

    @Test
    fun `get appointments issued within a given date range 2`() {
      webTestClient.get().uri {
        it.path("/non-associations/ids")
          .queryParam("fromDate", "2023-01-03")
          .queryParam("toDate", "2026-01-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(1)
        .jsonPath("$.content[0].eventId").isEqualTo(appointment3.eventId)
    }

    @Test
    fun `get appointments issued within a given prison`() {
      webTestClient.get().uri {
        it.path("/non-associations/ids")
          .queryParam("prisonIds", "MDI", "SWI")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
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
        it.path("/non-associations/ids")
          .queryParam("size", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
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
        it.path("/non-associations/ids")
          .queryParam("size", "2")
          .queryParam("page", "1")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(3)
        .jsonPath("numberOfElements").isEqualTo(1)
        .jsonPath("number").isEqualTo(1)
        .jsonPath("totalPages").isEqualTo(2)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `malformed date returns bad request`() {
      webTestClient.get().uri {
        it.path("/non-associations/ids")
          .queryParam("fromDate", "202-10-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_NON_ASSOCIATIONS")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `get appointments prevents access without appropriate role`() {
      assertThat(
        webTestClient.get().uri("/non-associations/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get appointments prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/non-associations/ids")
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }
}
