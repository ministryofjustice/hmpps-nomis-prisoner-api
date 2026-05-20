package uk.gov.justice.digital.hmpps.nomisprisonerapi.users

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository

@Service
@Transactional
class UserService(
  private val staffRepository: StaffRepository,
) {
  fun getUserDetails(userId: Long): UserDetails = staffRepository.findByIdOrNull(userId)
    ?.toUserDetails()
    ?: throw NotFoundException("User with id=$userId does not exist")
}

fun Staff.toUserDetails(): UserDetails = UserDetails(
  id = id,
  firstName = firstName,
  lastName = lastName,
  email = emails.firstOrNull()?.internetAddress,
  status = status.code,
  accounts = accounts.map { it.toUserAccount() },
  audit = toAudit(),
)

fun StaffUserAccount.toUserAccount() = UserAccount(
  username = username,
  typeCode = type.code,
  status = accountDetail?.status ?: "",
  sourceCode = source.code,
  activeCaseloadId = activeCaseloadId,
  lastLoggedIn = lastLoggedIn,
  caseloads = caseloads.map { it.id.caseloadId },
  roles = caseloads.flatMap(UserCaseload::roles).map { it.role.code },
  audit = toAudit(),
)
