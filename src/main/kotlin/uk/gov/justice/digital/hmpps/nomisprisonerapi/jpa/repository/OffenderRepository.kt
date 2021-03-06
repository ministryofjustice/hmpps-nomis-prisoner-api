package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.time.LocalDate
import java.util.Optional

@Repository
interface OffenderRepository : JpaRepository<Offender, Long> {
  fun findByNomsId(nomsId: String): List<Offender>
  fun findByLastNameAndFirstNameAndBirthDate(lastName: String, firstName: String, dob: LocalDate): List<Offender>

  @Query("select o from Offender o left join fetch o.bookings b WHERE o.nomsId = :nomsId order by b.bookingSequence asc")
  fun findOffendersByNomsId(@Param("nomsId") nomsId: String, pageable: Pageable): List<Offender>
  fun findOffenderByNomsId(nomsId: String): Optional<Offender> {
    return findOffendersByNomsId(nomsId, PageRequest.of(0, 1)).stream().findFirst()
  }
}
