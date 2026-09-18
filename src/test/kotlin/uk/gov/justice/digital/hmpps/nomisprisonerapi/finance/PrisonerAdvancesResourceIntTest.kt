package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDate

@WithMockAuthUser
class PrisonerAdvancesResourceIntTest : IntegrationTestBase() {
  private lateinit var advance: OffenderAdvance
  private lateinit var advance2: OffenderAdvance
  private lateinit var advance3: OffenderAdvance

  @BeforeEach
  fun setUp() {
    nomisDataBuilder.build {
      offender {
        advance = advance()
      }
      offender(nomsId = "A1234BC")
      offender(nomsId = "A6789CD") {
        advance2 = advance()
        advance3 = advance()
        scheduledPayment()
      }
    }
  }

  @AfterEach
  fun tearDown() {
    deleteOffenders()
  }

  @Nested
  @DisplayName("GET /finance/prisoners/{prisonNumber}/advances")
  inner class PrisonerAdvanceByPrisonNumberTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/A5194DY/advances")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/A5194DY/advances")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/A5194DY/advances")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerAdvances() {
      webTestClient.get().uri("/finance/prisoners/A5194DY/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
        .jsonPath("[0].id").isEqualTo(advance.id)
        .jsonPath("[0].prisonNumber").isEqualTo("A5194DY")
        .jsonPath("[0].caseloadId").isEqualTo("MDI")
        .jsonPath("[0].transactionType").isEqualTo("TELE")
        .jsonPath("[0].advanceAmount").isEqualTo(1245)
        .jsonPath("[0].advanceDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].repaymentAmount").isEqualTo(245)
        .jsonPath("[0].startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].createdBy").isEqualTo("SA")
        // TODO
        .jsonPath("[0].status").isEqualTo("TODO")
    }

    @Test
    fun getPrisonerAdvancesEmptyList() {
      webTestClient.get().uri("/finance/prisoners/A1234BC/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("[]")
    }

    @Test
    fun getPrisonerAdvancesMultiple() {
      webTestClient.get().uri("/finance/prisoners/A6789CD/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(2)
        .jsonPath("[0].id").isEqualTo(advance2.id)
        .jsonPath("[1].id").isEqualTo(advance3.id)
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/advances/{advanceId}")
  inner class GetPrisonerAdvanceTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/advances/${advance.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/advances/${advance.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/advances/${advance.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerAdvances() {
      webTestClient.get().uri("/finance/prisoners/advances/${advance.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("id").isEqualTo(advance.id)
        .jsonPath("prisonNumber").isEqualTo("A5194DY")
        .jsonPath("caseloadId").isEqualTo("MDI")
        .jsonPath("transactionType").isEqualTo("TELE")
        .jsonPath("advanceAmount").isEqualTo(1245)
        .jsonPath("advanceDate").isEqualTo("2026-09-18")
        .jsonPath("repaymentAmount").isEqualTo(245)
        .jsonPath("startDate").isEqualTo("2026-09-18")
        .jsonPath("createdBy").isEqualTo("SA")
        // TODO
        .jsonPath("status").isEqualTo("TODO")
    }

    @Test
    fun getPrisonerAdvanceDoesNotExist() {
      webTestClient.get().uri("/finance/prisoners/advances/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Offender advance with id 99999 not found")
        }
    }
  }
}
