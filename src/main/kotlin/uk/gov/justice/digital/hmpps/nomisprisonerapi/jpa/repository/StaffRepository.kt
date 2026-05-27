package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

@Repository
interface StaffRepository : JpaRepository<Staff, Long> {

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
  fun findAllStaffIds(staffId: Long, pageSize: Int): List<StaffIdProjection>

  interface StaffIdProjection {
    val id: Long
  }
}
