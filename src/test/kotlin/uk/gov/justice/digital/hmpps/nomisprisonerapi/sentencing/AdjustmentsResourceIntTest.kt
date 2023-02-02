package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.KeyDateAdjustmentBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.SentenceAdjustmentBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.SentenceBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.time.LocalDate
import java.time.LocalDateTime

class AdjustmentsResourceIntTest : IntegrationTestBase() {
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
  @DisplayName("GET /adjustments/ids")
  inner class GetAdjustmentIds {
    lateinit var anotherPrisoner: Offender

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
