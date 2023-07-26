package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentCharge
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncidentChargeId

@Repository
interface AdjudicationIncidentChargeRepository :
  JpaRepository<AdjudicationIncidentCharge, AdjudicationIncidentChargeId>,
  JpaSpecificationExecutor<AdjudicationIncidentCharge>
