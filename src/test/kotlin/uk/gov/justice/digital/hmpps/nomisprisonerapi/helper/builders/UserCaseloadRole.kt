package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Role
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseloadRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseloadRoleId

@DslMarker
annotation class UserCaseloadRoleDslMarker

@NomisDataDslMarker
interface UserCaseloadRoleDsl

@Component
class UserCaseloadRoleBuilderFactory {
  fun builder() = UserCaseloadRoleBuilder()
}

class UserCaseloadRoleBuilder : UserCaseloadRoleDsl {

  fun build(userCaseload: UserCaseload, role: Role): UserCaseloadRole = UserCaseloadRole(
    id = UserCaseloadRoleId(username = userCaseload.id.username, roleId = role.id, caseloadId = userCaseload.id.caseloadId),
    role = role,
    userCaseload = userCaseload,
  )
}
