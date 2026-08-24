package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

@Repository
interface StaffRepository : JpaRepository<Staff, Long> {

  fun findByAccountsUsername(username: String): Staff?

  @Query(
    """
        select STAFF_ID from (
          select STAFF_ID, rownum as seqnum from (
            select STAFF_ID
            from STAFF_MEMBERS
            where 
              (:active = false or status = 'ACTIVE')
            order by STAFF_ID
          )
        ) where mod(seqnum, :pageSize) = 0
    """,
    nativeQuery = true,
  )
  fun findEveryPageSizeStaffId(pageSize: Int, active: Boolean): List<Long>

  @Query(
    """
    select distinct STAFF_ID
     from STAFF_MEMBERS
    where 
        STAFF_ID > :fromStaffId and STAFF_ID <= :toStaffId and 
        (:active = false or status = 'ACTIVE')
  """,
    nativeQuery = true,
  )
  fun findStaffIdsBetweenIds(fromStaffId: Long, toStaffId: Long, active: Boolean): List<Long>

  @Query(
    """
      select *
      from (select s.STAFF_ID as id
            from STAFF_MEMBERS s
            where 
              s.STAFF_ID > :staffId
            order by s.STAFF_ID)
      where rownum <= :pageSize
    """,
    nativeQuery = true,
  )
  fun getStaffIdsFromId(staffId: Long, pageSize: Int): List<StaffIdProjection>

  @Query(
    """
      select 
        s.id as id
      from Staff s
      order by s.id
    """,
  )
  fun findAllStaffIds(pageable: Pageable): Page<StaffIdProjection>

  interface StaffIdProjection {
    val id: Long
  }
}
