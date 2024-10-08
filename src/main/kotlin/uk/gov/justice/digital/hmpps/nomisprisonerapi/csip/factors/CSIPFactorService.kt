package uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.factors

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.CSIPFactorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.csip.toFactorResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CSIPFactorRepository

@Service
@Transactional
class CSIPFactorService(
  private val csipFactorRepository: CSIPFactorRepository,
) {
  fun getCSIPFactor(csipFactorId: Long): CSIPFactorResponse? {
    val csipFactor = csipFactorRepository.findByIdOrNull(csipFactorId)
      ?: throw NotFoundException("CSIP Factor with id=$csipFactorId does not exist")

    return csipFactor.toFactorResponse()
  }

  fun deleteCSIPFactor(csipFactorId: Long) {
    csipFactorRepository.findByIdOrNull(csipFactorId)?.also {
      csipFactorRepository.deleteById(csipFactorId)
    }
  }
}
