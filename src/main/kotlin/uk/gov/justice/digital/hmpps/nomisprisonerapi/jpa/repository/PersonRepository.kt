package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import java.time.LocalDateTime

@Repository
interface PersonRepository :
  CrudRepository<Person, Long>,
  JpaRepository<Person, Long> {
  @Query(
    """
      select 
        p.id as personId
      from Person p 
    """,
  )
  fun findAllPersonIds(
    pageable: Pageable,
  ): Page<PersonIdProjection>

  @Query(
    """
      select 
        p.id as personId
      from Person p 
        where 
          (:fromDate is null or p.createDatetime >= :fromDate) and 
          (:toDate is null or p.createDatetime <= :toDate) 
    """,
  )
  fun findAllPersonIds(
    fromDate: LocalDateTime?,
    toDate: LocalDateTime?,
    pageable: Pageable,
  ): Page<PersonIdProjection>

  @Query(
    """
      select 
       PERSON_ID 
      from PERSONS
      where  PERSON_ID > :personId
        and rownum <= :pageSize
      order by PERSON_ID
    """,
    nativeQuery = true,
  )
  fun findAllIdsFromId(personId: Long, pageSize: Int): List<Long>
}

interface PersonIdProjection {
  val personId: Long
}
