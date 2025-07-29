package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderExternalMovementId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTemporaryAbsenceReturn

@Repository
interface OffenderTemporaryAbsenceReturnRepository : JpaRepository<OffenderTemporaryAbsenceReturn, OffenderExternalMovementId> {
  @Suppress("ktlint:standard:function-naming")
  fun findAllByOffenderBooking_Offender_NomsIdAndScheduledTemporaryAbsenceIsNull(offenderNo: String): List<OffenderTemporaryAbsenceReturn>
}
