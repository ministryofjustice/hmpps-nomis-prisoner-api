package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.ParameterizedTypeReference
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PostingType
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

@WithMockAuthUser
class PrisonTransactionResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  @BeforeEach
  fun setUp() {
    nomisDataBuilder.build {
      generalLedgerTransaction(transactionId = 1, transactionEntrySequence = 1, generalLedgerEntrySequence = 1, entryDateTime = LocalDateTime.parse("2026-02-07T12:24"))
      generalLedgerTransaction(transactionId = 1, transactionEntrySequence = 1, generalLedgerEntrySequence = 2, entryDateTime = LocalDateTime.parse("2026-02-08T12:24"))
      generalLedgerTransaction(transactionId = 2, transactionEntrySequence = 1, generalLedgerEntrySequence = 1, entryDateTime = LocalDateTime.parse("2026-02-08T12:24"))
      generalLedgerTransaction(transactionId = 2, transactionEntrySequence = 1, generalLedgerEntrySequence = 2, entryDateTime = LocalDateTime.parse("2026-02-08T12:24"))
      generalLedgerTransaction(transactionId = 2, transactionEntrySequence = 1, generalLedgerEntrySequence = 3, entryDateTime = LocalDateTime.parse("2026-02-08T12:24"))
      generalLedgerTransaction(transactionId = 3, transactionEntrySequence = 1, generalLedgerEntrySequence = 1, entryDateTime = LocalDateTime.parse("2026-02-08T12:24"))
      generalLedgerTransaction(transactionId = 3, transactionEntrySequence = 1, generalLedgerEntrySequence = 2, entryDateTime = LocalDateTime.parse("2026-02-09T12:24"))
      generalLedgerTransaction(transactionId = 4, transactionEntrySequence = 1, generalLedgerEntrySequence = 1, entryDateTime = LocalDateTime.parse("2026-02-09T12:24"))
      generalLedgerTransaction(transactionId = 5, transactionEntrySequence = 1, generalLedgerEntrySequence = 1, entryDateTime = LocalDateTime.parse("2026-02-10T12:24"))
    }
  }

  @AfterEach
  fun tearDown() {
    repository.deleteAllTransactions()
  }

  @Nested
  @DisplayName("GET /transactions/prison/{prisonId}/{date}")
  inner class PrisonTransactionTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/transactions/prison/BXI/2026-01-15")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/transactions/prison/BXI/2026-01-15")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/transactions/prison/BXI/2026-01-15")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonTransactionsWithOverlapBeforeAndAfter() {
      val results = webTestClient.get().uri("/transactions/prison/BXI/2026-02-08")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(object : ParameterizedTypeReference<List<GeneralLedgerTransactionDto>>() {})
        .returnResult()
        .responseBody!!

      assertThat(results.size).isEqualTo(7)
      assertThat(results.map { it.transactionId }).containsOnly(1, 2, 3)

      with(results.first()) {
        assertThat(transactionId).isEqualTo(1)
        assertThat(transactionEntrySequence).isEqualTo(1)
        assertThat(generalLedgerEntrySequence).isEqualTo(1)
        assertThat(caseloadId).isEqualTo("BXI")
        assertThat(amount).isEqualTo(BigDecimal(2))
        assertThat(type).isEqualTo("SPEN")
        assertThat(postingType).isEqualTo(PostingType.CR)
        assertThat(accountCode).isEqualTo(2000)
        assertThat(description).isEqualTo("entry description")
        assertThat(transactionTimestamp).isEqualTo("2026-02-07T12:24:00")
        assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        assertThat(createdBy).isEqualTo("SA")
        assertThat(createdByDisplayName).isEqualTo("PRISON USER")
      }
    }

    @Test
    fun getPrisonTransactionsWithOverlapBefore() {
      val results = webTestClient.get().uri("/transactions/prison/BXI/2026-02-09")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(object : ParameterizedTypeReference<List<GeneralLedgerTransactionDto>>() {})
        .returnResult()
        .responseBody!!

      assertThat(results.size).isEqualTo(3)
      assertThat(results.map { it.transactionId }).containsOnly(3, 4)

      with(results[0]) {
        assertThat(transactionId).isEqualTo(3)
        assertThat(transactionEntrySequence).isEqualTo(1)
        assertThat(generalLedgerEntrySequence).isEqualTo(1)
        assertThat(caseloadId).isEqualTo("BXI")
        assertThat(amount).isEqualTo(BigDecimal(2))
        assertThat(type).isEqualTo("SPEN")
        assertThat(postingType).isEqualTo(PostingType.CR)
        assertThat(accountCode).isEqualTo(2000)
        assertThat(description).isEqualTo("entry description")
        assertThat(transactionTimestamp).isEqualTo("2026-02-08T12:24:00")
        assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        assertThat(createdBy).isEqualTo("SA")
        assertThat(createdByDisplayName).isEqualTo("PRISON USER")
      }
    }

    @Test
    fun getPrisonTransactionsWithOverlapAfter() {
      webTestClient.get().uri("/transactions/prison/BXI/2026-02-07")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(2)
    }

    @Test
    fun getPrisonTransactionsSingleResult() {
      webTestClient.get().uri("/transactions/prison/BXI/2026-02-10")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
    }

    @Test
    fun `none found - missing prison`() {
      // TODO Update to return 404
      webTestClient.get().uri("/transactions/prison/XXX/2026-01-15")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(0)
    }

    @Test
    fun `none found - no matching entries`() {
      webTestClient.get().uri("/transactions/prison/BXI/2025-01-15")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(0)
    }
  }
}
