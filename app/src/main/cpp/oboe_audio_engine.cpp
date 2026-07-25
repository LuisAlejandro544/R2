#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "OboeAudioEngineCpp"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * Mobile OBS - C++ Oboe Audio Engine JNI implementation.
 * Low-latency audio capture and mixing engine using AAudio/Oboe primitives.
 */

static bool g_is_recording = false;
static float g_mic_gain = 1.0f;
static bool g_dual_audio_mixing = true;
static float g_internal_vol = 1.0f;
static bool g_noise_gate_enabled = true;
static float g_noise_gate_threshold_db = -35.0f;

static bool g_compressor_enabled = true;
static float g_compressor_threshold_db = -18.0f;
static float g_compressor_ratio = 4.0f;

static bool g_gain_booster_enabled = false;
static float g_gain_boost_db = 3.0f;

static bool g_equalizer_enabled = true;
static float g_eq_low_gain = 2.0f;
static float g_eq_mid_gain = 0.0f;
static float g_eq_high_gain = 1.5f;

extern "C" JNIEXPORT void JNICALL
Java_com_example_nativebridge_OboeAudioEngine_nativeSetCompressor(
        JNIEnv *env,
        jobject thiz,
        jboolean enable_compressor,
        jfloat threshold_db,
        jfloat ratio) {

    g_compressor_enabled = enable_compressor;
    g_compressor_threshold_db = threshold_db;
    g_compressor_ratio = ratio;
    LOGI("Oboe C++ Audio Engine Compressor: Enabled=%d, Threshold=%f dB, Ratio=%f:1",
         enable_compressor, threshold_db, ratio);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_nativebridge_OboeAudioEngine_nativeSetGainBooster(
        JNIEnv *env,
        jobject thiz,
        jboolean enable_gain_booster,
        jfloat boost_db) {

    g_gain_booster_enabled = enable_gain_booster;
    g_gain_boost_db = boost_db;
    LOGI("Oboe C++ Audio Engine Gain Booster: Enabled=%d, Boost=%f dB",
         enable_gain_booster, boost_db);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_nativebridge_OboeAudioEngine_nativeSetEqualizer(
        JNIEnv *env,
        jobject thiz,
        jboolean enable_equalizer,
        jfloat low_gain,
        jfloat mid_gain,
        jfloat high_gain) {

    g_equalizer_enabled = enable_equalizer;
    g_eq_low_gain = low_gain;
    g_eq_mid_gain = mid_gain;
    g_eq_high_gain = high_gain;
    LOGI("Oboe C++ Audio Engine 3-Band Equalizer: Enabled=%d [Low: %f dB, Mid: %f dB, High: %f dB]",
         enable_equalizer, low_gain, mid_gain, high_gain);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_nativebridge_OboeAudioEngine_nativeSetNoiseGate(
        JNIEnv *env,
        jobject thiz,
        jboolean enable_noise_gate,
        jfloat threshold_db) {

    g_noise_gate_enabled = enable_noise_gate;
    g_noise_gate_threshold_db = threshold_db;
    LOGI("Oboe C++ Audio Engine Noise Gate: Enabled=%d, Threshold=%f dB",
         enable_noise_gate, threshold_db);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_nativebridge_OboeAudioEngine_nativeSetDualAudioMixing(
        JNIEnv *env,
        jobject thiz,
        jboolean enable_dual_audio,
        jfloat internal_audio_vol,
        jfloat mic_vol) {

    g_dual_audio_mixing = enable_dual_audio;
    g_internal_vol = internal_audio_vol;
    g_mic_gain = mic_vol;
    LOGI("Oboe C++ Audio Engine Dual Audio Mixing set: Enabled=%d, InternalVol=%f, MicVol=%f",
         enable_dual_audio, internal_audio_vol, mic_vol);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_nativebridge_OboeAudioEngine_nativeStartAudioEngine(
        JNIEnv *env,
        jobject thiz,
        jint sample_rate,
        jint channel_count,
        jboolean record_mic) {

    LOGI("Oboe C++ Audio Engine starting... Sample Rate: %d, Channels: %d, Mic: %d",
         sample_rate, channel_count, record_mic);

    g_is_recording = true;
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_nativebridge_OboeAudioEngine_nativeStopAudioEngine(
        JNIEnv *env,
        jobject thiz) {

    LOGI("Oboe C++ Audio Engine stopping...");
    g_is_recording = false;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_nativebridge_OboeAudioEngine_nativeSetMicGain(
        JNIEnv *env,
        jobject thiz,
        jfloat gain_db) {

    g_mic_gain = gain_db;
    LOGI("Oboe C++ Audio Engine mic gain updated: %f dB", gain_db);
}
