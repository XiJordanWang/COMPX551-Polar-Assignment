package com.example.polar.data.polar

/** Holds one PolarManager that the whole app can share, so there's only one connection to the H10 at a time. */
object SharedPolarManager {

    var polarManager: PolarManager? = null
}