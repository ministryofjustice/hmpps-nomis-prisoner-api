package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadDeductionProfile
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CaseloadDeductionProfileId

@Repository
interface CaseloadDeductionProfileRepository : JpaRepository<CaseloadDeductionProfile, CaseloadDeductionProfileId>
