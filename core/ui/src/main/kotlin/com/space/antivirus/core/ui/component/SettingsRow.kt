package com.space.antivirus.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.space.antivirus.core.designsystem.theme.Elevation
import com.space.antivirus.core.designsystem.theme.IconTokens
import com.space.antivirus.core.designsystem.theme.LayoutTokens
import com.space.antivirus.core.designsystem.theme.LocalSpacing
import com.space.antivirus.core.designsystem.theme.ShapeTokens

/**
 * The single row anatomy every Settings screen is built from — Sprint 043A.
 *
 * Before this, Settings composed `AppCard` with a bespoke trailing slot
 * per control, which was fine for three rows and would not have survived
 * seven sections across five screens. One component means one set of
 * spacing, one touch-target guarantee, and one accessibility story
 * instead of five that drift.
 *
 * **On the trailing control being an enum rather than a slot.** A
 * `@Composable` trailing lambda would be more flexible, and that is
 * exactly the problem: this project's whole discipline is that a control
 * exists only when it changes real behavior (see Sprint 043's own
 * out-of-scope list, where three proposed toggles were cut for backing
 * nothing). A closed set of trailing controls makes "add a switch here"
 * a deliberate act rather than a one-line convenience.
 *
 * **Accessibility.** The row is the touch target, never the control
 * inside it: a `Switch` alone is a ~32dp target, and TalkBack users
 * would otherwise get "switch, on" with no idea what it switches. The
 * whole row therefore carries the semantics — role, merged label, and a
 * single click action — and the inner control is cleared from the tree
 * so it is not announced twice.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    icon: ImageVector? = null,
    control: SettingsRowControl = SettingsRowControl.None,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val spacing = LocalSpacing.current
    val contentAlpha = if (enabled) 1f else DISABLED_ALPHA

    val clickAction: (() -> Unit)? = when {
        !enabled -> null
        control is SettingsRowControl.Toggle -> ({ control.onCheckedChange(!control.checked) })
        else -> onClick
    }

    val rowSemantics = Modifier.semanticsFor(
        title = title,
        supportingText = supportingText,
        control = control,
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeTokens.card,
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.card),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = LayoutTokens.minTouchTarget)
                .then(
                    if (clickAction != null) {
                        Modifier.clickable(onClick = clickAction)
                    } else {
                        Modifier
                    },
                )
                .padding(spacing.medium)
                .then(rowSemantics),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                LeadingIcon(icon = icon, alpha = contentAlpha)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (icon != null) spacing.medium else 0.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                )
                supportingText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        modifier = Modifier.padding(top = spacing.tight),
                    )
                }
            }
            TrailingControl(control = control, enabled = enabled)
        }
    }
}

@Composable
private fun LeadingIcon(icon: ImageVector, alpha: Float) {
    val isDark = isSystemInDarkTheme()
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
    Box(
        modifier = Modifier
            .size(LEADING_BADGE_SIZE)
            .clip(ShapeTokens.iconBadge)
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = (if (isDark) 0.24f else 0.14f) * alpha),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            // Described by the row's own semantics, not announced twice.
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(LEADING_ICON_SIZE),
        )
    }
}

@Composable
private fun TrailingControl(control: SettingsRowControl, enabled: Boolean) {
    // Cleared from the semantics tree in every branch: the row already
    // carries the role, the label and the click action, so announcing
    // the control separately would read the same setting twice and give
    // TalkBack a second, smaller target for the same thing.
    val cleared = Modifier.clearAndSetSemantics { }
    when (control) {
        is SettingsRowControl.None -> Unit
        is SettingsRowControl.Navigate -> Icon(
            imageVector = IconTokens.chevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = cleared.size(TRAILING_ICON_SIZE),
        )
        is SettingsRowControl.Toggle -> Switch(
            checked = control.checked,
            onCheckedChange = control.onCheckedChange,
            enabled = enabled,
            modifier = cleared,
        )
        is SettingsRowControl.Selection -> RadioButton(
            selected = control.selected,
            onClick = null,
            enabled = enabled,
            modifier = cleared,
        )
        is SettingsRowControl.Value -> Text(
            text = control.value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = cleared,
        )
        // Deliberately NOT cleared: this is a separate target with its
        // own meaning and must stay independently focusable.
        is SettingsRowControl.Action -> AppTextButton(
            text = control.label,
            onClick = control.onClick,
            enabled = enabled,
        )
    }
}

/**
 * Merges the row's text into one announcement and gives it the role its
 * control implies, so TalkBack reads "Notify after scan, off when an
 * automatic scan finishes, switch, off" rather than three fragments and
 * an unlabelled switch.
 */
private fun Modifier.semanticsFor(
    title: String,
    supportingText: String?,
    control: SettingsRowControl,
): Modifier {
    // A row carrying an Action has two meanings — the row's own content
    // and a separate button — so merging it into one label would make
    // the button unreachable. Rows whose control IS the row keep the
    // merge, which is what stops TalkBack announcing an unlabelled
    // switch next to unrelated text.
    if (control is SettingsRowControl.Action) return this

    val label = listOfNotNull(title, supportingText).joinToString(". ")
    return this.clearAndSetSemantics {
        contentDescription = label
        when (control) {
            is SettingsRowControl.Toggle -> role = Role.Switch
            is SettingsRowControl.Selection -> role = Role.RadioButton
            is SettingsRowControl.Navigate -> role = Role.Button
            else -> Unit
        }
    }
}

/**
 * The closed set of things a settings row may end in.
 *
 * `Value` is read-only on purpose — it is how a hub row shows the
 * current state of a setting owned by another screen ("Scheduled scan —
 * Weekly") without duplicating that screen's control.
 */
sealed interface SettingsRowControl {

    data object None : SettingsRowControl

    /** Opens another screen. */
    data object Navigate : SettingsRowControl

    data class Toggle(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : SettingsRowControl

    /** One option within a single-choice list. The click is handled by
     *  the row, so the button itself is deliberately non-interactive. */
    data class Selection(val selected: Boolean) : SettingsRowControl

    data class Value(val value: String) : SettingsRowControl

    /**
     * A distinct action on the row, rather than the row itself being the
     * action — "Remove" on an ignore-list entry, for example.
     *
     * The only control that stays independently focusable: it is a
     * second target with its own meaning, so unlike a Switch or a
     * chevron it must not be merged into the row's label. See
     * `semanticsFor`, which deliberately skips merging for this case.
     */
    data class Action(val label: String, val onClick: () -> Unit) : SettingsRowControl
}

private val LEADING_BADGE_SIZE = 40.dp
private val LEADING_ICON_SIZE = 22.dp
private val TRAILING_ICON_SIZE = 20.dp
private const val DISABLED_ALPHA = 0.38f
