package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.InternalLocationUsage

@Repository
interface InternalLocationUsageRepository : CrudRepository<InternalLocationUsage, Long> {
  @Suppress("FunctionName")
  fun findOneByAgency_IdAndInternalLocationUsage(
    agencyInternalLocationId: String,
    internalLocationUsageCode: String,
  ): InternalLocationUsage?
}
