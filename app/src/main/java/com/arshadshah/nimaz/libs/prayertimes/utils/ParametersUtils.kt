package com.arshadshah.nimaz.libs.prayertimes.utils

import com.arshadshah.nimaz.libs.prayertimes.calculationClasses.CalculationParameters
import com.arshadshah.nimaz.libs.prayertimes.enums.CalculationMethod
import com.arshadshah.nimaz.libs.prayertimes.enums.Madhab
import com.arshadshah.nimaz.libs.prayertimes.enums.ShadowLength
import com.arshadshah.nimaz.libs.prayertimes.objects.Coordinates
import com.arshadshah.nimaz.libs.prayertimes.objects.PrayerAdjustments

internal object ParametersUtils {
    /**
     * Return the CalculationParameters for the given method
     *
     * @return CalculationParameters for the given Calculation method
     */
    fun getParametersForMethod(method: CalculationMethod): CalculationParameters {
        return when (method) {
            CalculationMethod.MWL -> {
                CalculationParameters(
                    18.0,
                    17.0,
                    method
                )
                    .withCoordinates(
                        Coordinates(
                            51.5194682,
                            -0.1270063
                        )
                    )
            }

            CalculationMethod.EGYPTIAN -> {
                CalculationParameters(
                    19.5,
                    17.5,
                    method
                ).withCoordinates(
                    Coordinates(
                        30.0444196,
                        31.2357116
                    )
                )
            }

            CalculationMethod.KARACHI -> {
                CalculationParameters(
                    18.0,
                    18.0,
                    method
                ).withCoordinates(
                    Coordinates(
                        24.8614622,
                        67.0099388
                    )
                )
            }

            CalculationMethod.MAKKAH -> {
                CalculationParameters(
                    18.5,
                    90,
                    method
                ).withCoordinates(
                    Coordinates(
                        21.3890824,
                        39.8579118
                    )
                )
            }

            CalculationMethod.DUBAI -> {
                CalculationParameters(
                    18.2,
                    18.2,
                    method
                ).withCoordinates(
                    Coordinates(
                        25.2048493,
                        55.2707828
                    )
                )
            }

            CalculationMethod.MOONSIGHTING -> {
                CalculationParameters(
                    18.0,
                    18.0,
                    method
                )
            }

            CalculationMethod.ISNA -> {
                CalculationParameters(
                    15.0,
                    15.0,
                    method
                ).withCoordinates(
                    Coordinates(
                        39.70421229999999,
                        -86.399438
                    )
                )
            }

            CalculationMethod.KUWAIT -> {
                CalculationParameters(
                    18.0,
                    17.5,
                    method
                ).withCoordinates(
                    Coordinates(
                        29.31166,
                        47.481766
                    )
                )
            }

            CalculationMethod.QATAR -> {
                CalculationParameters(
                    18.0,
                    90,
                    method
                ).withCoordinates(
                    Coordinates(
                        25.354826,
                        51.183884
                    )
                )
            }

            CalculationMethod.SINGAPORE -> {
                CalculationParameters(
                    20.0,
                    18.0,
                    method
                ).withCoordinates(
                    Coordinates(
                        1.352083,
                        103.819836
                    )
                )
            }

            CalculationMethod.FRANCE -> {
                CalculationParameters(
                    12.0,
                    12.0,
                    method
                ).withCoordinates(
                    Coordinates(
                        48.856614,
                        2.3522219
                    )
                )
            }

            CalculationMethod.TURKEY -> {
                CalculationParameters(
                    18.0,
                    17.0,
                    method
                ).withCoordinates(
                    Coordinates(
                        39.9333635,
                        39.9333635
                    )
                )
            }

            CalculationMethod.RUSSIA -> {
                CalculationParameters(
                    16.0,
                    15.0,
                    method
                ).withCoordinates(
                    Coordinates(
                        55.755826,
                        55.755826
                    )
                )
            }

            CalculationMethod.IRELAND -> {
                CalculationParameters(
                    16.0,
                    14.0,
                    method
                ).withCoordinates(
                    Coordinates(
                        53.3498053,
                        -6.2603097
                    )
                ).withMadhab(Madhab.HANAFI)
            }

            CalculationMethod.TEHRAN -> {
                CalculationParameters(
                    17.7,
                    14.0,
                    method
                ).withCoordinates(
                    Coordinates(
                        35.6891975,
                        51.3889736
                    )
                )
                    .withAdjustments(
                        PrayerAdjustments(
                            0,
                            0,
                            0,
                            0,
                            4,
                            0,
                        )
                    )
            }

            CalculationMethod.SHIA -> {
                CalculationParameters(
                    16.0,
                    14.0,
                    method
                ).withCoordinates(
                    Coordinates(
                        35.6891975,
                        51.3889736
                    )
                )
                    .withAdjustments(
                        PrayerAdjustments(
                            0,
                            0,
                            0,
                            0,
                            4,
                            0,
                        )
                    )
            }

            CalculationMethod.GULF -> {
                CalculationParameters(
                    19.5,
                    90,
                    method
                ).withCoordinates(
                    Coordinates(
                        25.2048493,
                        55.2707828
                    )
                )
            }

            CalculationMethod.OTHER -> {
                CalculationParameters(
                    0.0,
                    0.0,
                    method
                ).withCoordinates(
                    Coordinates(
                        0.0,
                        0.0
                    )
                )
            }

        }
    }

    /**
     * madhab used to calculate Asr
     * */
    fun getShadowLengthForMadhab(madhab: Madhab): ShadowLength {
        return when (madhab) {
            Madhab.SHAFI -> {
                ShadowLength.SINGLE
            }

            Madhab.HANAFI -> {
                ShadowLength.DOUBLE
            }
        }
    }


    /**
     * Return all the Calculations methods and thier parameters
     * */
    fun getAllCalculationMethods(): List<CalculationParameters> {
        val calculationParametersList = mutableListOf<CalculationParameters>()
        for (method in CalculationMethod.values()) {
            calculationParametersList.add(getParametersForMethod(method))
        }
        return calculationParametersList
    }
}