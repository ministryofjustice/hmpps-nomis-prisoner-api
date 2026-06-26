package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPropertyContainer

@Repository
interface OffenderPropertyContainerRepository : JpaRepository<OffenderPropertyContainer, Long> {
  @Query("from OffenderPropertyContainer")
  fun findIds(pageRequest: Pageable): Page<ProjectId>

  @Suppress("ktlint:standard:function-naming")
  fun findIdsByAgencyLocation_IdIn(pageRequest: Pageable, prisonIds: List<String>): Page<ProjectId>
}

interface ProjectId {
  val propertyContainerId: Long
}
