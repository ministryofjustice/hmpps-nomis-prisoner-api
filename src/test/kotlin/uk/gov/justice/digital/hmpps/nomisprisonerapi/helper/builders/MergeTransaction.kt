package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MergeTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.MergeTransactionRepository
import java.time.LocalDateTime

@DslMarker
annotation class MergeTransactionDslMarker

@NomisDataDslMarker
interface MergeTransactionDsl

@Component
class MergeTransactionBuilderRepository(
  private val mergeTransactionRepository: MergeTransactionRepository,
) {
  fun save(mergeTransaction: MergeTransaction): MergeTransaction = mergeTransactionRepository.save(mergeTransaction)
}

@Component
class MergeTransactionBuilderFactory(private val repository: MergeTransactionBuilderRepository) {
  fun builder() = MergeTransactionBuilder(repository)
}

class MergeTransactionBuilder(private val repository: MergeTransactionBuilderRepository) :
  MergeTransactionDsl {
  fun build(
    requestDate: LocalDateTime,
    requestStatusCode: String,
    transactionSource: String,
    offenderBookId1: Long,
    rootOffenderId1: Long,
    offenderId1: Long,
    nomsId1: String,
    lastName1: String,
    firstName1: String,
    offenderBookId2: Long,
    rootOffenderId2: Long,
    offenderId2: Long,
    nomsId2: String,
    lastName2: String,
    firstName2: String,
  ): MergeTransaction =
    MergeTransaction(
      requestDate = requestDate,
      requestStatusCode = requestStatusCode,
      transactionSource = transactionSource,
      offenderBookId1 = offenderBookId1,
      rootOffenderId1 = rootOffenderId1,
      offenderId1 = offenderId1,
      nomsId1 = nomsId1,
      lastName1 = lastName1,
      firstName1 = firstName1,
      offenderBookId2 = offenderBookId2,
      rootOffenderId2 = rootOffenderId2,
      offenderId2 = offenderId2,
      nomsId2 = nomsId2,
      lastName2 = lastName2,
      firstName2 = firstName2,
    )
      .let { repository.save(it) }
}
