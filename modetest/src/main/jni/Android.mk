LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := cmfaaudio
LOCAL_SRC_FILES := aaudio_beep.c
LOCAL_LDLIBS := -laaudio -llog -ldl
LOCAL_CFLAGS := -Wall -Werror
include $(BUILD_SHARED_LIBRARY)
