package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
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
  private lateinit var service: FinanceService

  private lateinit var offender: Offender
  private lateinit var transaction: OffenderTransaction

  @AfterEach
  fun tearDown() {
    repository.deleteAllTransactions()
    repository.delete(offender)
  }

  @Test
  fun getTransactionRepo() {
    nomisDataBuilder.build {
      offender = offender {
        booking {
          transaction = transaction("DPST")
        }
      }
    }

    with(
      repository.getTransaction(OffenderTransaction.Companion.Pk(transaction.transactionId, 1))!!,
    ) {
      assertThat(transactionId).isEqualTo(transaction.transactionId)
      assertThat(transactionEntrySequence).isEqualTo(1)
      assertThat(offenderBooking?.bookingId).isEqualTo(transaction.offenderBooking!!.bookingId)
      assertThat(trustAccount.prisonId).isEqualTo(transaction.trustAccount.prisonId)
      assertThat(trustAccount.offenderId).isEqualTo(transaction.trustAccount.offenderId)
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

    with(
      service.getTransaction(transaction.transactionId, 1),
    ) {
      assertThat(transactionId).isEqualTo(transaction.transactionId)
      assertThat(transactionEntrySequence).isEqualTo(1)
      assertThat(offenderNo).isEqualTo(transaction.offenderBooking?.offender?.nomsId)
      assertThat(amount).isCloseTo(transaction.entryAmount, Percentage.withPercentage(0.1))
      assertThat(type).isEqualTo(transaction.transactionType.type)
      assertThat(postingType).isEqualTo(transaction.postingType.name)
      assertThat(entryDate).isEqualTo(transaction.entryDate)
      assertThat(reference).isEqualTo(transaction.clientUniqueRef)
      assertThat(subAccountType).isEqualTo(transaction.subAccountType.name)
      assertThat(holdBalance).isCloseTo(transaction.trustAccount.holdBalance, Percentage.withPercentage(0.1))
      assertThat(currentBalance).isCloseTo(transaction.trustAccount.currentBalance, Percentage.withPercentage(0.1))
    }
  }
}
