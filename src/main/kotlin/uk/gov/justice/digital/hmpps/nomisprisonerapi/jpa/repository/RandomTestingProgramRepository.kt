package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RandomTestingProgram
import java.time.LocalDate

@Repository
interface RandomTestingProgramRepository : JpaRepository<RandomTestingProgram, Long> {
  @Query(
    """
        select RTP_ID from (
          select RTP_ID, rownum as seqnum from (
            select RTP_ID
            from RANDOM_TESTING_PROGRAMS
            where (:includedPrisonIds is null or CASELOAD_ID in (:includedPrisonIds))
            and CASELOAD_ID not in (:excludedPrisonIds)
            order by RTP_ID
          )
        ) where mod(seqnum, :pageSize) = 0
    """,
    nativeQuery = true,
  )
  fun findEveryPageSizeId(pageSize: Int, includedPrisonIds: List<String>?, excludedPrisonIds: List<String>): List<Long>

  @Query(
    """
     select rtp.id
        from RandomTestingProgram rtp
        where rtp.id > :fromId and rtp.id <= :toId
        and (:includedPrisonIds is null or rtp.caseloadId in :includedPrisonIds)
        and rtp.caseloadId not in :excludedPrisonIds
        order by rtp.id
  """,
  )
  fun findAllIdsBetweenIds(fromId: Long, toId: Long, includedPrisonIds: List<String>?, excludedPrisonIds: List<String>): List<Long>

  @Query(
    """
      select new uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.RandomTestingProgramDto(
        rtp.id,
        rtp.caseloadId,
        rtp.rtpDate,
        rtp.mainPercentage,
        rtp.reservePercentage,
        rtp.selectionsCount,
        rtp.eligibleCount,
        rtp.reserveCount,
        ob.bookingId,
        o.nomsId,
        otp.testSelectionType,
        otp.testSelectionNo,
        otp.testedFlag,
        otp.reasonNotTested,
        otp.notes
      )
      from RandomTestingProgram rtp
      left join rtp.offenderTestSelection otp
      left join otp.id.offenderBooking ob
      left join ob.offender o
      where rtp.id = :rtpId
    """,
  )
  fun findRandomTestingProgramWithOffenders(rtpId: Long): List<RandomTestingProgramDto>
}

data class RandomTestingProgramDto(
  val rtpId: Long,
  val caseloadId: String,
  val rtpDate: LocalDate,
  val mainPercentage: Short,
  val reservePercentage: Short,
  val selectionsCount: Int?,
  val eligibleCount: Int?,
  val reserveCount: Int?,
  val offenderBookId: Long?,
  val prisonNumber: String?,
  val testSelectionType: String?,
  val testSelectionNo: Int?,
  val testedFlag: String?,
  val reasonNotTested: String?,
  val notes: String?,
)
