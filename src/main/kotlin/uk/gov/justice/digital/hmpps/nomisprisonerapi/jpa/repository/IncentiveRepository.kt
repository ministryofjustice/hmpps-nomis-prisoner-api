package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncentiveId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDate

@Repository
interface IncentiveRepository : CrudRepository<Incentive, IncentiveId>, JpaSpecificationExecutor<Incentive> {
  // current IEP - determined first by IEP date, secondly by IEP sequence - if more than 1 on the same date
  fun findFirstById_offenderBookingOrderByIepDateDescId_SequenceDesc(
    offenderBooking: OffenderBooking,
  ): Incentive?

  fun findAllById_offenderBookingAndIepDateOrderById_SequenceAsc(offenderBooking: OffenderBooking, iepDate: LocalDate): List<Incentive>

  @Modifying
  @Query("update Incentive i set i.id.sequence = :newSequence where i.id.offenderBooking = :offenderBooking and i.id.sequence = :oldSequence")
  fun updateSequence(offenderBooking: OffenderBooking, oldSequence: Long, newSequence: Long)
}
