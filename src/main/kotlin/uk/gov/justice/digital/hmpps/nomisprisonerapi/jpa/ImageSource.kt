package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity

@Entity
@DiscriminatorValue(ImageSource.IMAGE_SOURCE_CODE)
class ImageSource(code: String, description: String) : ReferenceCode(IMAGE_SOURCE_CODE, code, description) {

  companion object {
    const val IMAGE_SOURCE_CODE = "IMAGE_SOURCE"
    fun pk(code: String): Pk = Pk(IMAGE_SOURCE_CODE, code)
  }
}
