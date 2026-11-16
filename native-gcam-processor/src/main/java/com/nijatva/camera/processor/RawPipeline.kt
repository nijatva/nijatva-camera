package com.nijatva.camera.processor

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.TotalCaptureResult
import android.graphics.Bitmap
import android.media.Image
import java.io.File
import java.nio.ByteBuffer

/**
 * Runs the native burst pipeline on RAW_SENSOR images and writes a 16-bit linear TIFF.
 * Images must come from the same camera and be RAW_SENSOR, with the frame's own
 * capture result and characteristics.
 */
class RawPipeline(private val engine: GcamEngine = GcamEngine()) {

    data class Result(
        val referenceIndex: Int,
        val shiftsX: IntArray,
        val shiftsY: IntArray,
        val exposureRatios: FloatArray,
        val alignMs: Float,
        val mergeMs: Float,
        val demosaicMs: Float,
        val writeMs: Float
    )

    /**
     * Caller keeps ownership of the images and closes them afterwards.
     * Returns null if the native side rejects the input or the write fails.
     */
    /**
     * Develops the burst and returns it as a Bitmap, which is what a capture mode
     * can actually save and show. Same stages as [process]; the only difference is
     * the output format.
     */
    fun processToBitmap(
        images: List<Image>,
        results: List<TotalCaptureResult>,
        characteristics: CameraCharacteristics,
    ): Bitmap? {
        val prepared = prepare(images, results, characteristics) ?: return null
        val pixels = engine.nativeProcessRawFramesArgb(
            prepared.buffers, prepared.meta, prepared.width, prepared.height
        ) ?: return null
        return Bitmap.createBitmap(
            pixels, prepared.width, prepared.height, Bitmap.Config.ARGB_8888
        )
    }

    fun process(
        images: List<Image>,
        results: List<TotalCaptureResult>,
        characteristics: CameraCharacteristics,
        outputTiff: File
    ): Result? {
        val prepared = prepare(images, results, characteristics) ?: return null
        val buffers = prepared.buffers
        val meta = prepared.meta
        val width = prepared.width
        val height = prepared.height

        val out = engine.nativeProcessRawFramesV2(
            buffers, meta, width, height, outputTiff.absolutePath
        ) ?: return null

        val n = images.size
        return Result(
            referenceIndex = out[0].toInt(),
            shiftsX = IntArray(n) { out[5 + it * 3].toInt() },
            shiftsY = IntArray(n) { out[5 + it * 3 + 1].toInt() },
            exposureRatios = FloatArray(n) { out[5 + it * 3 + 2] },
            alignMs = out[1],
            mergeMs = out[2],
            demosaicMs = out[3],
            writeMs = out[4]
        )
    }

    /** Fills the flat metadata block the native side reads. */
    private fun buildMeta(
        images: List<Image>,
        results: List<TotalCaptureResult>,
        characteristics: CameraCharacteristics,
        meta: FloatArray,
    ) {
        val black = RawDeveloper.blackLevel(characteristics)
        val white = RawDeveloper.whiteLevel(characteristics)
        images.forEachIndexed { i, image ->
            val res = results[i]
            val wb = RawDeveloper.whiteBalanceGains(res)
            val stride = image.planes[0].rowStride
            if (stride % 2 != 0) return  // not a 16-bit RAW plane; the frame is dropped
            val base = i * META_STRIDE
            meta[base + 0] = (res.get(android.hardware.camera2.CaptureResult.SENSOR_SENSITIVITY) ?: 100).toFloat()
            meta[base + 1] = (res.get(android.hardware.camera2.CaptureResult.SENSOR_EXPOSURE_TIME) ?: 0L).toFloat()
            meta[base + 2] = wb[0]
            meta[base + 3] = wb[1]
            meta[base + 4] = wb[2]
            meta[base + 5] = black
            meta[base + 6] = white
            // The sensor's own noise profile, converted to raw DN. The placeholder
            // is only used by a device that does not publish one, where a merge
            // weight of 1/variance would otherwise be nonsense.
            val noise = NoiseModel.fromResult(res, white)
            meta[base + 7] = noise?.a ?: NOISE_A_PLACEHOLDER
            meta[base + 8] = noise?.b ?: NOISE_B_PLACEHOLDER
            meta[base + 9] = stride.toFloat()
            // COLOR_CORRECTION_TRANSFORM, row-major, camera RGB -> linear sRGB.
            val ccm = res.get(android.hardware.camera2.CaptureResult.COLOR_CORRECTION_TRANSFORM)
            for (row in 0..2) for (col in 0..2) {
                meta[base + 10 + row * 3 + col] = ccm?.let {
                    val e = it.getElement(col, row)
                    e.numerator.toFloat() / e.denominator
                } ?: if (row == col) 1f else 0f
            }
        }
    }

    private class Prepared(
        val buffers: Array<ByteBuffer>,
        val meta: FloatArray,
        val width: Int,
        val height: Int,
    )

    /**
     * Everything both output formats need: the direct buffers, the flat metadata
     * block, and the frame size. Returns null if the input is not a usable burst.
     */
    private fun prepare(
        images: List<Image>,
        results: List<TotalCaptureResult>,
        characteristics: CameraCharacteristics,
    ): Prepared? {
        if (images.isEmpty() || images.size != results.size || images.size > 16) return null
        val width = images[0].width
        val height = images[0].height
        if (images.any { it.width != width || it.height != height }) return null
        if (width < 32 || height < 32) return null
        val buffers = Array(images.size) { i ->
            images[i].planes[0].buffer.also { if (!it.isDirect) return null }
        }
        val meta = FloatArray(images.size * META_STRIDE)
        buildMeta(images, results, characteristics, meta)
        return Prepared(buffers, meta, width, height)
    }

    companion object {
        private const val META_STRIDE = 19
        // Placeholders only. The noise model must be measured per device and ISO.
        private const val NOISE_A_PLACEHOLDER = 0.0f
        private const val NOISE_B_PLACEHOLDER = 9.0f
    }
}
