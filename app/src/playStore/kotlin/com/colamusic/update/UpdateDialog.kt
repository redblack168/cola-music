package com.colamusic.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Play Store variant stub. Google Play policy §"Device and Network Abuse"
 * forbids apps from self-updating outside the Play install pipeline, so
 * the in-app updater is compiled out of this flavor entirely — no
 * UpdateChecker, no UpdateDialog, no REQUEST_INSTALL_PACKAGES permission,
 * no FileProvider path.
 *
 * This stub exists only because [ColaNavGraph] is shared across flavors
 * and already carries an `UpdateDialog(...)` call. Signature matches the
 * github-flavor composable exactly; calls are a no-op beyond clearing the
 * trigger flag so the UI state returns to idle.
 */
@Composable
fun UpdateDialog(
    triggerCheck: Boolean,
    onTriggerHandled: () -> Unit,
) {
    LaunchedEffect(triggerCheck) {
        if (triggerCheck) onTriggerHandled()
    }
}
