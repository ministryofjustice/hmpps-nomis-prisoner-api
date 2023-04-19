package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PrisonIncentiveLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PrisonIncentiveLevelId

@Repository
interface PrisonIncentiveLevelRepository : CrudRepository<PrisonIncentiveLevel, PrisonIncentiveLevelId>
