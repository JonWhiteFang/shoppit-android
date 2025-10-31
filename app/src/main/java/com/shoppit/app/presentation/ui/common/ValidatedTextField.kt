package com.shoppit.app.presentation.ui.common

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation

/**
 * A text field component with built-in validation error display.
 * 
 * Displays an error message below the field when validation fails,
 * and applies error styling to the field border.
 * 
 * Requirements: 4.1, 4.2, 4.3
 * 
 * @param value The current text value
 * @param onValueChange Callback when the text changes
 * @param label The label text for the field
 * @param error Optional error message to display. When non-null, the field shows error state
 * @param modifier Optional modifier for the text field
 * @param enabled Whether the field is enabled for input
 * @param readOnly Whether the field is read-only
 * @param singleLine Whether the field should be single line
 * @param maxLines Maximum number of lines for multi-line fields
 * @param minLines Minimum number of lines for multi-line fields
 * @param keyboardOptions Keyboard options for the field
 * @param keyboardActions Keyboard actions for the field
 * @param visualTransformation Visual transformation for the text (e.g., password masking)
 * @param placeholder Optional placeholder text
 * @param leadingIcon Optional leading icon composable
 * @param trailingIcon Optional trailing icon composable
 */
@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val hasError = error != null
    
    // Build content description for accessibility
    val fieldDescription = buildString {
        append(label)
        if (hasError) {
            append(". Error: $error")
        }
    }
    
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = hasError,
        supportingText = error?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics {
                        contentDescription = "Error: $it"
                    }
                )
            }
        },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        modifier = modifier.semantics {
            contentDescription = fieldDescription
        }
    )
}
