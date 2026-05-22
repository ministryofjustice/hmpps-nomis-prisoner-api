package uk.gov.justice.digital.hmpps.nomisprisonerapi.users

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Caseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.UserCaseloadRoleRepository

@Service
@Transactional
class UserService(
  private val staffRepository: StaffRepository,
  private val userCaseloadRoleRepository: UserCaseloadRoleRepository,
) {
  fun getUserDetails(userId: Long, dpsRolesOnly: Boolean): UserDetails = staffRepository.findByIdOrNull(userId)
    ?.toUserDetails(dpsRolesOnly)
    ?: throw NotFoundException("Staff User with id=$userId does not exist")

  fun Staff.toUserDetails(dpsRolesOnly: Boolean): UserDetails = UserDetails(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = emails.firstOrNull()?.internetAddress,
    status = status.code,
    accounts = accounts.map { it.toUserAccount(dpsRolesOnly) },
    audit = toAudit(),
  )

  fun StaffUserAccount.toUserAccount(dpsRolesOnly: Boolean): UserAccount = UserAccount(
    username = username,
    typeCode = type.code,
    status = accountDetail?.status ?: "",
    sourceCode = source.code,
    activeCaseloadId = activeCaseloadId,
    lastLoggedIn = lastLoggedIn,
    caseloads = caseloads.map { it.id.caseloadId }.sorted(),
    roles = userCaseloadRoleRepository.findAllRoleCodes(username, if (dpsRolesOnly) Caseload.DPS_CASELOAD else null),
    audit = toAudit(),
  )
}
