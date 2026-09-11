package uk.gov.justice.digital.hmpps.nomisprisonerapi.drugtesting

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.RandomTestingProgramRepository

@Service
@Transactional(readOnly = true)
class DrugTestingService(
  private val randomTestingProgramRepository: RandomTestingProgramRepository,
) {
  fun findIdRanges(pageSize: Int, filter: DrugTestingFilter): List<Long> = randomTestingProgramRepository.findEveryPageSizeId(
    pageSize,
    filter.includedPrisonIdsAsList,
    filter.excludedPrisonIdsAsList,
  )

  fun findAllIdsBetweenIds(fromId: Long, toId: Long, filter: DrugTestingFilter): List<Long> = randomTestingProgramRepository
    .findAllIdsBetweenIds(
      fromId,
      toId,
      filter.includedPrisonIdsAsList,
      filter.excludedPrisonIdsAsList,
    )

  fun findRandomTestingProgramWithPrisoners(rtpId: Long) = randomTestingProgramRepository.findRandomTestingProgramWithOffenders(rtpId) ?: throw NotFoundException("Random testing program with id $rtpId not found")
}
