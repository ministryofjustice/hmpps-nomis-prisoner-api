package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@WithMockAuthUser
class FinanceResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  @Autowired
  private lateinit var service: TransactionsService

  private lateinit var offender: Offender
  private lateinit var transaction: OffenderTransaction
  private lateinit var glTransaction: GeneralLedgerTransaction

  @AfterEach
  fun tearDown() {
    repository.deleteAllTransactions()
    repository.delete(offender)
  }

  @Nested
  inner class Security {
    @Test
    fun `access forbidden when no role`() {
      webTestClient.get().uri("/transactions/99/1")
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.get().uri("/transactions/99/1")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access unauthorised with no auth token`() {
      webTestClient.get().uri("/transactions/99/1")
        .exchange()
        .expectStatus().isUnauthorized
    }
  }

  @Test
  fun getTransactionRepo() {
    nomisDataBuilder.build {
      offender = offender {
        booking {
          transaction = transaction("DPST") {
            glTransaction = generalLedgerTransaction(1, 10201)
          }
        }
      }
    }

    with(
      repository.getTransaction(OffenderTransaction.Companion.Pk(transaction.transactionId, 1))!!,
    ) {
      assertThat(transactionId).isEqualTo(transaction.transactionId)
      assertThat(transactionEntrySequence).isEqualTo(1)
      assertThat(offenderBooking?.bookingId).isEqualTo(transaction.offenderBooking!!.bookingId)
      assertThat(trustAccount).isEqualTo(transaction.trustAccount)
//      assertThat(trustAccount.id.prisonId).isEqualTo(transaction.trustAccount.id.prisonId)
//      assertThat(trustAccount.id.offender).isEqualTo(transaction.trustAccount.id.offender)
      assertThat(trustAccount.accountClosed).isFalse()
      assertThat(trustAccount.holdBalance).isCloseTo(transaction.trustAccount.holdBalance, Percentage.withPercentage(0.1))
      assertThat(trustAccount.currentBalance).isCloseTo(transaction.trustAccount.currentBalance, Percentage.withPercentage(0.1))
      assertThat(subAccountType).isEqualTo(transaction.subAccountType)
      assertThat(transactionType).isEqualTo(transaction.transactionType)
      assertThat(transactionReferenceNumber).isEqualTo(transaction.transactionReferenceNumber)
      assertThat(clientUniqueRef).isEqualTo(transaction.clientUniqueRef)
      assertThat(entryDate).isEqualTo(transaction.entryDate)
      assertThat(entryDescription).isEqualTo(transaction.entryDescription)
      assertThat(entryAmount).isCloseTo(transaction.entryAmount, Percentage.withPercentage(0.1))
      assertThat(postingType).isEqualTo(transaction.postingType)
    }
  }

  @Test
  fun getTransaction() {
    nomisDataBuilder.build {
      offender = offender {
        booking {
          transaction = transaction("DPST")
        }
      }
    }

    webTestClient.get().uri("/transactions/${transaction.transactionId}/${transaction.transactionEntrySequence}")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("transactionId").isEqualTo(transaction.transactionId)
      .jsonPath("transactionEntrySequence").isEqualTo(transaction.transactionEntrySequence)
      .jsonPath("offenderNo").isEqualTo(transaction.offenderBooking!!.offender.nomsId)
      .jsonPath("amount").value<Double> {
        assertThat(it).isCloseTo(transaction.entryAmount.toDouble(), Percentage.withPercentage(0.1))
      }
      .jsonPath("type").isEqualTo(transaction.transactionType.type)
      .jsonPath("postingType").isEqualTo(transaction.postingType.name)
      .jsonPath("entryDate").isEqualTo(transaction.entryDate)
      .jsonPath("reference").isEqualTo(transaction.clientUniqueRef!!)
      .jsonPath("subAccountType").isEqualTo(transaction.subAccountType.name)
      .jsonPath("holdBalance").value<Double> {
        assertThat(it).isCloseTo(transaction.trustAccount.holdBalance?.toDouble(), Percentage.withPercentage(0.1))
      }
      .jsonPath("currentBalance").value<Double> {
        assertThat(it).isCloseTo(transaction.trustAccount.currentBalance?.toDouble(), Percentage.withPercentage(0.1))
      }
  }
}
