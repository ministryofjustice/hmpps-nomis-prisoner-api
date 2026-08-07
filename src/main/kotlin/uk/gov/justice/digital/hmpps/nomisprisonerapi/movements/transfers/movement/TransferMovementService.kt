package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.movement

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class TransferMovementService {

  fun getTransferMovementOut(offenderNo: String, bookingId: Long, sequence: Int): TransferMovementOut = TODO("Not yet implemented")
}
