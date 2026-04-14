package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.taps

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.EntityManager
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Address
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressUsage
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressUsageId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressUsageType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CorporateAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CorporateRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderAddressRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.CorporateInsertRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.repository.TapAddressInsertRepository

@Service
class TapAddressService(
  private val agencyLocationAddressRepository: AgencyLocationAddressRepository,
  private val corporateAddressRepository: CorporateAddressRepository,
  private val corporateInsertRepository: CorporateInsertRepository,
  private val corporateRepository: CorporateRepository,
  private val offenderAddressRepository: OffenderAddressRepository,
  private val tapAddressInsertRepository: TapAddressInsertRepository,
  private val tapHelpers: TapHelpers,
  private val entityManager: EntityManager,
  addressUsageTypeRepository: ReferenceCodeRepository<AddressUsageType>,
) {

  private val rotlAddressType = addressUsageTypeRepository.findByIdOrNull(AddressUsageType.pk("ROTL"))

  fun getAddressDescription(address: Address?) = address?.addressId?.let {
    when (address.addressOwnerClass) {
      "CORP" -> corporateAddressRepository.findByIdOrNull(it)?.corporate?.corporateName
      "AGY" -> agencyLocationAddressRepository.findByIdOrNull(it)?.agencyLocation?.description
      else -> null
    }
  }

  fun findOrCreateAddress(request: UpsertTapAddress, offender: Offender): Address {
    // If we have an address id then use that
    if (request.id != null) return tapHelpers.addressOrThrow(request.id)

    if (request.addressText == null) throw BadDataException("Address text required to create a new address")

    with(request.copyAndTrim()) {
      return when (isCorporateAddress()) {
        true -> findOrCreateCorporateAddress(name!!, addressText!!, postalCode)
        false -> findOrCreateOffenderAddress(addressText!!, postalCode, offender)
      }
    }
  }

  fun findAddressOrThrow(request: UpsertTapAddress, offender: Offender): Address {
    // If we have an address id then use that (which means the address was created in NOMIS or has already been syncd from DPS)
    if (request.id != null) return tapHelpers.addressOrThrow(request.id)

    if (request.addressText == null) throw BadDataException("Address text required to create a new address")

    with(request.copyAndTrim()) {
      return when (isCorporateAddress()) {
        true -> findCorporateAddress(name!!, addressText!!, postalCode)
        false -> findOffenderAddress(addressText!!, postalCode, offender)
      }
        ?: throw BadDataException("Address not found")
    }
  }

  // Note that this assumes the address was created in DPS... a fair assumption because if the address was created in NOMIS we have the ID so never need to find it
  private fun findOrCreateCorporateAddress(name: String, addressText: String, postalCode: String?): CorporateAddress {
    val (premise, street) = formatAddressText(addressText)
    val corporateName = name.toCorporateName()
    return findCorporateAddress(corporateName, addressText, postalCode)
      ?: let {
        corporateInsertRepository.insertCorporateIfNotExists(corporateName)
        val corporate = corporateRepository.findAllByCorporateName(corporateName).first()
        tapAddressInsertRepository.insertAddressIfNotExists("CORP", corporate.id, premise, street, postalCode)
          .also { entityManager.refresh(corporate) }

        corporateRepository.findById(corporate.id).get()
          .addresses.first { it.matchesDpsAddress(premise, street, postalCode) }
      }
  }

  private fun formatAddressText(addressText: String): Pair<String, String?> {
    val maxPremiseLength = 135

    if (addressText.length <= maxPremiseLength) {
      return addressText.trimDpsAddress() to null
    }

    val split = if (addressText.substring(maxPremiseLength, maxPremiseLength + 1) == " ") {
      maxPremiseLength
    } else {
      addressText.substring(0, maxPremiseLength).lastIndexOf(" ")
    }
    return addressText.substring(0, split).trim() to addressText.substring(split, addressText.length).trim()
  }

  private fun findOffenderAddress(addressText: String, postalCode: String?, offender: Offender): OffenderAddress? = offenderAddressRepository.findByOffender_RootOffenderId(offender.rootOffenderId!!)
    .firstOrNull {
      it.toFullAddress(null) == addressText.trimDpsAddress().trimEnd(',') && it.postalCode == postalCode?.trim()
    }

  private fun findCorporateAddress(name: String, addressText: String, postalCode: String?): CorporateAddress? {
    val corporateName = name.toCorporateName()
    return corporateAddressRepository.findAllByCorporate_CorporateName(corporateName)
      .firstOrNull {
        // Need to check address with and without corporate name - it might be included on a DPS address
        (it.toFullAddress(corporateName) == addressText.trimDpsAddress().trimEnd(',') || it.toFullAddress() == addressText.trimDpsAddress()) &&
          it.postalCode == postalCode?.trim()
      }
  }

  // Note that this assumes the address was created in DPS... a fair assumption because if the address was created in NOMIS we have the ID so never need to find it
  private fun findOrCreateOffenderAddress(addressText: String, postalCode: String?, offender: Offender): OffenderAddress {
    val (premise, street) = formatAddressText(addressText)
    tapAddressInsertRepository.insertAddressIfNotExists("OFF", offender.rootOffenderId!!, premise, street, postalCode)

    val address = offenderAddressRepository.findByOffender_RootOffenderId(offender.rootOffenderId!!)
      .first { it.matchesDpsAddress(premise, street, postalCode) }
    if (address.usages.none { it.addressUsage == rotlAddressType }) {
      address.apply {
        usages += AddressUsage(AddressUsageId(this, "ROTL"), true, rotlAddressType)
      }
    }

    return address
  }

  private fun Address.matchesDpsAddress(premise: String?, street: String?, postalCode: String?): Boolean = (this.premise == premise && this.street == street && this.postalCode == postalCode && flat == null && locality == null && city == null && county == null && country == null)

  private fun String.trimDpsAddress() = this.trim().trimEnd(',')
}

