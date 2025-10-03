/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko.autofill

import mozilla.components.concept.engine.autofill.AddressField
import mozilla.components.concept.engine.autofill.SelectorAddressField
import mozilla.components.concept.engine.autofill.TextAddressField
import org.mozilla.geckoview.Autocomplete.AddressStructure
import org.mozilla.geckoview.GeckoResult

interface RuntimeAddressStructureAccessor {
    fun getAddressStructure(
        countryCode: String,
        onSuccess: (List<AddressField>) -> Unit,
        onError: (Throwable) -> Unit,
    )
}

internal class DefaultRuntimeAddressStructureAccessor: RuntimeAddressStructureAccessor {
    override fun getAddressStructure(
        countryCode: String,
        onSuccess: (List<AddressField>) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        handleGeckoResult(
            geckoResult = AddressStructure.getAddressStructure(countryCode)
                .toConceptAddressFields(),
            onSuccess = onSuccess,
            onError = onError,
        )
    }

    fun <T : Any> handleGeckoResult(
        geckoResult: GeckoResult<T>,
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        geckoResult.then(
            { res: T? ->
                onSuccess(res!!)
                GeckoResult<Void>()
            },
            { throwable ->
                onError(throwable)
                GeckoResult<Void>()
            },
        )
    }

    private fun GeckoResult<List<AddressStructure.Field>>.toConceptAddressFields(): GeckoResult<List<AddressField>> {
        return map { results ->
            results?.mapNotNull { result ->
                when (result) {
                    is AddressStructure.Field.SelectorField -> SelectorAddressField(
                        id = result.id,
                        localizationKey = result.localizationKey,
                        defaultSelectionKey = result.defaultValue,
                        options = result.options.map { option ->
                            SelectorAddressField.Option(
                                key = option.key,
                                value = option.value,
                            )
                        },
                    )

                    is AddressStructure.Field.TextField -> TextAddressField(
                        id = result.id,
                        localizationKey = result.localizationKey,
                    )

                    else -> null
                }
            } ?: emptyList()
        }
    }
}
