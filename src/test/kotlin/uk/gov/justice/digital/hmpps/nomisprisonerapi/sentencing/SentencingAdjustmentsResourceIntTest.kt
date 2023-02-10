package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.KeyDateAdjustmentBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.SentenceAdjustmentBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.SentenceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SentencingAdjustmentsResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository
  lateinit var prisoner: Offender
  var bookingId: Long = 0

  @SpyBean
  private lateinit var spRepository: StoredProcedureRepository

  @SpyBean
  private lateinit var telemetryClient: TelemetryClient

  @BeforeEach
  internal fun createPrisoner() {
    prisoner = repository.save(
      OffenderBuilder(nomsId = "A1234TT")
        .withBooking(
          OffenderBookingBuilder()
            .withSentences(SentenceBuilder())
        )
    )
    bookingId = prisoner.bookings.first().bookingId
  }

  @AfterEach
  internal fun deletePrisoner() {
    repository.delete(prisoner)
  }

  @Nested
  @DisplayName("GET /key-date-adjustments/{adjustmentId}")
  inner class GetKeyDateAdjustment {
    private lateinit var anotherPrisoner: Offender
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

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(anotherPrisoner)
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

        verify(spRepository).postKeyDateAdjustmentUpsert(
          eq(adjustmentId),
          eq(bookingId)
        )
      }

      @Test
      fun `will track telemetry for the create`() {
        val adjustmentId = webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicKeyDateAdjustmentRequest())
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateAdjustmentResponse::class.java)
          .returnResult().responseBody!!.id

        verify(telemetryClient).trackEvent(
          eq("key-date-adjustment-created"),
          check {
            assertThat(it).containsEntry("adjustmentId", adjustmentId.toString())
            assertThat(it).containsEntry("bookingId", bookingId.toString())
            assertThat(it).containsEntry("offenderNo", "A1234TT")
            assertThat(it).containsEntry("adjustmentType", "ADA")
          },
          isNull()
        )
      }

      @Test
      fun `will call store procedure to audit key date adjustment create`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicKeyDateAdjustmentRequest())
          )
          .exchange()
          .expectStatus().isCreated

        verify(spRepository).audit("DPS_SYNCHRONISATION")
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
      fun `will call store procedure to update key date adjustments`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicKeyDateAdjustmentUpdateRequest())
          )
          .exchange()
          .expectStatus().isOk

        verify(spRepository).postKeyDateAdjustmentUpsert(
          eq(adjustmentId),
          eq(bookingId)
        )
      }
      @Test
      fun `will call store procedure to audit key date adjustment update`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicKeyDateAdjustmentUpdateRequest())
          )
          .exchange()
          .expectStatus().isOk

        verify(spRepository).audit("DPS_SYNCHRONISATION")
      }

      @Test
      fun `will track telemetry for the update`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicKeyDateAdjustmentUpdateRequest())
          )
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("key-date-adjustment-updated"),
          check {
            assertThat(it).containsEntry("adjustmentId", adjustmentId.toString())
            assertThat(it).containsEntry("bookingId", bookingId.toString())
            assertThat(it).containsEntry("offenderNo", "A1239TX")
            assertThat(it).containsEntry("adjustmentType", "ADA")
          },
          isNull()
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

      verify(telemetryClient).trackEvent(
        eq("key-date-adjustment-delete-not-found"),
        check {
          assertThat(it).containsEntry("adjustmentId", "9999")
        },
        isNull()
      )
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
    fun `will track telemetry for the delete`() {
      webTestClient.delete().uri("/key-date-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(telemetryClient).trackEvent(
        eq("key-date-adjustment-deleted"),
        check {
          assertThat(it).containsEntry("adjustmentId", adjustmentId.toString())
          assertThat(it).containsEntry("bookingId", bookingId.toString())
          assertThat(it).containsEntry("offenderNo", "A1239TX")
          assertThat(it).containsEntry("adjustmentType", "ADA")
        },
        isNull()
      )
    }

    @Test
    fun `will call store procedure to delete sentence adjustments`() {
      webTestClient.delete().uri("/key-date-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(spRepository).preKeyDateAdjustmentDeletion(
        eq(adjustmentId),
        eq(bookingId)
      )
    }
    @Test
    fun `will call audit store procedure to record key date adjustments delete`() {
      webTestClient.delete().uri("/key-date-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(spRepository).audit("DPS_SYNCHRONISATION")
    }
  }

  @Nested
  @DisplayName("GET /sentence-adjustments/{adjustmentId}")
  inner class GetSentenceAdjustment {
    private lateinit var anotherPrisoner: Offender
    var adjustmentId: Long = 0

    @BeforeEach
    internal fun createPrisoner() {
      anotherPrisoner = repository.save(
        OffenderBuilder(nomsId = "A1234TX")
          .withBooking(
            OffenderBookingBuilder()
              .withSentences(SentenceBuilder().withAdjustment())
          )
      )
      adjustmentId = anotherPrisoner.bookings.first().sentences.first().adjustments.first().id
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(anotherPrisoner)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/sentence-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/sentence-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/sentence-adjustments/$adjustmentId")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    internal fun `404 when adjustment does not exist`() {
      webTestClient.get().uri("/sentence-adjustments/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    internal fun `200 when adjustment does exist`() {
      webTestClient.get().uri("/sentence-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(adjustmentId)
    }
  }

  @Nested
  @DisplayName("POST /prisoners/booking-id/{bookingId}/sentences/{sentenceSequence}/adjustments")
  inner class CreateSentenceAdjustment {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentRequest()))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      internal fun `404 when booking id does not exist`() {
        webTestClient.post().uri("/prisoners/booking-id/999/sentences/1/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Booking 999 not found")
      }

      @Test
      internal fun `404 when sentence does not exist`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/999/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence with sequence 999 not found")
      }

      @Test
      internal fun `400 when days not present in request`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
              {
              "adjustmentTypeCode": "RX"
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
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
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
      internal fun `400 when adjustment type is for a booking not sentence`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
              {
              "adjustmentDays": 10,
              "adjustmentTypeCode": "ADA"
              }
              """
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence adjustment type ADA not valid for a sentence")
      }

      @Test
      internal fun `400 when adjustment type not present in request`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
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
    inner class WithOneSentence {
      @Test
      fun `can create an adjustment with minimal data`() {
        val sentenceAdjustmentId = webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
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
          .expectStatus().isCreated.expectBody(CreateAdjustmentResponse::class.java)
          .returnResult().responseBody!!.id

        webTestClient.get().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(sentenceAdjustmentId)
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("sentenceSequence").isEqualTo(1)
          .jsonPath("adjustmentType.code").isEqualTo("RX")
          .jsonPath("adjustmentType.description").isEqualTo("Remand")
          .jsonPath("adjustmentDays").isEqualTo(10)
          .jsonPath("adjustmentDate").isEqualTo(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
          .jsonPath("adjustmentFromDate").doesNotExist()
          .jsonPath("adjustmentToDate").doesNotExist()
          .jsonPath("comment").doesNotExist()
          .jsonPath("active").isEqualTo(true)
      }

      @Test
      fun `can create an adjustment with all data`() {
        val sentenceAdjustmentId = webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
              {
              "adjustmentDays": 10,
              "adjustmentTypeCode": "RX",
              "adjustmentDate": "2023-01-16",
              "adjustmentFromDate": "2023-01-01",
              "comment": "Remand for 10 days",
              "active": false
              }
              """
            )
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateAdjustmentResponse::class.java)
          .returnResult().responseBody!!.id

        webTestClient.get().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(sentenceAdjustmentId)
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("sentenceSequence").isEqualTo(1)
          .jsonPath("adjustmentType.code").isEqualTo("RX")
          .jsonPath("adjustmentType.description").isEqualTo("Remand")
          .jsonPath("adjustmentDate").isEqualTo("2023-01-16")
          .jsonPath("adjustmentFromDate").isEqualTo("2023-01-01")
          .jsonPath("adjustmentToDate").isEqualTo("2023-01-10")
          .jsonPath("adjustmentDays").isEqualTo(10)
          .jsonPath("comment").isEqualTo("Remand for 10 days")
          .jsonPath("active").isEqualTo(false)
      }

      @Test
      fun `will track telemetry for the create`() {
        val adjustmentId = webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicSentenceAdjustmentRequest())
          )
          .exchange()
          .expectStatus().isCreated.expectBody(CreateAdjustmentResponse::class.java)
          .returnResult().responseBody!!.id

        verify(telemetryClient).trackEvent(
          eq("sentence-adjustment-created"),
          check {
            assertThat(it).containsEntry("adjustmentId", adjustmentId.toString())
            assertThat(it).containsEntry("bookingId", bookingId.toString())
            assertThat(it).containsEntry("sentenceSequence", "1")
            assertThat(it).containsEntry("offenderNo", "A1234TT")
            assertThat(it).containsEntry("adjustmentType", "RX")
          },
          isNull()
        )
      }

      @Test
      fun `will call store procedure to audit sentence adjustment create`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicSentenceAdjustmentRequest())
          )
          .exchange()
          .expectStatus().isCreated

        verify(spRepository).audit("DPS_SYNCHRONISATION")
      }
    }

    @Nested
    inner class WithMultipleSentences {
      private lateinit var anotherPrisoner: Offender
      private var anotherBookingId: Long = 0

      @BeforeEach
      internal fun createPrisoner() {
        anotherPrisoner = repository.save(
          OffenderBuilder(nomsId = "A1234TT")
            .withBooking(
              OffenderBookingBuilder()
                .withSentences(SentenceBuilder(), SentenceBuilder().withAdjustment())
            )
        )
        anotherBookingId = anotherPrisoner.bookings.first().bookingId
      }

      @AfterEach
      internal fun deletePrisoner() {
        repository.delete(anotherPrisoner)
      }

      @Test
      fun `can create an adjustment with minimal data`() {
        val sentenceAdjustmentId =
          webTestClient.post().uri("/prisoners/booking-id/$anotherBookingId/sentences/2/adjustments")
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
            .expectStatus().isCreated.expectBody(CreateAdjustmentResponse::class.java)
            .returnResult().responseBody!!.id

        webTestClient.get().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(sentenceAdjustmentId)
          .jsonPath("bookingId").isEqualTo(anotherBookingId)
          .jsonPath("sentenceSequence").isEqualTo(2)
          .jsonPath("adjustmentType.code").isEqualTo("RX")
          .jsonPath("adjustmentType.description").isEqualTo("Remand")
          .jsonPath("adjustmentDays").isEqualTo(10)
          .jsonPath("adjustmentDate").isEqualTo(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
          .jsonPath("adjustmentFromDate").doesNotExist()
          .jsonPath("adjustmentToDate").doesNotExist()
          .jsonPath("comment").doesNotExist()
          .jsonPath("active").isEqualTo(true)
      }

      @Test
      fun `can create an adjustment with all data`() {
        val sentenceAdjustmentId =
          webTestClient.post().uri("/prisoners/booking-id/$anotherBookingId/sentences/2/adjustments")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                """
                {
                "adjustmentDays": 2,
                "adjustmentTypeCode": "RX",
                "adjustmentDate": "2023-01-16",
                "adjustmentFromDate": "2023-02-01",
                "comment": "Remand for 2 days",
                "active": false
                }
                """
              )
            )
            .exchange()
            .expectStatus().isCreated.expectBody(CreateAdjustmentResponse::class.java)
            .returnResult().responseBody!!.id

        webTestClient.get().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(sentenceAdjustmentId)
          .jsonPath("bookingId").isEqualTo(anotherBookingId)
          .jsonPath("sentenceSequence").isEqualTo(2)
          .jsonPath("adjustmentType.code").isEqualTo("RX")
          .jsonPath("adjustmentType.description").isEqualTo("Remand")
          .jsonPath("adjustmentDate").isEqualTo("2023-01-16")
          .jsonPath("adjustmentFromDate").isEqualTo("2023-02-01")
          .jsonPath("adjustmentToDate").isEqualTo("2023-02-02")
          .jsonPath("adjustmentDays").isEqualTo(2)
          .jsonPath("comment").isEqualTo("Remand for 2 days")
          .jsonPath("active").isEqualTo(false)
      }
    }

    fun createBasicSentenceAdjustmentRequest() = """
    {
    "adjustmentDays": 10,
    "adjustmentTypeCode": "RX"
    }
    """.trimIndent()
  }

  @Nested
  @DisplayName("PUT /sentence-adjustments/{adjustmentId}")
  inner class UpdateSentenceAdjustment {
    private lateinit var anotherPrisoner: Offender
    var sentenceAdjustmentId: Long = 0
    var bookingId: Long = 0

    @BeforeEach
    internal fun createPrisoner() {
      anotherPrisoner = repository.save(
        OffenderBuilder(nomsId = "A1239TX")
          .withBooking(
            OffenderBookingBuilder()
              .withSentences(SentenceBuilder().withAdjustment())
          )
      )
      bookingId = anotherPrisoner.bookings.first().bookingId
      sentenceAdjustmentId = anotherPrisoner.bookings.first().sentences.first().adjustments.first().id
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(anotherPrisoner)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest()))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      internal fun `404 when adjustment id does not exist`() {
        webTestClient.put().uri("/sentence-adjustments/999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence adjustment with id 999 not found")
      }

      @Test
      internal fun `400 when days not present in request`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
              {
              "adjustmentTypeCode": "RX"
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
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
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
      internal fun `400 when adjustment type is for a booking not sentence`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
              {
              "adjustmentDays": 10,
              "adjustmentTypeCode": "ADA"
              }
              """
            )
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence adjustment type ADA not valid for a sentence")
      }

      @Test
      internal fun `400 when adjustment type not present in request`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
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
    inner class WhenAdjustmentIsUpdated {
      @Test
      fun `can update an adjustment with minimal data keep data that is not updatable`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
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
          .expectStatus().isOk

        webTestClient.get().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(sentenceAdjustmentId)
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("sentenceSequence").isEqualTo(1)
          .jsonPath("adjustmentType.code").isEqualTo("RX")
          .jsonPath("adjustmentType.description").isEqualTo("Remand")
          .jsonPath("adjustmentDays").isEqualTo(10)
          .jsonPath("adjustmentDate").isEqualTo(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
          .jsonPath("adjustmentFromDate").doesNotExist()
          .jsonPath("adjustmentToDate").doesNotExist()
          .jsonPath("comment").doesNotExist()
          .jsonPath("active").isEqualTo(true)
      }

      @Test
      fun `can update most of adjustment data`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
              {
              "adjustmentDays": 12,
              "adjustmentTypeCode": "RST",
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

        webTestClient.get().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("id").isEqualTo(sentenceAdjustmentId)
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("sentenceSequence").isEqualTo(1)
          .jsonPath("adjustmentType.code").isEqualTo("RST")
          .jsonPath("adjustmentType.description").isEqualTo("Recall Sentence Tagged Bail")
          .jsonPath("adjustmentDate").isEqualTo("2023-01-18")
          .jsonPath("adjustmentFromDate").isEqualTo("2023-01-02")
          .jsonPath("adjustmentToDate").isEqualTo("2023-01-13")
          .jsonPath("adjustmentDays").isEqualTo(12)
          .jsonPath("comment").isEqualTo("12 days")
          .jsonPath("active").isEqualTo(false)
      }

      @Test
      fun `will track telemetry for the update`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest())
          )
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("sentence-adjustment-updated"),
          check {
            assertThat(it).containsEntry("adjustmentId", sentenceAdjustmentId.toString())
            assertThat(it).containsEntry("bookingId", bookingId.toString())
            assertThat(it).containsEntry("sentenceSequence", "1")
            assertThat(it).containsEntry("offenderNo", "A1239TX")
            assertThat(it).containsEntry("adjustmentType", "RX")
          },
          isNull()
        )
      }

      @Test
      fun `will call store procedure to audit sentence adjustment update`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest())
          )
          .exchange()
          .expectStatus().isOk

        verify(spRepository).audit("DPS_SYNCHRONISATION")
      }
    }

    fun createBasicSentenceAdjustmentUpdateRequest() = """
    {
    "adjustmentDays": 10,
    "adjustmentTypeCode": "RX"
    }
    """.trimIndent()
  }

  @Nested
  @DisplayName("DELETE /sentence-adjustments/{adjustmentId}")
  inner class DeleteSentenceAdjustment {
    private lateinit var anotherPrisoner: Offender
    var adjustmentId: Long = 0
    var bookingId: Long = 0

    @BeforeEach
    internal fun createPrisoner() {
      anotherPrisoner = repository.save(
        OffenderBuilder(nomsId = "A1234TX")
          .withBooking(
            OffenderBookingBuilder()
              .withSentences(SentenceBuilder().withAdjustment(SentenceAdjustmentBuilder(adjustmentTypeCode = "UR")))
          )
      )
      adjustmentId = anotherPrisoner.bookings.first().sentences.first().adjustments.first().id
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
        webTestClient.delete().uri("/sentence-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/sentence-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/sentence-adjustments/$adjustmentId")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    internal fun `204 even when adjustment does not exist`() {
      webTestClient.get().uri("/sentence-adjustments/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound

      webTestClient.delete().uri("/sentence-adjustments/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(telemetryClient).trackEvent(
        eq("sentence-adjustment-delete-not-found"),
        check {
          assertThat(it).containsEntry("adjustmentId", "9999")
        },
        isNull()
      )
    }

    @Test
    internal fun `204 when adjustment does exist`() {
      webTestClient.get().uri("/sentence-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/sentence-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      webTestClient.get().uri("/sentence-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `will track telemetry for the delete`() {
      webTestClient.delete().uri("/sentence-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(telemetryClient).trackEvent(
        eq("sentence-adjustment-deleted"),
        check {
          assertThat(it).containsEntry("adjustmentId", adjustmentId.toString())
          assertThat(it).containsEntry("bookingId", bookingId.toString())
          assertThat(it).containsEntry("sentenceSequence", "1")
          assertThat(it).containsEntry("offenderNo", "A1234TX")
          assertThat(it).containsEntry("adjustmentType", "UR")
        },
        isNull()
      )
    }
    @Test
    fun `will call store procedure to audit sentence adjustment update`() {
      webTestClient.delete().uri("/sentence-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent

      verify(spRepository).audit("DPS_SYNCHRONISATION")
    }
  }

  @Nested
  @DisplayName("GET /adjustments/ids")
  inner class GetAdjustmentIds {
    private lateinit var anotherPrisoner: Offender

    @BeforeEach
    internal fun createPrisoner() {
      anotherPrisoner = repository.save(
        OffenderBuilder(nomsId = "A1234TX")
          .withBooking(
            OffenderBookingBuilder()
              .withSentences(
                SentenceBuilder().withAdjustments(
                  SentenceAdjustmentBuilder(
                    createdDate = LocalDateTime.of(2023, 1, 1, 13, 30)
                  ),
                  SentenceAdjustmentBuilder(
                    createdDate = LocalDateTime.of(2023, 1, 5, 13, 30)
                  ),
                  SentenceAdjustmentBuilder(
                    createdDate = LocalDateTime.of(2023, 1, 10, 13, 30)
                  )
                )
              )
              .withKeyDateAdjustments(
                KeyDateAdjustmentBuilder(createdDate = LocalDateTime.of(2023, 1, 2, 13, 30)),

                KeyDateAdjustmentBuilder(createdDate = LocalDateTime.of(2023, 1, 3, 13, 30)),

                KeyDateAdjustmentBuilder(createdDate = LocalDateTime.of(2023, 1, 15, 13, 30))
              )
          )
      )
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(anotherPrisoner)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/adjustments/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/adjustments/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/adjustments/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `get all adjustment ids - no filter specified`() {
      webTestClient.get().uri("/adjustments/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(6)
    }

    @Test
    fun `get adjustments created within a given date range`() {
      webTestClient.get().uri {
        it.path("/adjustments/ids")
          .queryParam("fromDate", LocalDate.of(2023, 1, 1).toString())
          .queryParam("toDate", LocalDate.of(2023, 1, 5).toString())
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(4)
        .jsonPath("$.content[0].adjustmentCategory").isEqualTo("SENTENCE")
        .jsonPath("$.content[1].adjustmentCategory").isEqualTo("KEY_DATE")
        .jsonPath("$.content[2].adjustmentCategory").isEqualTo("KEY_DATE")
        .jsonPath("$.content[3].adjustmentCategory").isEqualTo("SENTENCE")
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/adjustments/ids")
          .queryParam("size", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(6)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(3)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `can request a different page`() {
      webTestClient.get().uri {
        it.path("/adjustments/ids")
          .queryParam("size", "2")
          .queryParam("page", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(6)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(2)
        .jsonPath("totalPages").isEqualTo(3)
        .jsonPath("size").isEqualTo(2)
    }
  }
}
