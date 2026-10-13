package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserGroupMember
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserGroupMemberId
import java.time.LocalDate

@DslMarker
annotation class MemberCaseloadDslMarker

@NomisDataDslMarker
interface MemberCaseloadDsl

@Component
class MemberCaseloadBuilderFactory {
  fun builder() = MemberCaseloadBuilder()
}

class MemberCaseloadBuilder : MemberCaseloadDsl {
  fun build(
    username: String,
    caseloadId: String,
    startDate: LocalDate,
    expiryDate: LocalDate?,
  ): UserGroupMember = UserGroupMember(
    id = UserGroupMemberId(username = username, caseloadId = caseloadId),
    startDate = startDate,
    expiryDate = expiryDate,
  )
}
