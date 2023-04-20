package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitAllowanceLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitAllowanceLevelId

@Repository
interface VisitAllowanceLevelRepository : CrudRepository<VisitAllowanceLevel, VisitAllowanceLevelId>
