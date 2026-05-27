package uk.gov.justice.digital.hmpps.nomisprisonerapi.staff

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Caseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseloadRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository

@Service
@Transactional
class StaffService(
  private val staffRepository: StaffRepository,
) {
  fun getStaffDetails(userId: Long, dpsRolesOnly: Boolean): StaffDetails = staffRepository.findByIdOrNull(userId)
    ?.toStaffDetails(dpsRolesOnly)
    ?: throw NotFoundException("Staff with id=$userId does not exist")

  fun getStaffIds(
    staffId: Long,
    pageSize: Int,
  ): StaffIdsPage = staffRepository.findAllStaffIds(
    staffId = staffId,
    pageSize = pageSize,
  )
    .map { StaffIdResponse(staffId = it.id) }.let { StaffIdsPage(it) }

  fun Staff.toStaffDetails(dpsRolesOnly: Boolean) = StaffDetails(
    id = id,
    firstName = firstName,
    lastName = lastName,
    email = emails.firstOrNull()?.internetAddress,
    status = status.code,
    accounts = accounts.map { it.toUserAccount(dpsRolesOnly) },
    audit = toAudit(),
  )

  fun StaffUserAccount.toUserAccount(dpsRolesOnly: Boolean) = StaffAccount(
    username = username,
    typeCode = type.code,
    status = accountDetail?.status ?: "",
    sourceCode = source.code,
    activeCaseloadId = activeCaseloadId,
    lastLoggedIn = lastLoggedIn,
    caseloads = userCaseloads.sortedBy { it.id.caseloadId }.map { it.toResponse(dpsRolesOnly) },
    audit = toAudit(),
  )

  fun UserCaseload.toResponse(dpsRolesOnly: Boolean): CaseloadResponse = CaseloadResponse(
    caseload = id.caseloadId,
    roles = if (!dpsRolesOnly || id.caseloadId == Caseload.DPS_CASELOAD) {
      userCaseloadRoles.map { it.toResponse() }
    } else {
      emptyList()
    },
    audit = toAudit(),
  )

  fun UserCaseloadRole.toResponse() = RoleResponse(
    code = role.code,
    name = role.name,
    audit = toAudit(),
  )
}
