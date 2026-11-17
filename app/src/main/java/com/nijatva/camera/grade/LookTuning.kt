package com.nijatva.camera.grade

/**
 * The four sliders a user can move on top of a look.
 *
 * Kept separate from [LookPreset] on purpose: a preset is a considered set of
 * numbers, and these are the user's own corrections on top of it. Each one is a
 * -1..1 nudge, so the value can be stored as an integer and moved back to zero
 * without losing the preset underneath.
 */
data class LookTuning(
    /** Lifts or lowers the whole frame. */
    val brightness: Float = 0f,
    /** Separates the midtones: more of an S-curve, same black and white points. */
    val contrast: Float = 0f,
    /** Scales colour around luma. */
    val saturation: Float = 0f,
    /** Scales the sharpening amount. */
    val sharpness: Float = 0f,
) {

    val isNeutral: Boolean
        get() = brightness == 0f && contrast == 0f && saturation == 0f && sharpness == 0f

    /**
     * Applies the nudges to a look.
     *
     * Multipliers are bounded: saturation can reach 1.5x the look's own value and
     * sharpening 1.6x, and neither can fall below half, so a slider at the end of
     * its travel still produces a photograph rather than a smear or a poster.
     */
    fun applyTo(look: ColorGrade.Look): ColorGrade.Look {
        if (isNeutral) return look
        return look.copy(
            brightness = (look.brightness + brightness.coerceIn(-1f, 1f) * 0.9f).coerceIn(-1f, 1f),
            contrast = (look.contrast + contrast.coerceIn(-1f, 1f)).coerceIn(-1f, 1f),
            saturation = (look.saturation * (1f + saturation.coerceIn(-1f, 1f) * 0.5f)).coerceIn(0.5f, 2f),
            sharpen = (look.sharpen * (1f + sharpness.coerceIn(-1f, 1f) * 0.6f)).coerceIn(0f, 1.5f),
        )
    }

    companion object {
        /** The JSON keys, so a settings export stays readable. */
        const val KEY_BRIGHTNESS = "tuneBrightness"
        const val KEY_CONTRAST = "tuneContrast"
        const val KEY_SATURATION = "tuneSaturation"
        const val KEY_SHARPNESS = "tuneSharpness"
    }
}
