package com.nijatva.camera.processor

import java.nio.ByteBuffer

class GcamEngine {

    companion object {
        init {
            System.loadLibrary("gcamprocessor")
        }
    }

    /**
     * Runs the burst pipeline and returns packed ARGB_8888 pixels, row-major, or
     * null if the native side rejects the input. This is the entry point a capture
     * mode should use: the pipeline's own output is a 16-bit TIFF, which Android
     * cannot display.
     */
    external fun nativeProcessRawFramesArgb(
        buffers: Array<ByteBuffer>,
        meta: FloatArray,
        width: Int,
        height: Int
    ): IntArray?

    /**
     * Develops one RAW plane to packed ARGB_8888 pixels (see raw_develop.cpp).
     * Returns null if the native side rejects the input.
     */
    external fun nativeDevelopRaw(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStrideBytes: Int,
        blackLevel: Float,
        whiteLevel: Float,
        wbGains: FloatArray
    ): IntArray?

    /**
     * Full burst pipeline (see pipeline_core.cpp). meta is 19 floats per frame:
     * [iso, exposureNs, wbR, wbG, wbB, black, white, noiseA, noiseB, strideBytes, ccm 3x3].
     * Returns [referenceIndex, alignMs, mergeMs, demosaicMs, writeMs,
     *          then per frame: shiftX, shiftY, exposureRatio], or null on failure.
     */
    external fun nativeProcessRawFramesV2(
        buffers: Array<ByteBuffer>,
        meta: FloatArray,
        width: Int,
        height: Int,
        outputPath: String
    ): FloatArray?

}
