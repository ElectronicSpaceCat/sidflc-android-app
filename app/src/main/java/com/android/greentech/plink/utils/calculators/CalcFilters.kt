package com.android.greentech.plink.utils.calculators

object CalcFilters {
    /**
     * Moving average
     *
     * @param windowSize (sets the sample size)
     */
    class MovingAverage(private val windowSize: Int) {
        private val _samples: DoubleArray = DoubleArray(windowSize)
        private var _index = 0
        private var _sum = 0.0
        private var _average = 0.0

        /**
         * Returns averaged input
         *
         * @param value
         */
        fun getAverage(value: Double): Double {
            _sum -= _samples[_index]
            _samples[_index] = value
            _sum += value
            _index = (_index + 1) % windowSize
            _average = _sum / windowSize.toFloat()
            return _average
        }

        /**
         * Reset the moving average filter
         */
        fun reset(init : Double = 0.0) {
            for (i in _samples.indices) {
                _samples[i] = init
            }
            _average = init
            _index = 0
            _sum = init * windowSize
        }
    }

    /**
     * Kalman filter
     *
     * Source: https://github.com/christianb/Kalman-Filter/blob/master/KalmanFilter.kt
     *
     * @param R Models the process noise and describes how noisy a system internally is.
     *          How much noise can be expected from the system itself?
     *          When a system is constant R can be set to a (very) low value.
     *
     * @param Q Resembles the measurement noise.
     *          How much noise is caused by the measurements?
     *          When it's expected that the measurements will contain most of the noise,
     *          it makes sense to set this parameter to a high number (especially in comparison to the process noise).
     *
     *          Usually you make an estimate of R and Q based on measurements or domain knowledge.
     *
     * @param A State vector
     * @param B Control vector
     * @param C Measurement vector
     */
    class KalmanFilter(private val R: Double,
                       private val Q: Double,
                       private val A: Double = 1.0,
                       private val B: Double = 0.0,
                       private val C: Double = 1.0) {

        private var x: Double? = null // estimated signal without noise
        private var cov: Double = 0.0

        private fun square(x: Double) = x * x

        // Predict next value
        private fun predict(x: Double, u: Double): Double = (A * x) + (B * u)

        // Uncertainty of filter
        private fun uncertainty(): Double = (square(A) * cov) + R

        fun filter(signal: Double, u: Double = 0.0): Double {
            val x: Double? = this.x

            if (x == null) {
                this.x = (1 / C) * signal
                cov = square(1 / C) * Q
            } else {
                val prediction: Double = predict(x, u)
                val uncertainty: Double = uncertainty()

                // Kalman gain
                val kGain: Double = uncertainty * C * (1 / ((square(C) * uncertainty) + Q))

                // Correction
                this.x = prediction + kGain * (signal - (C * prediction))
                cov = uncertainty - (kGain * C * uncertainty)
            }

            return this.x!!
        }
    }
}