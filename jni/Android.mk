JNI_DIR := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE     := native-utils
LOCAL_C_INCLUDES := $(JNI_DIR)/utils/
LOCAL_CFLAGS     += -Wall

LOCAL_SRC_FILES := $(JNI_DIR)/utils/org_thoughtcrime_securesms_util_FileUtils.cpp
LOCAL_SRC_FILES += $(JNI_DIR)/utils/org_thoughtcrime_securesms_service_MemoryWipeService.c

include $(BUILD_SHARED_LIBRARY)