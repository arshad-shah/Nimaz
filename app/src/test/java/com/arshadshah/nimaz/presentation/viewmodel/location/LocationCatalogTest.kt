package com.arshadshah.nimaz.presentation.viewmodel.location

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import com.arshadshah.nimaz.presentation.screens.settings.formatCoordinates

/**
 * How a pair of coordinates is written out on the Location screen.
 *
 * The catalogue assertions that used to live here moved to `:core:domain`'s `CityCatalogTest`,
 * which is where `SearchLocation` and the two grouping functions live; this keeps the half that
 * belongs to the settings surface.
 */
class LocationCatalogTest {

    @Test
    fun `formatCoordinates names the hemisphere rather than signing the number`() {
        assertThat(formatCoordinates(21.4225, 39.8262)).isEqualTo("21.4225\u00b0 N, 39.8262\u00b0 E")
        assertThat(formatCoordinates(-6.2088, 106.8456)).isEqualTo("6.2088\u00b0 S, 106.8456\u00b0 E")
        assertThat(formatCoordinates(40.7128, -74.0060)).isEqualTo("40.7128\u00b0 N, 74.0060\u00b0 W")
        assertThat(formatCoordinates(-34.6037, -58.3816)).isEqualTo("34.6037\u00b0 S, 58.3816\u00b0 W")
    }
}
