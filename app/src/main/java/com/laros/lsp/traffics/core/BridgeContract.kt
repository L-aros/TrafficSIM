package com.laros.lsp.traffics.core

object BridgeContract {
    const val PERMISSION_INTERNAL_BRIDGE = "com.laros.lsp.traffics.permission.INTERNAL_BRIDGE"
    const val ACTION_SWITCH_DATA = "com.laros.lsp.traffics.ACTION_SWITCH_DATA"
    const val ACTION_SWITCH_RESULT = "com.laros.lsp.traffics.ACTION_SWITCH_RESULT"

    const val EXTRA_TOKEN = "token"
    const val EXTRA_TARGET_SLOT = "target_slot"
    const val EXTRA_TARGET_SUB_ID = "target_sub_id"
    const val EXTRA_REASON = "reason"
    const val EXTRA_REQUEST_PACKAGE = "request_package"
    const val EXTRA_SUCCESS = "success"
    const val EXTRA_MESSAGE = "message"
    const val EXTRA_SOURCE_PACKAGE = "source_package"

    val PHONE_PACKAGES = listOf(
        "android",
        "com.android.phone",
        "com.xiaomi.phone",
        "com.qti.phone",
        "com.qualcomm.qti.telephonyservice",
        "com.android.services.telephony",
        "com.android.telephony"
    )
}
