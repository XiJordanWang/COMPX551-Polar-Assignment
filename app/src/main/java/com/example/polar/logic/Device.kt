package com.example.polar.logic

// A Polar H10 device ID is 8 characters, 0-9 and A-F, e.g. "B5E1A12F".
// It is printed on the back of the sensor.
fun isValidDeviceId(id: String): Boolean {
    return id.length == 8 && id.all { it in '0'..'9' || it in 'A'..'F' }
}

// Clean up what the user typed: capital letters, no spaces, at most 8 characters
fun cleanDeviceIdInput(text: String): String {
    return text.uppercase().filter { it.isLetterOrDigit() }.take(8)
}
