package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.KeyDateAdjustmentBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class KeyDateAdjustmentsResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository
  lateinit var prisoner: Offender
  var bookingId: Long = 0

  @BeforeEach
  internal fun createPrisoner() {
    prisoner = repository.save(
      OffenderBuilder(nomsId = "A1234TT")
        .withBooking(
          OffenderBookingBuilder()
        )
    )
    bookingId = prisoner.bookings.first().bookingId
  }

  @Nested
  @DisplayName("GET /key-date-adjustments/{keyDateAdjustmentId}")
  inner class GetKeyDateAdjustment {
    lateinit var anotherPrisoner: Offender
    var adjustmentId: Long = 0

    @BeforeEach
    internal fun createPrisoner() {
      anotherPrisoner = repository.save(
        OffenderBuilder(nomsId = "A1234TX")
          .withBooking(
            OffenderBookingBuilder()
              .withKeyDateAdjustments(KeyDateAdjustmentBuilder())
          )
      )
      adjustmentId = anotherPrisoner.bookings.first().keyDateAdjustments.first().id
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/key-date-adjustments/$adjustmentId")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    internal fun `404 when adjustment does not exist`() {
      webTestClient.get().uri("/key-date-adjustments/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }
    @Test
    internal fun `200 when adjustment does exist`() {
      webTestClient.get().uri("/key-date-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(adjustmentId)
    }
  }

  @Nested
  @DisplayName("POST /prisoners/booking-id/{bookingId}/adjustments")
  inner class CreateKeyDateAdjustment {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicKeyDateAdjustmentRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicKeyDateAdjustmentRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicKeyDateAdjustmentRequest()))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      internal fun `404 when booking id does not exist`() {
        webTestClient.post().uri("/prisoners/booking-id/999/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicKeyDateAdjustmentRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Booking 999 not found")
      }

      @Test
      internal fun `400 when days not present in request`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                  {
                    "adjustmentTypeCode": "LAL"
                  }
                """
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("adjustmentDays must be greater than or equal to 0")
      }

      @Test
      internal fun `400 when adjustment type not valid`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                  {
                    "adjustmentDays": 10,
                    "adjustmentTypeCode": "BANANAS"
                  }
                """
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence adjustment type BANANAS not found")
      }

      @Test
      internal fun `400 when adjustment type is for a sentence not booking`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                  {
                    "adjustmentDays": 10,
                    "adjustmentTypeCode": "RX"
                  }
                """
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence adjustment type RX not valid for a booking")
      }

      @Test
      internal fun `400 when adjustment type not present in request`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                  {
                    "adjustmentDays": 10
                  }
                """
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("adjustmentTypeCode must not be blank")
      }
    }

    @Nested
    inner class CreateKeyDateAdjustmentSuccess {
      @Test
      fun `can create an adjustment with minimal data`() {
        val adjustmentId = webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                    {
                      "adjustmentDays": 10,
                      "adjustmentTypeCode": "LAL"
                    }
                  """
            )
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateAdjustmentResponse::class.java)
          .returnResult().responseBody!!.id

        webTestClient.get().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(adjustmentId)
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("adjustmentType.code").isEqualTo("LAL")
          .jsonPath("adjustmentType.description").isEqualTo("Lawfully at Large")
          .jsonPath("adjustmentDays").isEqualTo(10)
          .jsonPath("adjustmentDate").isEqualTo(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
          .jsonPath("adjustmentFromDate").doesNotExist()
          .jsonPath("adjustmentToDate").doesNotExist()
          .jsonPath("comment").doesNotExist()
          .jsonPath("active").isEqualTo(true)
      }

      @Test
      fun `can create an adjustment with all data`() {
        val adjustmentId = webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                    {
                      "adjustmentDays": 10,
                      "adjustmentTypeCode": "LAL",
                      "adjustmentDate": "2023-01-16",
                      "adjustmentFromDate": "2023-01-01",
                      "comment": "a comment",
                      "active": false
                    }
                  """
            )
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateAdjustmentResponse::class.java)
          .returnResult().responseBody!!.id

        webTestClient.get().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(adjustmentId)
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("adjustmentType.code").isEqualTo("LAL")
          .jsonPath("adjustmentType.description").isEqualTo("Lawfully at Large")
          .jsonPath("adjustmentDate").isEqualTo("2023-01-16")
          .jsonPath("adjustmentFromDate").isEqualTo("2023-01-01")
          .jsonPath("adjustmentToDate").isEqualTo("2023-01-10")
          .jsonPath("adjustmentDays").isEqualTo(10)
          .jsonPath("comment").isEqualTo("a comment")
          .jsonPath("active").isEqualTo(false)
      }
    }

    fun createBasicKeyDateAdjustmentRequest() = """
      {
        "adjustmentDays": 10,
        "adjustmentTypeCode": "ADA"
      }
    """.trimIndent()
  }
}
