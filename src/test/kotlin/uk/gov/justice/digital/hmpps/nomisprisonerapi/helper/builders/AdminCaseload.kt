package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserGroupAdministratorId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserGroupCaseloadAdministrator
import java.time.LocalDate

@DslMarker
annotation class AdminCaseloadDslMarker

@NomisDataDslMarker
interface AdminCaseloadDsl

@Component
class AdminCaseloadBuilderFactory {
  fun builder() = AdminCaseloadBuilder()
}

class AdminCaseloadBuilder : AdminCaseloadDsl {
  fun build(
    username: String,
    caseloadId: String,
    expiryDate: LocalDate?,
  ): UserGroupCaseloadAdministrator = UserGroupCaseloadAdministrator(
    id = UserGroupAdministratorId(username = username, caseloadId = caseloadId),
    expiryDate = expiryDate,
  )
}
