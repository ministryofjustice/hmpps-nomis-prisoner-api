package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Role
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RoleCaseloadType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserAccountType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.RoleRepository

@DslMarker
annotation class RoleDslMarker

@NomisDataDslMarker
interface RoleDsl

@Component
class RoleBuilderFactory(
  private val repository: RoleBuilderRepository,
) {
  fun builder(): RoleBuilder = RoleBuilder(repository)
}

@Component
class RoleBuilderRepository(
  private val repository: RoleRepository,
  private val roleUserTypeRepository: ReferenceCodeRepository<UserAccountType>,
  private val roleCaseloadTypeRepository: ReferenceCodeRepository<RoleCaseloadType>,
) {
  fun lookupUserAccountType(code: String) = roleUserTypeRepository.findByIdOrNull(UserAccountType.pk(code))!!
  fun lookupRoleCaseloadType(code: String) = roleCaseloadTypeRepository.findByIdOrNull(RoleCaseloadType.pk(code))!!
  fun save(role: Role): Role = repository.save(role)
}

class RoleBuilder(
  private val repository: RoleBuilderRepository,
) : RoleDsl {
  fun build(
    code: String,
    name: String,
    userAccountType: String,
    type: String,
  ): Role = Role(
    code = code,
    name = name,
    userAccountType = repository.lookupUserAccountType(userAccountType),
    type = repository.lookupRoleCaseloadType(type),
  )
    .let { repository.save(it) }
}
