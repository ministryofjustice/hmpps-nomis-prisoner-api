package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Caseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseloadId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadRepository
import java.time.LocalDate

@DslMarker
annotation class UserCaseloadDslMarker

@NomisDataDslMarker
interface UserCaseloadDsl

@Component
class UserCaseloadBuilderFactory(private val userCaseloadRepository: UserCaseloadRepository) {
  fun builder() = UserCaseloadBuilder(userCaseloadRepository)
}

@Component
class UserCaseloadRepository(
  private val caseloadRepository: CaseloadRepository,
) {
  fun caseloadOf(code: String): Caseload = caseloadRepository.findById(code).orElseThrow()
}

class UserCaseloadBuilder(
  private val repository: UserCaseloadRepository,
) : UserCaseloadDsl {

  fun build(
    username: String,
    caseloadId: String,
    startDate: LocalDate,
  ): UserCaseload = UserCaseload(
    id = UserCaseloadId(username = username, caseloadId = caseloadId),
    caseload = repository.caseloadOf(caseloadId),
    startDate = startDate,
  )
}
