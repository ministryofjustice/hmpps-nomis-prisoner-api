package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

@WithMockAuthUser
@TestInstance(PER_CLASS)
class PrisonerAdvancesResourceIntTest : IntegrationTestBase() {
  private lateinit var advance: OffenderAdvance
  private lateinit var advance2: OffenderAdvance
  private lateinit var advance3: OffenderAdvance
  private var id1: Long = 0
  private var id2: Long = 0
  private var id3: Long = 0

  @BeforeAll
  fun setUp() {
    nomisDataBuilder.build {
      id1 = offender {
        trustAccount()
        advance = advance(informationNumber = "12341234-2", deductionPriority = 1)
      }.id
      id2 = offender(nomsId = "A1234BC").id
      id3 = offender(nomsId = "A6789CD") {
        trustAccount()
        advance2 = advance(informationNumber = "10002000", deductionPriority = 2)
        // paid off
        advance3 = advance(paymentAmount = BigDecimal.ONE, deductionAmount = BigDecimal.TEN, informationNumber = "10002000-1", deductionPriority = 3)
        scheduledPayment()
      }.id
    }
  }

  @AfterAll
  fun tearDown() {
    deleteOffenders()
  }

  @Nested
  @DisplayName("GET /finance/prisoners/{prisonNumber}/advances")
  inner class PrisonerAdvancesByPrisonNumberTests {
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
        .jsonPath("[0].advanceAmount").isEqualTo(1000)
        .jsonPath("[0].advanceDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].repaymentAmount").isEqualTo(100)
        .jsonPath("[0].startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].createdBy").isEqualTo("SA")
        .jsonPath("[0].createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("[0].informationNumber").isEqualTo("12341234-2")
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

    @Test
    fun getActivePrisonerAdvances() {
      webTestClient.get().uri("/finance/prisoners/A6789CD/advances?activeOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .consumeWith(::println)
        .jsonPath("size()").isEqualTo(1)
        .jsonPath("[0].id").isEqualTo(advance2.id)
        .jsonPath("[0].prisonNumber").isEqualTo("A6789CD")
        .jsonPath("[0].caseloadId").isEqualTo("MDI")
        .jsonPath("[0].advanceAmount").isEqualTo(1000)
        .jsonPath("[0].advanceDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].repaymentAmount").isEqualTo(100)
        .jsonPath("[0].startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].createdBy").isEqualTo("SA")
        .jsonPath("[0].createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("[0].informationNumber").isEqualTo("10002000")
        .jsonPath("[0].status").isEqualTo("ACTIVE")
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/root-offender-id/{rootOffenderId}/advances")
  inner class PrisonerAdvancesByRootOffenderIdTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/root-offender-id/$id1/advances")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/root-offender-id/$id1/advances")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/root-offender-id/$id1/advances")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerAdvances() {
      webTestClient.get().uri("/finance/prisoners/root-offender-id/$id1/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
        .jsonPath("[0].id").isEqualTo(advance.id)
        .jsonPath("[0].prisonNumber").isEqualTo("A5194DY")
        .jsonPath("[0].caseloadId").isEqualTo("MDI")
        .jsonPath("[0].advanceAmount").isEqualTo(1000)
        .jsonPath("[0].advanceDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].repaymentAmount").isEqualTo(100)
        .jsonPath("[0].startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].createdBy").isEqualTo("SA")
        .jsonPath("[0].createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("[0].informationNumber").isEqualTo("12341234-2")
        // TODO
        .jsonPath("[0].status").isEqualTo("TODO")
    }

    @Test
    fun getPrisonerAdvancesEmptyList() {
      webTestClient.get().uri("/finance/prisoners/root-offender-id/$id2/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("[]")
    }

    @Test
    fun getPrisonerAdvancesMultiple() {
      webTestClient.get().uri("/finance/prisoners/root-offender-id/$id3/advances")
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
        .jsonPath("advanceAmount").isEqualTo(1000)
        .jsonPath("advanceDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("repaymentAmount").isEqualTo(100)
        .jsonPath("startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("createdBy").isEqualTo("SA")
        .jsonPath("createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("informationNumber").isEqualTo("12341234-2")
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
