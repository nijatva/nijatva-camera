// Checks that the ARGB output path agrees with the TIFF path: same pixels, opaque
// alpha, and the same photographic mid-grey. No phone needed.
#include "../src/main/cpp/pipeline_core.h"

#include <cmath>
#include <cstdio>
#include <vector>

namespace {

constexpr int W = 64, H = 64;
constexpr float kBlack = 64.f, kWhite = 1023.f;

gcam::FrameInput greyCard(float r, float g, float b, std::vector<uint16_t>& raw) {
    raw.assign(W * H, 0);
    for (int y = 0; y < H; ++y) {
        for (int x = 0; x < W; ++x) {
            const bool evenRow = (y & 1) == 0, evenCol = (x & 1) == 0;
            float v;
            if (evenRow && evenCol) v = r;
            else if (!evenRow && !evenCol) v = b;
            else v = g;
            raw[y * W + x] = static_cast<uint16_t>(kBlack + v * (kWhite - kBlack) + 0.5f);
        }
    }
    gcam::FrameInput f;
    f.raw = raw.data();
    f.meta.black = kBlack;
    f.meta.white = kWhite;
    f.meta.strideU16 = W;
    f.meta.wbR = g / r;
    f.meta.wbG = 1.f;
    f.meta.wbB = g / b;
    return f;
}

}  // namespace

int main() {
    std::vector<uint16_t> raw;
    std::vector<gcam::FrameInput> frames{greyCard(0.18f, 0.18f, 0.18f, raw)};

    std::vector<uint32_t> argb;
    gcam::BurstStats st;
    if (!gcam::developBurstToArgb(frames, W, H, argb, &st)) {
        std::printf("FAIL: developBurstToArgb returned false\n");
        return 1;
    }
    if (argb.size() != static_cast<size_t>(W) * H) {
        std::printf("FAIL: expected %d pixels, got %zu\n", W * H, argb.size());
        return 1;
    }

    // Alpha must be opaque on every pixel, or Bitmap will blend the frame away.
    int notOpaque = 0;
    for (uint32_t p : argb) if ((p >> 24) != 0xFF) ++notOpaque;

    const uint32_t centre = argb[(H / 2) * W + (W / 2)];
    const int r = (centre >> 16) & 0xFF, g = (centre >> 8) & 0xFF, b = centre & 0xFF;
    const double srgb = r / 255.0;

    std::printf("pixels %zu, opaque failures %d, centre RGB %d %d %d (%.3f sRGB)\n",
                argb.size(), notOpaque, r, g, b, srgb);
    std::printf("timings ms: align %.1f merge %.1f demosaic %.1f pack %.1f\n",
                st.alignMs, st.mergeMs, st.demosaicMs, st.writeMs);

    bool ok = true;
    if (notOpaque != 0) { std::printf("FAIL: %d pixels are not opaque\n", notOpaque); ok = false; }
    if (std::abs(r - g) > 2 || std::abs(g - b) > 2) {
        std::printf("FAIL: a grey card must stay grey\n"); ok = false;
    }
    // Same photographic target the TIFF path is held to: 18% grey lands near 0.46.
    if (std::abs(srgb - 0.46) > 0.02) {
        std::printf("FAIL: mid-grey %.3f is not near 0.46\n", srgb); ok = false;
    }
    std::printf("%s\n", ok ? "PASS" : "FAIL");
    return ok ? 0 : 1;
}
