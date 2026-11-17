package com.nijatva.camera.grade

/**
 * Per-lens adjustments to the chosen look.
 *
 * One look applied to every camera is a compromise, and the phone makers do not do
 * it: a tuned camera app sets each lens separately, because the lenses are not the
 * same lens. The reference settings for a recent flagship set, for example, run
 * saturation 1.18 on the main camera, 1.20 on the ultra-wide, 1.15 on the 3x and
 * 1.10 on the periscope, with the front camera at 1.08 - punchiest where the frame
 * is wide and mostly landscape, most conservative where the pixels are smallest and
 * the noise is highest.
 *
 * These are multipliers, not absolute values, so they compose with whatever look the
 * user picked: Punch on the ultra-wide stays punchier than Punch on the telephoto,
 * and Natural stays neutral.
 */
object LensTuning {

    /** Which lens produced the frame, as far as colour is concerned. */
    enum class Lens { MAIN, ULTRA_WIDE, TELE, FRONT }

    /**
     * Classifies a camera.
     *
     * @param facing CameraCharacteristics.LENS_FACING_FRONT for the selfie camera
     * @param focalLength the lens focal length in mm
     * @param sensorWidthMm the physical sensor width, used to tell a real ultra-wide
     *   from a main camera on a phone whose main lens is itself wide
     */
    fun classify(facing: Int, focalLength: Float, sensorWidthMm: Float): Lens = when {
        facing == 1 -> Lens.FRONT // LENS_FACING_FRONT
        // A 35mm-equivalent focal length is the honest comparison; the sensor width
        // gives it without trusting the reported millimetres alone.
        equivalent35mm(focalLength, sensorWidthMm) <= 18f -> Lens.ULTRA_WIDE
        equivalent35mm(focalLength, sensorWidthMm) >= 50f -> Lens.TELE
        else -> Lens.MAIN
    }

    /** 35mm-equivalent focal length: focal * (36 / sensor width). */
    fun equivalent35mm(focalLength: Float, sensorWidthMm: Float): Float {
        if (focalLength <= 0f || sensorWidthMm <= 0f) return 26f // an ordinary main lens
        return focalLength * (36f / sensorWidthMm)
    }

    /** The look for this lens, derived from the user's choice. */
    fun adjust(look: ColorGrade.Look, lens: Lens): ColorGrade.Look = when (lens) {
        Lens.MAIN -> look
        // Wide frames are mostly sky and landscape, where colour and contrast carry
        // the picture and noise is not the limiting factor.
        Lens.ULTRA_WIDE -> look.copy(
            saturation = look.saturation * 1.03f,
            clarity = look.clarity * 1.15f,
        )
        // Telephoto frames are cropped and noisier; sharpening and colour both
        // amplify that, so they come down a little.
        Lens.TELE -> look.copy(
            saturation = look.saturation * 0.97f,
            sharpen = look.sharpen * 0.90f,
        )
        // A face is the subject: less sharpening on skin, gentler colour, a touch of
        // warmth.
        Lens.FRONT -> look.copy(
            saturation = look.saturation * 0.94f,
            sharpen = look.sharpen * 0.85f,
            warmth = look.warmth + 0.01f,
        )
    }
}
