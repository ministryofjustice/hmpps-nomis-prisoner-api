package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class StaffUserAccountDslMarker

@NomisDataDslMarker
interface StaffUserAccountDsl {

  @UserCaseloadDslMarker
  fun userCaseload(
    caseloadId: String,
    startDate: LocalDate = LocalDate.now(),
    dsl: UserCaseloadDsl.() -> Unit = {},
  ): UserCaseload
}

@Component
class StaffUserAccountBuilderFactory(
  private val repository: StaffUserAccountBuilderRepository,
  private val userCaseloadBuilderFactory: UserCaseloadBuilderFactory,
) {
  fun builder(): StaffUserAccountBuilder = StaffUserAccountBuilder(repository, userCaseloadBuilderFactory)
}

@Component
class StaffUserAccountBuilderRepository(
  private val staffUserAccountRepository: StaffUserAccountRepository,
) {
  fun save(staffUserAccount: StaffUserAccount): StaffUserAccount = staffUserAccountRepository.save(staffUserAccount)
}

class StaffUserAccountBuilder(
  private val repository: StaffUserAccountBuilderRepository,
  private val userCaseloadBuilderFactory: UserCaseloadBuilderFactory,
) : StaffUserAccountDsl {
  private lateinit var staffUserAccount: StaffUserAccount

  fun build(
    username: String,
    staff: Staff,
    type: String,
    activeCaseloadId: String?,
    lastLoggedIn: LocalDateTime?,
  ): StaffUserAccount = StaffUserAccount(
    username = username,
    staff = staff,
    type = type,
    activeCaseloadId = activeCaseloadId,
    source = "USER",
    lastLoggedIn = lastLoggedIn,
  )
    .let { repository.save(it) }
    .also { staffUserAccount = it }

  override fun userCaseload(
    caseloadId: String,
    startDate: LocalDate,
    dsl: UserCaseloadDsl.() -> Unit,
  ): UserCaseload = userCaseloadBuilderFactory.builder().let { builder ->
    builder.build(
      username = staffUserAccount.username,
      caseloadId = caseloadId,
      startDate = startDate,
    )
      .also { staffUserAccount.caseloads += it }
      .also { builder.apply(dsl) }
  }
}
