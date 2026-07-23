package uk.gov.justice.digital.hmpps.nomisprisonerapi.courtsentencing

import org.springframework.stereotype.Service

@Service
class ImprisonmentStatusService {
  companion object {
    enum class ChangeType {
      UPDATE_SENTENCE,
      UPDATE_RESULT,
      DELETE,
    }
  }

  @Suppress("unused")
  fun recalculateImprisonmentStatus(offenderNo: String, reason: ChangeType) {
  }
}
