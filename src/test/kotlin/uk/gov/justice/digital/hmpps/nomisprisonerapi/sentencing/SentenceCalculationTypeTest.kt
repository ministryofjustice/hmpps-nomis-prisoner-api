package uk.gov.justice.digital.hmpps.nomisprisonerapi.sentencing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationTypeId

class SentenceCalculationTypeTest {

  @Suppress("unused")
  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class IsRecallSentence {

    private fun someNonRecallNomisSentenceCalcTypes(): Set<String> = setOf(
      "SEC91_03_ORA",
      "ADIMP",
      "ADIMP_ORA",
      "SEC250",
      "SEC250_ORA",
    )

    // list taken from DPS Remand & Recall hardcoded list
    private fun recallNomisSentenceCalcTypes(): Set<String> = setOf(
      "CUR",
      "CUR_ORA",
      "HDR",
      "HDR_ORA",
      "FTR",
      "FTR_ORA",
      "14FTR_ORA",
      "FTRSCH18",
      "FTRSCH18_ORA",
      "FTR_SCH15",
      "FTRSCH15_ORA",
      "FTR_HDC",
      "FTR_HDC_ORA",
      "14FTRHDC_ORA",
      "LR",
      "LR_ORA",
      "LR_DPP",
      "LR_DLP",
      "LR_ALP",
      "LR_ALP_LASPO",
      "LR_ALP_CDE18",
      "LR_ALP_CDE21",
      "LR_LIFE",
      "LR_EPP",
      "LR_IPP",
      "LR_MLP",
      "LR_SEC236A",
      "LR_SEC91_ORA",
      "LRSEC250_ORA",
      "LR_ES",
      "LR_EDS18",
      "LR_EDS21",
      "LR_EDSU18",
      "LR_LASPO_AR",
      "LR_LASPO_DR",
      "LR_SOPC18",
      "LR_SOPC21",
      "LR_YOI_ORA",
    )

    @ParameterizedTest
    @MethodSource("recallNomisSentenceCalcTypes")
    fun `return true for recall sentences`(calculationType: String) {
      assertThat(sentenceCalculationType(calculationType).isRecallSentence()).isTrue()
    }

    @ParameterizedTest
    @MethodSource("someNonRecallNomisSentenceCalcTypes")
    fun `return false for normal sentences`(calculationType: String) {
      assertThat(sentenceCalculationType(calculationType).isRecallSentence()).isFalse()
    }
  }

  fun sentenceCalculationType(calculationType: String) = SentenceCalculationType(
    id = SentenceCalculationTypeId(calculationType = calculationType, category = "2003"),
    description = "whatever description",
    sentenceType = "whatever",
    active = true,
  )
}
