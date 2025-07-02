package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "SENTENCE_ADJUSTMENTS")
data class SentenceAdjustment(
  @Id
  @Column(name = "SENTENCE_ADJUST_CODE", nullable = false)
  val id: String,
  @Column(name = "DESCRIPTION", nullable = false)
  val description: String,
  @Column(name = "USAGE_CODE", nullable = false)
  val usage: String?,
) {
  companion object {
    const val REMAND_CODE = "RX"
    const val RECALL_REMAND_CODE = "RSR"
    const val TAGGED_BAIL_CODE = "S240A"
    const val RECALL_TAGGED_BAIL_CODE = "RST"
  }
  fun isSentenceRelated() = usage in listOf("SENT", "DPS_ONLY")
  fun isBookingRelated() = usage in listOf("BKG", "DPS_ONLY")
}
