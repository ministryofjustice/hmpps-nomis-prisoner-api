package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderFixedTermRecall

@Repository
interface OffenderFixedTermRecallRepository : JpaRepository<OffenderFixedTermRecall, Long> 
