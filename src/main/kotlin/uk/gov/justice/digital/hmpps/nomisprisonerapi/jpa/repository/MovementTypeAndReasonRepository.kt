
package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementTypeAndReason
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementTypeAndReasonId

@Repository
interface MovementTypeAndReasonRepository : JpaRepository<MovementTypeAndReason, MovementTypeAndReasonId>
