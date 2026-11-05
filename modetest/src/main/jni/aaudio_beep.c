// The same left-only beep as the other tests (0.3 s of 880 Hz, 0.3 s of
// silence), played through AAudio the way a low-latency game asks for it:
// performance mode LOW_LATENCY and, for the first button, an exclusive
// stream, which is what makes Android try its direct (MMAP) path.
#include <jni.h>
#include <aaudio/AAudio.h>
#include <dlfcn.h>
#include <math.h>
#include <stdio.h>
#include <stdbool.h>
#include <string.h>

#ifndef AAUDIO_FORMAT_PCM_I32
#define AAUDIO_FORMAT_PCM_I32 4
#endif

static AAudioStream *stream;
static int64_t pos;
static int32_t fmt, chans, rate;

static aaudio_data_callback_result_t cb(AAudioStream *s, void *user, void *data, int32_t n) {
  (void)s; (void)user;
  const int64_t period = rate * 6 / 10, on = rate * 3 / 10;
  for (int32_t i = 0; i < n; i++, pos++) {
    const int64_t t = pos % period;
    float v = 0.f;
    if (t < on) {
      float fade = fminf(1.f, fminf((float)t, (float)(on - t)) / (rate / 100.f));
      v = sinf(2.f * (float)M_PI * 880.f * (float)t / (float)rate) * 0.37f * fade;
    }
    for (int32_t c = 0; c < chans; c++) {
      const float x = c == 0 ? v : 0.f;   // channel 0 is left; the rest stay silent
      const int32_t k = i * chans + c;
      if (fmt == AAUDIO_FORMAT_PCM_FLOAT) ((float *)data)[k] = x;
      else if (fmt == AAUDIO_FORMAT_PCM_I16) ((int16_t *)data)[k] = (int16_t)(x * 32767.f);
      else if (fmt == AAUDIO_FORMAT_PCM_I32) ((int32_t *)data)[k] = (int32_t)(x * 2147483647.f);
    }
  }
  return AAUDIO_CALLBACK_RESULT_CONTINUE;
}

static const char *share_name(aaudio_sharing_mode_t m) {
  return m == AAUDIO_SHARING_MODE_EXCLUSIVE ? "EXCLUSIVE" : "SHARED";
}
static const char *perf_name(aaudio_performance_mode_t m) {
  return m == AAUDIO_PERFORMANCE_MODE_LOW_LATENCY ? "LOW_LATENCY"
       : m == AAUDIO_PERFORMANCE_MODE_POWER_SAVING ? "POWER_SAVING" : "NONE";
}
static const char *fmt_name(int32_t f) {
  return f == AAUDIO_FORMAT_PCM_FLOAT ? "float" : f == AAUDIO_FORMAT_PCM_I16 ? "16-bit"
       : f == AAUDIO_FORMAT_PCM_I32 ? "32-bit" : "other";
}

JNIEXPORT jstring JNICALL
Java_com_tiimoo_cmfstereo_modetest_AAudioBeep_start(JNIEnv *env, jclass cls, jboolean exclusive) {
  (void)cls;
  char msg[256];
  if (stream) return (*env)->NewStringUTF(env, "already playing");
  AAudioStreamBuilder *b;
  aaudio_result_t r = AAudio_createStreamBuilder(&b);
  if (r != AAUDIO_OK) {
    snprintf(msg, sizeof msg, "AAudio builder failed: %s", AAudio_convertResultToText(r));
    return (*env)->NewStringUTF(env, msg);
  }
  AAudioStreamBuilder_setDirection(b, AAUDIO_DIRECTION_OUTPUT);
  AAudioStreamBuilder_setPerformanceMode(b, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
  AAudioStreamBuilder_setSharingMode(b, exclusive ? AAUDIO_SHARING_MODE_EXCLUSIVE : AAUDIO_SHARING_MODE_SHARED);
  AAudioStreamBuilder_setChannelCount(b, 2);
  AAudioStreamBuilder_setSampleRate(b, 48000);
  AAudioStreamBuilder_setDataCallback(b, cb, NULL);
  r = AAudioStreamBuilder_openStream(b, &stream);
  AAudioStreamBuilder_delete(b);
  if (r != AAUDIO_OK) {
    stream = NULL;
    snprintf(msg, sizeof msg, "AAudio open failed: %s", AAudio_convertResultToText(r));
    return (*env)->NewStringUTF(env, msg);
  }
  fmt = AAudioStream_getFormat(stream);
  chans = AAudioStream_getChannelCount(stream);
  rate = AAudioStream_getSampleRate(stream);
  if (rate <= 0) rate = 48000;
  pos = 0;
  // AAudioStream_isMMapUsed is a testing call outside the NDK's headers;
  // libaaudio exports it on most builds.
  const char *mmap = "unknown";
  void *lib = dlopen("libaaudio.so", RTLD_NOW);
  if (lib) {
    bool (*used)(AAudioStream *) = (bool (*)(AAudioStream *))dlsym(lib, "AAudioStream_isMMapUsed");
    if (used) mmap = used(stream) ? "YES (direct path)" : "no";
  }
  r = AAudioStream_requestStart(stream);
  snprintf(msg, sizeof msg, "AAudio %s %s, %s, %d ch, %d Hz\nDirect (MMAP) path: %s%s",
           share_name(AAudioStream_getSharingMode(stream)),
           perf_name(AAudioStream_getPerformanceMode(stream)), fmt_name(fmt), chans, rate, mmap,
           r == AAUDIO_OK ? "" : "\nstart failed");
  return (*env)->NewStringUTF(env, msg);
}

JNIEXPORT void JNICALL
Java_com_tiimoo_cmfstereo_modetest_AAudioBeep_stop(JNIEnv *env, jclass cls) {
  (void)env; (void)cls;
  if (!stream) return;
  AAudioStream_requestStop(stream);
  AAudioStream_close(stream);
  stream = NULL;
}
