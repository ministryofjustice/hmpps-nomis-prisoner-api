package uk.gov.justice.digital.hmpps.nomisprisonerapi.roles

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Role
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RoleCaseloadType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.RoleRepository

@Service
@Transactional
class RolesService(
  val roleRepository: RoleRepository,
) {
  fun getAllRoles(): List<RoleDetail> = roleRepository.findAll().map { it.toRoleDetail() }
  fun getAllDpsRoles(): List<RoleDetail> = roleRepository.findByType(RoleCaseloadType.APP).map { it.toRoleDetail() }
  fun findRoleByCode(code: String): RoleDetail = roleRepository.findByCode(code)?.toRoleDetail()
    ?: throw NotFoundException("Role with code $code not found")
}

fun Role.toRoleDetail() = RoleDetail(
  code = code,
  name = name,
  adminRoleOnly = false,
)
