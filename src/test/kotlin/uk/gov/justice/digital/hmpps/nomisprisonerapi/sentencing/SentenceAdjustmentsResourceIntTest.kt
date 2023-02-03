package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.SentenceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SentenceAdjustmentsResourceIntTest : IntegrationTestBase() {
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
  @DisplayName("GET /sentence-adjustments/{adjustmentId}")
  inner class GetSentenceAdjustment {
    lateinit var anotherPrisoner: Offender
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
    }

    @Nested
    inner class WithMultipleSentences {
      lateinit var anotherPrisoner: Offender
      var anotherBookingId: Long = 0

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
    lateinit var anotherPrisoner: Offender
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
    lateinit var anotherPrisoner: Offender
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
        .expectBody()

      webTestClient.delete().uri("/sentence-adjustments/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent
    }
    @Test
    internal fun `204 when adjustment does exist`() {
      webTestClient.get().uri("/sentence-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isOk
        .expectBody()

      webTestClient.delete().uri("/sentence-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNoContent
        .expectBody()

      webTestClient.get().uri("/sentence-adjustments/$adjustmentId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_SENTENCING")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody()
    }
  }
}
