package uk.gov.justice.digital.hmpps.nomisprisonerapi.staff

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
  fun getStaffDetails(staffId: Long, dpsRolesOnly: Boolean): StaffDetails = staffRepository.findByIdOrNull(staffId)
    ?.toStaffDetails(dpsRolesOnly)
    ?: throw NotFoundException("Staff with id=$staffId does not exist")

  fun getStaffDetails(username: String, dpsRolesOnly: Boolean): StaffDetails = staffRepository.findByAccountsUsername(username)
    ?.toStaffDetails(dpsRolesOnly)
    ?: throw NotFoundException("Staff with username=$username does not exist")

  fun getStaffIdsFromId(
    staffId: Long,
    pageSize: Int,
  ): StaffIdsPage = staffRepository.getStaffIdsFromId(
    staffId = staffId,
    pageSize = pageSize,
  )
    .map { StaffIdResponse(staffId = it.id) }.let { StaffIdsPage(it) }

  fun getStaffIds(
    pageRequest: Pageable,
  ): Page<StaffIdResponse> = staffRepository.findAllStaffIds(pageRequest).map {
    StaffIdResponse(staffId = it.id)
  }

  fun Staff.toStaffDetails(dpsRolesOnly: Boolean) = StaffDetails(
    id = id,
    firstName = firstName,
    lastName = lastName,
    emails = emails.map { it.internetAddress },
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
    caseloadId = id.caseloadId,
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
