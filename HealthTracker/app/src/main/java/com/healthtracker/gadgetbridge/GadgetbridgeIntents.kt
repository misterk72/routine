package com.healthtracker.gadgetbridge

object GadgetbridgeIntents {
    // Use deprecated TRIGGER_EXPORT because current Gadgetbridge IntentApiReceiver
    // filter does not include TRIGGER_DATABASE_EXPORT.
    const val ACTION_TRIGGER_DB_EXPORT =
        "nodomain.freeyourgadget.gadgetbridge.command.TRIGGER_EXPORT"
    const val ACTION_DATABASE_EXPORT_SUCCESS =
        "nodomain.freeyourgadget.gadgetbridge.action.DATABASE_EXPORT_SUCCESS"
    const val ACTION_DATABASE_EXPORT_FAIL =
        "nodomain.freeyourgadget.gadgetbridge.action.DATABASE_EXPORT_FAIL"
}