internal fun Address.toFullAddress(description: String? = null): String {
  val address = mutableListOf<String>()

  fun MutableList<String>.addIfNotEmpty(value: String?) {
    if (!value.isNullOrBlank()) {
      add(value.trim())
    }
  }

  fun String?.cleanAddressComponent(): String? {
    // remove corporate/agency description from any address elements that might contain it
    fun String?.withoutDescription() = description?.let { this?.replace(description, "") } ?: this

    // remove trailing commas because they will be added back when the components are joined together
    fun String?.withoutTrailingCommas() = this?.trim()?.trimEnd(',')

    return this.withoutDescription().withoutTrailingCommas()
  }

  // Append "Flat" if there is one
  if (!flat.isNullOrBlank()) {
    val flatText = if (flat!!.contains("flat", ignoreCase = true)) "" else "Flat "
    address.add("$flatText${flat!!.trim()}")
  }

  val cleanPremise = premise.cleanAddressComponent()
  val cleanStreet = street.cleanAddressComponent()
  val cleanLocality = locality.cleanAddressComponent()

  // Don't separate a numeric premise from the street, only if it's a name
  val hasPremise = !cleanPremise.isNullOrBlank()
  val premiseIsNumber = cleanPremise?.all { char -> char.isDigit() } ?: false
  val hasStreet = !cleanStreet.isNullOrBlank()
  when {
    hasPremise && premiseIsNumber && hasStreet -> address.add("$cleanPremise $cleanStreet")
    hasPremise && !premiseIsNumber && hasStreet -> address.add("$cleanPremise, $cleanStreet")
    hasPremise -> address.add(cleanPremise)
    hasStreet -> address.add(cleanStreet)
  }
  // Add others if they exist
  address.addIfNotEmpty(cleanLocality)
  address.addIfNotEmpty(city?.description)
  address.addIfNotEmpty(county?.description)
  address.addIfNotEmpty(country?.description)

  return address.joinToString(", ").trim()
    .takeIf { it.isNotBlank() }
    ?: description
    // If the NOMIS address is empty that's fine - it just won't match the address we're looking for. Maybe another address will match or if not we'll throw.
    ?: ""
}

private fun String.toCorporateName() = substring(0..(minOf(this.length, 40) - 1))

@Schema(description = "Upsert temporary absence address")
data class UpsertTapAddress(
  @Schema(description = "Address ID - if entered then this is a known NOMIS address, if not a new address is required based on the other properties")
  val id: Long? = null,

  @Schema(description = "The name of a corporation or agency")
  val name: String? = null,

  @Schema(description = "The full address text")
  val addressText: String? = null,

  @Schema(description = "The postal code")
  val postalCode: String? = null,
) {
  @Schema(hidden = true)
  fun hasNullValues(): Boolean = id == null && name == null && addressText == null && postalCode == null

  @Schema(hidden = true)
  fun isCorporateAddress(): Boolean = !name.isNullOrBlank() && name != addressText

  @Schema(hidden = true)
  fun copyAndTrim(): UpsertTapAddress = this.copy(name = name?.trim(), addressText = addressText?.trim(), postalCode = postalCode?.trim())
}
