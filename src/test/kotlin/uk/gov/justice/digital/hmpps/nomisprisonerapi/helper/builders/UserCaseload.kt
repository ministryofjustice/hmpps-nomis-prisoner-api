package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Caseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Role
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseloadId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseloadRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CaseloadRepository
import java.time.LocalDate
import kotlin.collections.plus

@DslMarker
annotation class UserCaseloadDslMarker

@NomisDataDslMarker
interface UserCaseloadDsl {
  @UserCaseloadRoleDslMarker
  fun userCaseloadRole(
    role: Role,
    dsl: UserCaseloadRoleDsl.() -> Unit = {},
  ): UserCaseloadRole
}

@Component
class UserCaseloadBuilderFactory(
  private val userCaseloadRepository: UserCaseloadRepository,
  private val userCaseloadRoleBuilderFactory: UserCaseloadRoleBuilderFactory,
) {
  fun builder() = UserCaseloadBuilder(userCaseloadRepository, userCaseloadRoleBuilderFactory)
}

@Component
class UserCaseloadRepository(
  private val caseloadRepository: CaseloadRepository,
) {
  fun caseloadOf(code: String): Caseload = caseloadRepository.findById(code).orElseThrow()
}

class UserCaseloadBuilder(
  private val repository: UserCaseloadRepository,
  private val userCaseloadRoleBuilderFactory: UserCaseloadRoleBuilderFactory,
) : UserCaseloadDsl {
  private lateinit var userCaseload: UserCaseload

  fun build(
    username: String,
    caseloadId: String,
    startDate: LocalDate,
  ): UserCaseload = UserCaseload(
    id = UserCaseloadId(username = username, caseloadId = caseloadId),
    caseload = repository.caseloadOf(caseloadId),
    startDate = startDate,
  )
    .also { userCaseload = it }

  override fun userCaseloadRole(
    role: Role,
    dsl: UserCaseloadRoleDsl.() -> Unit,
  ): UserCaseloadRole = userCaseloadRoleBuilderFactory.builder().let { builder ->
    builder.build(
      userCaseload = userCaseload,
      role = role,
    )
      .also { userCaseload.roles += it }
      .also { builder.apply(dsl) }
  }
}
