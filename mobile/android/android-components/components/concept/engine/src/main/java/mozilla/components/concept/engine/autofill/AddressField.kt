/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.concept.engine.autofill

/**
 * Interface representing an address field
 *
 * @property id Identifier for the field. It is used to map from this to the UI element
 */
sealed interface AddressField {
    val id: String

    val localizationKey: String
}

/**
 * Text input address field
 */
class TextAddressField(override val id: String, override val localizationKey: String) : AddressField

/**
 * Selector address field
 *
 * @param id Identifier for the field. Used to map this to the right prompt value
 * @param defaultSelectionKey The key for the default value. Ideally this key would be represented in [options].
 * @param options List of [Option] options that the selector field represents.
 */
class SelectorAddressField(
    override val id: String,
    override val localizationKey: String,
    val defaultSelectionKey: String,
    val options: List<Option> = emptyList(),
) : AddressField {

    /**
     * An option item of an address field
     *
     * @param key The key to identify the elements
     * @param value The value if the address field option
     */
    data class Option(val key: String, val value: String)
}
