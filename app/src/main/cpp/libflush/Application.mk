# Use alternate build script
APP_BUILD_SCRIPT := Android.mk
#include $(BUILD_PREBUILT)

# This variable contains the name of the target Android platform.
APP_PLATFORM := android-29

# By default, the NDK build system generates machine code for the armeabi ABI.
# This machine code corresponds to an ARMv5TE-based CPU with software floating
# point operations. You can use APP_ABI to select a different ABI.
#
# See https://developer.android.com/ndk/guides/application_mk.html
APP_ABI := x86_64 armeabi-v7a arm64-v8a
#LOCAL_XOM := false

#COMPILER_FILTER := speed
#PRODUCT_DEX_PREOPT_DEFAULT_COMPILER_FILTER=${COMPILER_FILTER}
