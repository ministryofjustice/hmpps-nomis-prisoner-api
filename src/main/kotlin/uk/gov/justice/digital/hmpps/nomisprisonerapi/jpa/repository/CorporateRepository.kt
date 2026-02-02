package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import java.time.LocalDateTime

@Repository
interface CorporateRepository : JpaRepository<Corporate, Long> {
  @Query(
    """
      select 
        p.id as corporateId
      from Corporate p 
    """,
  )
  fun findAllCorporateIds(
    pageable: Pageable,
  ): Page<CorporateIdProjection>

  @Query(
    """
      select 
        p.id as corporateId
      from Corporate p 
        where 
          (:fromDate is null or p.createDatetime >= :fromDate) and 
          (:toDate is null or p.createDatetime <= :toDate) 
    """,
  )
  fun findAllCorporateIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<CorporateIdProjection>

  fun findAllByCorporateName(name: String): List<Corporate>
}

interface CorporateIdProjection {
  val corporateId: Long
}
