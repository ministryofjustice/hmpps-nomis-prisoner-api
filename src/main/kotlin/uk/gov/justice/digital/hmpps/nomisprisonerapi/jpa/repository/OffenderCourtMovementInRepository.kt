@file:Suppress("ktlint:standard:function-naming")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId

@Repository
interface OffenderCourtMovementInRepository : JpaRepository<OffenderCourtMovementIn, OffenderExternalMovementId> {
  fun findAllByOffenderBooking_Offender_NomsId(offenderNo: String): List<OffenderCourtMovementIn>
}
