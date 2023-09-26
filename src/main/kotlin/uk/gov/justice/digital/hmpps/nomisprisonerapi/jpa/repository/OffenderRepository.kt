package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender

@Repository
interface OffenderRepository : JpaRepository<Offender, Long> {
  fun findByNomsId(nomsId: String): List<Offender>

  @Query("select o from Offender o left join fetch o.bookings b WHERE o.nomsId = :nomsId order by b.bookingSequence asc")
  fun findByNomsIdOrderedWithBookings(nomsId: String): List<Offender>

  @Query("select o.id from Offender o join o.bookings b WHERE o.nomsId = :nomsId and b.bookingSequence = 1")
  fun findCurrentIdByNomsId(nomsId: String): Long?
}

fun OffenderRepository.findRootByNomisId(nomsId: String): Offender? = findByNomsIdOrderedWithBookings(nomsId).firstOrNull()
