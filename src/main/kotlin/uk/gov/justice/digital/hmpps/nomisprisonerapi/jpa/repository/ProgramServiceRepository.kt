package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ProgramService

@Repository
interface ProgramServiceRepository : CrudRepository<ProgramService, Long>, JpaSpecificationExecutor<ProgramService> {
  fun findByProgramCode(programCode: String): ProgramService?
}
