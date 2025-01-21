package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

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
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderKeyDateAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentenceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.StoredProcedureRepository
import java.time.LocalDate
import java.time.LocalDateTime

class SentencingAdjustmentsResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  lateinit var prisoner: Offender
  var bookingId: Long = 0

  @MockitoSpyBean
  private lateinit var spRepository: StoredProcedureRepository

  @BeforeEach
  internal fun createPrisoner() {
    nomisDataBuilder.build {
      prisoner = offender(nomsId = "A1234TT") {
        booking {
          sentence {}
        }
      }
    }
    bookingId = prisoner.latestBooking().bookingId
  }

  @AfterEach
  internal fun deletePrisoner() {
    repository.delete(prisoner)
  }

  @Nested
  @DisplayName("GET /key-date-adjustments/{adjustmentId}")
  inner class GetKeyDateAdjustment {
    private lateinit var anotherPrisoner: Offender
    private var adjustmentIdOnActiveBooking: Long = 0
    private var adjustmentIdOnInActiveBooking: Long = 0

    @BeforeEach
    internal fun createPrisoner() {
      nomisDataBuilder.build {
        anotherPrisoner = offender(nomsId = "A1234TX") {
          booking(agencyLocationId = "MDI") {
            sentence {}
            adjustmentIdOnActiveBooking = adjustment {}.id
          }
          booking(bookingBeginDate = LocalDateTime.now().minusDays(2)) {
            sentence {}
            adjustmentIdOnInActiveBooking = adjustment {}.id
            release(date = LocalDateTime.now().minusDays(1))
          }
        }
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(anotherPrisoner)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/key-date-adjustments/$adjustmentIdOnActiveBooking")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/key-date-adjustments/$adjustmentIdOnActiveBooking")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/key-date-adjustments/$adjustmentIdOnActiveBooking")
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
      webTestClient.get().uri("/key-date-adjustments/$adjustmentIdOnActiveBooking")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(adjustmentIdOnActiveBooking)
        .jsonPath("offenderNo").isEqualTo("A1234TX")
        .jsonPath("bookingSequence").isEqualTo("1")
    }

    @Test
    fun `release flag and prison is populated`() {
      webTestClient.get().uri("/key-date-adjustments/$adjustmentIdOnActiveBooking")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("hasBeenReleased").isEqualTo(false)
        .jsonPath("prisonId").isEqualTo("MDI")
      webTestClient.get().uri("/key-date-adjustments/$adjustmentIdOnInActiveBooking")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("hasBeenReleased").isEqualTo(true)
        .jsonPath("prisonId").isEqualTo("OUT")
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
              """,
            ),
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
              """,
            ),
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
              """,
            ),
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
              """,
            ),
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
              """,
            ),
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
          .jsonPath("adjustmentDate").doesNotExist()
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
              """,
            ),
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
          eq(bookingId),
        )
      }

      @Test
      fun `will track telemetry for the create`() {
        val adjustmentId = webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicKeyDateAdjustmentRequest()),
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
          isNull(),
        )
      }

      @Test
      fun `will call store procedure to audit key date adjustment create`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicKeyDateAdjustmentRequest()),
          )
          .exchange()
          .expectStatus().isCreated
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
      lateinit var adjustment: OffenderKeyDateAdjustment
      lateinit var booking: OffenderBooking
      nomisDataBuilder.build {
        anotherPrisoner = offender(nomsId = "A1239TX") {
          booking = booking {
            sentence {}
            adjustment = adjustment(active = false) {}
          }
        }
      }

      bookingId = booking.bookingId
      adjustmentId = adjustment.id
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
              """,
            ),
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
              """,
            ),
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
              """,
            ),
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
              """,
            ),
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
              """,
            ),
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
          .jsonPath("adjustmentDate").doesNotExist()
          .jsonPath("adjustmentFromDate").isEqualTo("2023-01-02")
          .jsonPath("adjustmentToDate").isEqualTo("2023-01-11")
          .jsonPath("comment").doesNotExist()
          .jsonPath("active").isEqualTo(false)
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
              "active": true
              }
              """,
            ),
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
          .jsonPath("active").isEqualTo(true)
      }

      @Test
      fun `will call store procedure to update key date adjustments`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicKeyDateAdjustmentUpdateRequest()),
          )
          .exchange()
          .expectStatus().isOk

        verify(spRepository).postKeyDateAdjustmentUpsert(
          eq(adjustmentId),
          eq(bookingId),
        )
      }

      @Test
      fun `will call store procedure to audit key date adjustment update`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicKeyDateAdjustmentUpdateRequest()),
          )
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `will track telemetry for the update`() {
        webTestClient.put().uri("/key-date-adjustments/$adjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicKeyDateAdjustmentUpdateRequest()),
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
          isNull(),
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
      lateinit var adjustment: OffenderKeyDateAdjustment
      lateinit var booking: OffenderBooking
      nomisDataBuilder.build {
        anotherPrisoner = offender(nomsId = "A1239TX") {
          booking = booking {
            sentence {}
            adjustment = adjustment {}
          }
        }
      }
      adjustmentId = adjustment.id
      bookingId = booking.bookingId
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
        isNull(),
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
        isNull(),
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
        eq(bookingId),
      )
    }
  }

  @Nested
  @DisplayName("GET /sentence-adjustments/{adjustmentId}")
  inner class GetSentenceAdjustment {
    private lateinit var anotherPrisoner: Offender
    private lateinit var dummyPrisoner: Offender
    private var adjustmentIdOnActiveBooking: Long = 0
    private var adjustmentIdOnInActiveBooking: Long = 0
    private var keyDateRelatedAdjustmentId: Long = 0

    @BeforeEach
    internal fun createPrisoner() {
      lateinit var adjustment: OffenderKeyDateAdjustment
      nomisDataBuilder.build {
        dummyPrisoner = offender(nomsId = "A1238TX") {
          booking {
            adjustment = adjustment {}
          }
        }
      }
      // hack to create a key-date adjustment, so I have a valid ID for the next adjustment
      val keyDateAdjustmentId = adjustment.id

      nomisDataBuilder.build {
        anotherPrisoner = offender(nomsId = "A1234TX") {
          booking(agencyLocationId = "MDI") {
            sentence {
              adjustmentIdOnActiveBooking = adjustment {}.id
            }
            sentence {
              keyDateRelatedAdjustmentId = adjustment(keyDateAdjustmentId = keyDateAdjustmentId).id
            }
          }
          booking(bookingBeginDate = LocalDateTime.now().minusDays(2)) {
            sentence {
              adjustmentIdOnInActiveBooking = adjustment {}.id
            }
            release(date = LocalDateTime.now().minusDays(1))
          }
        }
      }
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(anotherPrisoner)
      repository.delete(dummyPrisoner)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/sentence-adjustments/$adjustmentIdOnActiveBooking")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/sentence-adjustments/$adjustmentIdOnActiveBooking")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/sentence-adjustments/$adjustmentIdOnActiveBooking")
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
      webTestClient.get().uri("/sentence-adjustments/$adjustmentIdOnActiveBooking")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(adjustmentIdOnActiveBooking)
        .jsonPath("bookingSequence").isEqualTo("1")
        .jsonPath("offenderNo").isEqualTo("A1234TX")
        .jsonPath("hiddenFromUsers").isEqualTo(false)
    }

    @Test
    internal fun `adjustment has hidden flag set when related to a key date adjustment`() {
      webTestClient.get().uri("/sentence-adjustments/$keyDateRelatedAdjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("id").isEqualTo(keyDateRelatedAdjustmentId)
        .jsonPath("hiddenFromUsers").isEqualTo(true)
    }

    @Test
    fun `release flag and prison is populated`() {
      webTestClient.get().uri("/sentence-adjustments/$adjustmentIdOnActiveBooking")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("prisonId").isEqualTo("MDI")
        .jsonPath("bookingSequence").isEqualTo("1")
        .jsonPath("hasBeenReleased").isEqualTo(false)
      webTestClient.get().uri("/sentence-adjustments/$adjustmentIdOnInActiveBooking")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("prisonId").isEqualTo("OUT")
        .jsonPath("bookingSequence").isEqualTo("2")
        .jsonPath("hasBeenReleased").isEqualTo(true)
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
              """,
            ),
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
              """,
            ),
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
              """,
            ),
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
              """,
            ),
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
              """,
            ),
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
          .jsonPath("adjustmentDate").doesNotExist()
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
              """,
            ),
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
            BodyInserters.fromValue(createBasicSentenceAdjustmentRequest()),
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
          isNull(),
        )
      }

      @Test
      fun `will call store procedure to audit sentence adjustment create`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/sentences/1/adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicSentenceAdjustmentRequest()),
          )
          .exchange()
          .expectStatus().isCreated
      }
    }

    @Nested
    inner class WithMultipleSentences {
      private lateinit var anotherPrisoner: Offender
      private var anotherBookingId: Long = 0

      @BeforeEach
      internal fun createPrisoner() {
        lateinit var booking: OffenderBooking
        nomisDataBuilder.build {
          anotherPrisoner = offender(nomsId = "A1234TT") {
            booking = booking {
              sentence { }
              sentence {
                adjustment {}
              }
            }
          }
        }

        anotherBookingId = booking.bookingId
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
                """,
              ),
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
          .jsonPath("adjustmentDate").doesNotExist()
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
                """,
              ),
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
    var sentenceSequence = 0L
    var anotherSentenceSequence = 0L

    @BeforeEach
    internal fun createPrisoner() {
      lateinit var booking: OffenderBooking
      lateinit var adjustment: OffenderSentenceAdjustment
      nomisDataBuilder.build {
        anotherPrisoner = offender(nomsId = "A1239TX") {
          booking = booking {
            sentenceSequence = sentence {
              adjustment = adjustment(active = false) {}
            }.id.sequence
            anotherSentenceSequence = sentence {
            }.id.sequence
          }
        }
      }

      bookingId = booking.bookingId
      sentenceAdjustmentId = adjustment.id
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
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest(sentenceSequence = sentenceSequence)))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest(sentenceSequence = sentenceSequence)))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest(sentenceSequence = sentenceSequence)))
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
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest(sentenceSequence = sentenceSequence)))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence adjustment with id 999 not found")
      }

      @Test
      internal fun `404 when sentence sequence does not exist`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest(sentenceSequence = 99)))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Sentence with sequence 99 not found")
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
              "adjustmentTypeCode": "RX",
              "sentenceSequence": $sentenceSequence
              }
              """,
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("adjustmentDays must be greater than or equal to 0")
      }

      @Test
      internal fun `400 when sentence sequence not present in request`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
              {
              "adjustmentTypeCode": "RX",
              "adjustmentDays": 10
              }
              """,
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("sentenceSequence must be greater than or equal to 0")
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
              "sentenceSequence": $sentenceSequence,
              "adjustmentTypeCode": "BANANAS"
              }
              """,
            ),
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
              "sentenceSequence": $sentenceSequence,
              "adjustmentTypeCode": "ADA"
              }
              """,
            ),
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
              "sentenceSequence": $sentenceSequence,
              "adjustmentDays": 10
              }
              """,
            ),
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
              "sentenceSequence": $sentenceSequence,
              "adjustmentDays": 10,
              "adjustmentTypeCode": "RX"
              }
              """,
            ),
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
          .jsonPath("sentenceSequence").isEqualTo(sentenceSequence)
          .jsonPath("adjustmentType.code").isEqualTo("RX")
          .jsonPath("adjustmentType.description").isEqualTo("Remand")
          .jsonPath("adjustmentDays").isEqualTo(10)
          .jsonPath("adjustmentDate").doesNotExist()
          .jsonPath("adjustmentFromDate").doesNotExist()
          .jsonPath("adjustmentToDate").doesNotExist()
          .jsonPath("comment").doesNotExist()
          .jsonPath("active").isEqualTo(false)
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
              "sentenceSequence": $anotherSentenceSequence,
              "adjustmentDays": 12,
              "adjustmentTypeCode": "RST",
              "adjustmentDate": "2023-01-18",
              "adjustmentFromDate": "2023-01-02",
              "comment": "12 days",
              "active": true
              }
              """,
            ),
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
          .jsonPath("sentenceSequence").isEqualTo(anotherSentenceSequence)
          .jsonPath("adjustmentType.code").isEqualTo("RST")
          .jsonPath("adjustmentType.description").isEqualTo("Recall Sentence Tagged Bail")
          .jsonPath("adjustmentDate").isEqualTo("2023-01-18")
          .jsonPath("adjustmentFromDate").isEqualTo("2023-01-02")
          .jsonPath("adjustmentToDate").isEqualTo("2023-01-13")
          .jsonPath("adjustmentDays").isEqualTo(12)
          .jsonPath("comment").isEqualTo("12 days")
          .jsonPath("active").isEqualTo(true)
      }

      @Test
      fun `will track telemetry for the update`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest(sentenceSequence = sentenceSequence)),
          )
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("sentence-adjustment-updated"),
          check {
            assertThat(it).containsEntry("adjustmentId", sentenceAdjustmentId.toString())
            assertThat(it).containsEntry("bookingId", bookingId.toString())
            assertThat(it).containsEntry("sentenceSequence", "$sentenceSequence")
            assertThat(it).containsEntry("offenderNo", "A1239TX")
            assertThat(it).containsEntry("adjustmentType", "RX")
          },
          isNull(),
        )
      }

      @Test
      fun `will call store procedure to audit sentence adjustment update`() {
        webTestClient.put().uri("/sentence-adjustments/$sentenceAdjustmentId")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(createBasicSentenceAdjustmentUpdateRequest(sentenceSequence = sentenceSequence)),
          )
          .exchange()
          .expectStatus().isOk
      }
    }

    fun createBasicSentenceAdjustmentUpdateRequest(sentenceSequence: Long) = """
    {
    "adjustmentDays": 10,
    "adjustmentTypeCode": "RX",
    "sentenceSequence": $sentenceSequence
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
      lateinit var booking: OffenderBooking
      lateinit var adjustment: OffenderSentenceAdjustment
      nomisDataBuilder.build {
        anotherPrisoner = offender(nomsId = "A1234TX") {
          booking = booking {
            sentence {
              adjustment = adjustment(adjustmentTypeCode = "UR")
            }
          }
        }
      }

      bookingId = booking.bookingId
      adjustmentId = adjustment.id
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
        isNull(),
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
        isNull(),
      )
    }

    @Test
    fun `will call store procedure to audit sentence adjustment update`() {
      webTestClient.delete().uri("/sentence-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent
    }
  }

  @Nested
  @DisplayName("GET /adjustments/ids")
  inner class GetAdjustmentIds {
    private lateinit var anotherPrisoner: Offender

    @BeforeEach
    internal fun createPrisoner() {
      nomisDataBuilder.build {
        anotherPrisoner = offender(nomsId = "A1234TX") {
          booking {
            sentence {
              adjustment(createdDate = LocalDateTime.of(2023, 1, 1, 13, 30))
              adjustment(createdDate = LocalDateTime.of(2023, 1, 5, 13, 30))
              adjustment(createdDate = LocalDateTime.of(2023, 1, 10, 13, 30))
            }
            adjustment(createdDate = LocalDateTime.of(2023, 1, 2, 13, 30))
            adjustment(createdDate = LocalDateTime.of(2023, 1, 3, 13, 30))
            adjustment(createdDate = LocalDateTime.of(2023, 1, 15, 13, 30))
          }
        }
      }
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
        .jsonPath("$.content[1].adjustmentCategory").isEqualTo("KEY-DATE")
        .jsonPath("$.content[2].adjustmentCategory").isEqualTo("KEY-DATE")
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

  @Nested
  @DisplayName("GET /prisoners/booking-id/{bookingId}/sentencing-adjustments")
  inner class GetAdjustmentByBookingId {
    private lateinit var anotherPrisoner: Offender
    private var bookingId: Long = 0
    private lateinit var linkedUal: OffenderKeyDateAdjustment
    private lateinit var ual: OffenderSentenceAdjustment

    @BeforeEach
    internal fun createPrisoner() {
      nomisDataBuilder.build {
        anotherPrisoner = offender(nomsId = "A1234TX") {
          booking {
            adjustment(adjustmentTypeCode = "LAL", active = false)
            linkedUal = adjustment(adjustmentTypeCode = "UAL")
            adjustment(adjustmentTypeCode = "ADA")
            sentence {
              adjustment(adjustmentTypeCode = "RSR")
              adjustment(adjustmentTypeCode = "S240A")
              ual = adjustment(adjustmentTypeCode = "UAL", keyDateAdjustmentId = linkedUal.id)
              adjustment(adjustmentTypeCode = "RX", active = false)
            }
          }
        }
      }

      bookingId = anotherPrisoner.latestBooking().bookingId
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(ual)
      repository.delete(anotherPrisoner)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/booking-id/{bookingId}/sentencing-adjustments", bookingId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/booking-id/{bookingId}/sentencing-adjustments", bookingId)
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/booking-id/{bookingId}/sentencing-adjustments", bookingId)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `get all active adjustments ignoring linked key date adjustments by default`() {
      webTestClient.get().uri("/prisoners/booking-id/{bookingId}/sentencing-adjustments", bookingId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.keyDateAdjustments.size()").isEqualTo(2)
        .jsonPath("$.keyDateAdjustments[0].adjustmentType.code").isEqualTo("UAL")
        .jsonPath("$.keyDateAdjustments[1].adjustmentType.code").isEqualTo("ADA")
        .jsonPath("$.sentenceAdjustments.size()").isEqualTo(2)
        .jsonPath("$.sentenceAdjustments[0].adjustmentType.code").isEqualTo("RSR")
        .jsonPath("$.sentenceAdjustments[1].adjustmentType.code").isEqualTo("S240A")
    }

    @Test
    fun `can get all adjustments regardless of active status`() {
      webTestClient.get().uri("/prisoners/booking-id/{bookingId}/sentencing-adjustments?active-only=false", bookingId)
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.keyDateAdjustments.size()").isEqualTo(3)
        .jsonPath("$.keyDateAdjustments[0].adjustmentType.code").isEqualTo("LAL")
        .jsonPath("$.keyDateAdjustments[0].active").isEqualTo(false)
        .jsonPath("$.keyDateAdjustments[1].adjustmentType.code").isEqualTo("UAL")
        .jsonPath("$.keyDateAdjustments[1].active").isEqualTo(true)
        .jsonPath("$.keyDateAdjustments[2].adjustmentType.code").isEqualTo("ADA")
        .jsonPath("$.keyDateAdjustments[2].active").isEqualTo(true)
        .jsonPath("$.sentenceAdjustments.size()").isEqualTo(3)
        .jsonPath("$.sentenceAdjustments[0].adjustmentType.code").isEqualTo("RSR")
        .jsonPath("$.sentenceAdjustments[0].active").isEqualTo(true)
        .jsonPath("$.sentenceAdjustments[1].adjustmentType.code").isEqualTo("S240A")
        .jsonPath("$.sentenceAdjustments[1].active").isEqualTo(true)
        .jsonPath("$.sentenceAdjustments[2].adjustmentType.code").isEqualTo("RX")
        .jsonPath("$.sentenceAdjustments[2].active").isEqualTo(false)
    }
  }
}
