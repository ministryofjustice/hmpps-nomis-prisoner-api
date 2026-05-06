package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Role
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RoleCaseloadType

@Repository
interface RoleRepository : CrudRepository<Role, Long> {
  fun findByCode(code: String): Role?
  fun findByType(type: RoleCaseloadType): List<Role>
}
