package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseloadRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseloadRoleId

@Repository
interface UserCaseloadRoleRepository : CrudRepository<UserCaseloadRole, UserCaseloadRoleId> {
  @Query(
    """
    select distinct r.role.code
    from StaffUserAccount sua
    join sua.caseloads uc
    join uc.roles r
    where sua.username = :username
    and (:caseloadId is null or uc.caseload.id = :caseloadId)
    order by r.role.code
""",
  )
  fun findAllRoleCodes(username: String, caseloadId: String?): List<String>
}
