package uk.gov.justice.digital.hmpps.nomisprisonerapi.courtsentencing

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ImprisonmentStatusService {
  companion object {
    enum class ChangeType {
      UPDATE_SENTENCE,
      UPDATE_RESULT,
      DELETE,
    }
  }

  fun recalculateImprisonmentStatus(offenderNo: String, reason: ChangeType) {
  }
}
