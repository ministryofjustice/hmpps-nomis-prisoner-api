package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RandomTestingProgram

@Repository
interface RandomTestingProgramRepository : JpaRepository<RandomTestingProgram, Long> {
  @Query(
    """
        select RTP_ID from (
          select RTP_ID, rownum as seqnum from (
            select RTP_ID
            from RANDOM_TESTING_PROGRAMS
            order by RTP_ID
          )
        ) where mod(seqnum, :pageSize) = 0
    """,
    nativeQuery = true,
  )
  fun findEveryPageSizeId(pageSize: Int): List<Long>

  @Query(
    """
     select rtp.id
        from RandomTestingProgram rtp
        where rtp.id > :fromId and rtp.id <= :toId
        order by rtp.id
  """,
  )
  fun findAllIdsBetweenIds(fromId: Long, toId: Long): List<Long>

  @Query("select rtp from RandomTestingProgram rtp left join fetch rtp.offenderTestSelection where rtp.id = :rtpId")
  fun findRandomTestingProgramWithOffenders(rtpId: Long): RandomTestingProgram
}
