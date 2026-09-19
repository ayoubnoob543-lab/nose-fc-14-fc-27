#!/usr/bin/env python3
import sys
from androguard.core.apk import APK
apk=APK(sys.argv[1])
print('package=', apk.get_package())
print('version_code=', apk.get_androidversion_code())
print('version_name=', apk.get_androidversion_name())
print('min_sdk=', apk.get_min_sdk_version())
print('target_sdk=', apk.get_target_sdk_version())
print('permissions=', len(apk.get_permissions()))
print('native_archs=', sorted(apk.get_native_code()))
