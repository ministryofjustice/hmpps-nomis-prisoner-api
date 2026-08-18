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

  @Query(
    """
        select PROPERTY_CONTAINER_ID from (
          select PROPERTY_CONTAINER_ID, rownum as seqnum from (
            select PROPERTY_CONTAINER_ID
            from OFFENDER_PPTY_CONTAINERS
            order by PROPERTY_CONTAINER_ID
          )
        ) where mod(seqnum, :pageSize) = 0
    """,
    nativeQuery = true,
  )
  fun findEveryPageSizeId(pageSize: Int): List<Long>

  @Query(
    """
     select opc.propertyContainerId
        from OffenderPropertyContainer opc
        where opc.propertyContainerId > :fromId and opc.propertyContainerId <= :toId
        order by opc.propertyContainerId
  """,
  )
  fun findAllIdsBetweenIds(fromId: Long, toId: Long): List<Long>
}

interface ProjectId {
  val propertyContainerId: Long
}
