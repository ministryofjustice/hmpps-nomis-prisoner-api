package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class TransferScheduleService {
  fun getTransferScheduleOut(offenderNo: String, eventId: Long): TransferScheduleOut {
    TODO("To be implemented")
  }
}
