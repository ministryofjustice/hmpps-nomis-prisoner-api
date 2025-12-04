package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person

@Repository
interface PersonRepository :
  CrudRepository<Person, Long>,
  JpaRepository<Person, Long> {

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
