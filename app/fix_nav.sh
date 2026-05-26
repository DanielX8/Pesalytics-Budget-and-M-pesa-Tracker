#!/bin/bash
sed -i 's/indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)/indicatorColor = com.example.ui.theme.AccentGreenLight.copy(alpha = 0.15f), selectedIconColor = com.example.ui.theme.AccentGreenDark, selectedTextColor = com.example.ui.theme.AccentGreenDark/g' app/src/main/java/com/example/MainActivity.kt
