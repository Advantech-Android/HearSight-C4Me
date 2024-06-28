package com.codewithkael.firebasevideocall.utils

object setViewFields
{

    const val IS_VIDEO_CALL="isVideoCall"
    const val TARGET="target"
    const val IS_CALLER="isCaller"

    const val IS_INCOMING="isIncoming"
    const val TIMER="timer"

    //intent extras key
    const val EXTRA_USER_PHONE: String ="userphone"
    const val EXTRA_USER_NAME="username"
    const val EXTRA_IS_VIDEO_CALL="isVideoCall"
    const val EXTRA_TARGET="target"
    const val EXTRA_IS_CALLER="isCaller"
    const val EXTRA_SHOULD_MUTED="shouldBeMuted"
    const val EXTRA_IS_INCOMING="isIncoming"
    const val EXTRA_TIMER="timer"
    const val EXTRA_RESULT="result"
    const val EXTRA_RESTART="Restart"
    const val EXTRA_SIM_NO="fetchedSimNo"

    //shared preference key
    const val PREF_NAME="see_for_me"
    const val KEY_IS_LOGIN = "is_login"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_PHONE = "user_phone"
    const val KEY_IS_MIRROR="isMirror"


    //Fire base path
    const val PATH_USER_NAME="user_name"
    const val PATH_CONTACTS="contacts"
    const val PATH_STATUS= "status"

    const val CALLER_NAME="callerName"
    const val TYPE="type"
    const val IS_STARTING="isStarting"

    const val END_CALL="EndCall"
    const val ACCEPT_CALL="AcceptCall"
    const val IS_METERED="isMetered"
    const val GET_NW_POLICY="getNetworkPolicies"

    const val ACTION_EXIT="ACTION_EXIT"
    const val CHANNEL_ID1 = "channel1"
    const val CHANNEL_ID2 = "channel2"
    const val CHANNEL_NAME1 = "foreground"
    const val CHANNEL_NAME2 = "foreground1"
    const val NOTIFICATION_ACTION_WIFI_SCAN = "ACTION_WIFI_SCAN"
    const val NOTIFICATION_ACTION_EXIT = "Exit"
    const val NOTIFICATION_TITLE = "Wifi"

}