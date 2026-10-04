package com.moasseum.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.moasseum.app.ui.theme.LocalFinanceColors

/** Consistent, restrained input surfaces; typography and field behavior stay unchanged. */
@Composable
fun FinanceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = RoundedCornerShape(14.dp),
) {
    val colors = LocalFinanceColors.current
    OutlinedTextField(
        value, onValueChange, modifier, enabled = enabled, readOnly = readOnly,
        textStyle = MaterialTheme.typography.bodyLarge,
        label = label, placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon,
        suffix = suffix, supportingText = supportingText, isError = isError,
        keyboardOptions = keyboardOptions, singleLine = singleLine, maxLines = maxLines, minLines = minLines,
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceInput,
            unfocusedContainerColor = colors.surfaceInput,
            errorContainerColor = colors.surfaceInput,
            unfocusedBorderColor = colors.divider.copy(alpha = 0.7f),
        ),
    )
}
