package com.nijatva.camera.grade

import com.nijatva.camera.R

/**
 * The looks the app can develop merged photos with.
 *
 * Each one is a set of numbers for the same pipeline, not a separate code path, so
 * switching looks cannot break anything: the stages run in the same order and only
 * their strengths change. That also keeps the cost identical whatever the look.
 *
 * These are taste, not truth. Natural is the neutral starting point; the other
 * three borrow the character of the looks people compare phone cameras by - lifted
 * shadows and warmth, or strong colour and crispness - without pretending to be
 * anyone's actual colour science, which comes from per-device tuning nobody outside
 * that vendor has.
 */
enum class LookPreset(
    val labelRes: Int,
    val clarity: Float,
    val saturation: Float,
    val shoulder: Float,
    val warmth: Float,
    val shadowLift: Float,
    val sharpen: Float,
    val chromaNr: Float,
) {
    /** Neutral: the measured mid-grey, gentle clarity, nothing pushed. */
    NATURAL(
        labelRes = R.string.look_natural,
        clarity = 0.18f,
        saturation = 1.08f,
        shoulder = 0.92f,
        warmth = 0.0f,
        shadowLift = 0.0f,
        sharpen = 0.25f,
        chromaNr = 0.35f,
    ),

    /** Deeper shadows, cooler shadows against warmer highlights, more midtone bite. */
    CINEMATIC(
        labelRes = R.string.look_cinematic,
        clarity = 0.30f,
        saturation = 1.02f,
        shoulder = 0.86f,
        warmth = 0.04f,
        shadowLift = -0.02f,
        sharpen = 0.35f,
        chromaNr = 0.30f,
    ),

    /** Strong colour and crispness, the way a vivid phone profile reads. */
    VIVID(
        labelRes = R.string.look_vivid,
        clarity = 0.24f,
        saturation = 1.24f,
        shoulder = 0.90f,
        warmth = 0.01f,
        shadowLift = 0.01f,
        sharpen = 0.55f,
        chromaNr = 0.45f,
    ),

    /** Lifted shadows, warm cast, soft highlights: forgiving and gentle. */
    SOFT(
        labelRes = R.string.look_soft,
        clarity = 0.12f,
        saturation = 1.04f,
        shoulder = 0.80f,
        warmth = 0.06f,
        shadowLift = 0.05f,
        sharpen = 0.15f,
        chromaNr = 0.50f,
    ),
}
