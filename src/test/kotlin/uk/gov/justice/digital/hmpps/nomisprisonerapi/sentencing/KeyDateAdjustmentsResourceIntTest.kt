package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.KeyDateAdjustmentBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class KeyDateAdjustmentsResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository
  lateinit var prisoner: Offender
  var bookingId: Long = 0

  @SpyBean
  private lateinit var spRepository: StoredProcedureRepository

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
  @DisplayName("GET /key-date-adjustments/{adjustmentId}")
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
                    "adjustmentTypeCode": "LAL",
                    "adjustmentFromDate": "2023-01-01"
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
                    "adjustmentTypeCode": "BANANAS",
                    "adjustmentFromDate": "2023-01-01"
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
                    "adjustmentTypeCode": "RX",
                    "adjustmentFromDate": "2023-01-01"
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
                    "adjustmentDays": 10,
                    "adjustmentFromDate": "2023-01-01"
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
                      "adjustmentTypeCode": "LAL",
                      "adjustmentFromDate": "2023-01-01"
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
          .jsonPath("adjustmentFromDate").isEqualTo("2023-01-01")
          .jsonPath("adjustmentToDate").isEqualTo("2023-01-10")
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

        verify(spRepository).postKeyDateAdjustmentCreation(
          eq(adjustmentId),
          eq(bookingId)
        )
      }
    }

    fun createBasicKeyDateAdjustmentRequest() = """
      {
        "adjustmentDays": 10,
        "adjustmentTypeCode": "ADA",
        "adjustmentFromDate": "2023-01-01"
      }
    """.trimIndent()
  }

  @Nested
  @DisplayName("PUT /key-date-adjustments/{adjustmentId}")
  inner class UpdateKeyDateAdjustment {
    lateinit var anotherPrisoner: Offender
    var adjustmentId: Long = 0
    var bookingId: Long = 0

    @BeforeEach
    internal fun createPrisoner() {
      anotherPrisoner = repository.save(
        OffenderBuilder(nomsId = "A1239TX")
          .withBooking(
            OffenderBookingBuilder()
              .withKeyDateAdjustments(KeyDateAdjustmentBuilder())
          )
      )
      bookingId = anotherPrisoner.bookings.first().bookingId
      adjustmentId = anotherPrisoner.bookings.first().keyDateAdjustments.first().id
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(anotherPrisoner)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicKeyDateAdjustmentUpdateRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicKeyDateAdjustmentUpdateRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicKeyDateAdjustmentUpdateRequest()))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      internal fun `404 when adjustment id does not exist`() {
        webTestClient.put().uri("/key-date-adjustments/999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicKeyDateAdjustmentUpdateRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Key date adjustment with id 999 not found")
      }

      @Test
      internal fun `400 when days not present in request`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                  {
                    "adjustmentTypeCode": "RX",
                    "adjustmentFromDate": "2023-01-02"
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
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                  {
                    "adjustmentDays": 10,
                    "adjustmentTypeCode": "BANANAS",
                    "adjustmentFromDate": "2023-01-02"
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
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                  {
                    "adjustmentDays": 10,
                    "adjustmentTypeCode": "RX",
                    "adjustmentFromDate": "2023-01-02"
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
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                  {
                    "adjustmentDays": 10,
                    "adjustmentFromDate": "2023-01-02"
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
    inner class WhenAdjustmentIsUpdated {
      @Test
      fun `can update an adjustment with minimal data keeping data that is not updatable`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                    {
                      "adjustmentDays": 10,
                      "adjustmentTypeCode": "ADA",
                      "adjustmentFromDate": "2023-01-02"
                    }
                  """
            )
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(adjustmentId)
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("adjustmentType.code").isEqualTo("ADA")
          .jsonPath("adjustmentType.description").isEqualTo("Additional Days Awarded")
          .jsonPath("adjustmentDays").isEqualTo(10)
          .jsonPath("adjustmentDate").isEqualTo(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
          .jsonPath("adjustmentFromDate").isEqualTo("2023-01-02")
          .jsonPath("adjustmentToDate").isEqualTo("2023-01-11")
          .jsonPath("comment").doesNotExist()
          .jsonPath("active").isEqualTo(true)
      }

      @Test
      fun `can update most of adjustment data`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
                    {
                      "adjustmentDays": 12,
                      "adjustmentTypeCode": "ADA",
                      "adjustmentDate": "2023-01-18",
                      "adjustmentFromDate": "2023-01-02",
                      "comment": "12 days",
                      "active": false
                    }
                  """
            )
          )
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(adjustmentId)
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("adjustmentType.code").isEqualTo("ADA")
          .jsonPath("adjustmentType.description").isEqualTo("Additional Days Awarded")
          .jsonPath("adjustmentDate").isEqualTo("2023-01-18")
          .jsonPath("adjustmentFromDate").isEqualTo("2023-01-02")
          .jsonPath("adjustmentToDate").isEqualTo("2023-01-13")
          .jsonPath("adjustmentDays").isEqualTo(12)
          .jsonPath("comment").isEqualTo("12 days")
          .jsonPath("active").isEqualTo(false)
      }

      @Test
      fun `will call store procedure to update sentence adjustments`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicKeyDateAdjustmentUpdateRequest())
          )
          .exchange()
          .expectStatus().isOk

        verify(spRepository).postKeyDateAdjustmentCreation(
          eq(adjustmentId),
          eq(bookingId)
        )
      }
    }

    fun createBasicKeyDateAdjustmentUpdateRequest() = """
      {
        "adjustmentDays": 10,
        "adjustmentTypeCode": "ADA",
        "adjustmentFromDate": "2023-01-02"
      }
    """.trimIndent()
  }

  @Nested
  @DisplayName("DELETE /key-date-adjustments/{adjustmentId}")
  inner class DeleteKeyDateAdjustment {
    private lateinit var anotherPrisoner: Offender
    var adjustmentId: Long = 0
    var bookingId: Long = 0

    @BeforeEach
    internal fun createPrisoner() {
      anotherPrisoner = repository.save(
        OffenderBuilder(nomsId = "A1239TX")
          .withBooking(
            OffenderBookingBuilder()
              .withKeyDateAdjustments(KeyDateAdjustmentBuilder())
          )
      )
      adjustmentId = anotherPrisoner.bookings.first().keyDateAdjustments.first().id
      bookingId = anotherPrisoner.bookings.first().bookingId
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(anotherPrisoner)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/key-date-adjustments/$adjustmentId")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    internal fun `204 even when adjustment does not exist`() {
      webTestClient.get().uri("/key-date-adjustments/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound

      webTestClient.delete().uri("/key-date-adjustments/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent
    }
    @Test
    internal fun `204 when adjustment does exist`() {
      webTestClient.get().uri("/key-date-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/key-date-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/key-date-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }
    @Test
    fun `will call store procedure to delete sentence adjustments`() {
      webTestClient.delete().uri("/key-date-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(spRepository).postKeyDateAdjustmentCreation(
        eq(adjustmentId),
        eq(bookingId)
      )
    }
  }
}
