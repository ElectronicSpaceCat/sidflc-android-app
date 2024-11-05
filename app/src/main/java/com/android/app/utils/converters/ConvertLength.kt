package com.android.app.utils.converters

object ConvertLength: IConvert<ConvertLength.Unit> {
    enum class Unit{
        MM,
        CM,
        M,
        IN,
        FT,
        YD
    }

    enum class Metric(val unit: Unit){
        MM(Unit.MM),
        CM(Unit.CM),
        M(Unit.M)
    }

    enum class Std(val unit: Unit){
        IN(Unit.IN),
        FT(Unit.FT),
        YD(Unit.YD)
    }

    override fun getUnit(unit: String): Unit {
        return Unit.valueOf(unit)
    }

    override fun convert(from: Unit, to: Unit, input: Double): Double {
        when(from){
            Unit.MM -> {
                return when(to){
                    Unit.MM -> { input }
                    Unit.CM -> { input * 0.1 }
                    Unit.M  -> { input * 0.001 }
                    Unit.IN -> { input * 0.03937 }
                    Unit.FT -> { input * 0.00328 }
                    Unit.YD -> { input * 0.001093 }
                }
            }
            Unit.CM -> {
                return when(to){
                    Unit.MM -> { input * 10.0 }
                    Unit.CM -> { input }
                    Unit.M  -> { input * 0.01 }
                    Unit.IN -> { input * 0.3937 }
                    Unit.FT -> { input * 0.03281 }
                    Unit.YD -> { input * 0.0109361 }
                }
            }
            Unit.M -> {
                return when(to){
                    Unit.IN -> { input * 39.37 }
                    Unit.FT -> { input * 3.28084 }
                    Unit.YD -> { input * 1.0936133 }
                    Unit.MM -> { input * 1000.0 }
                    Unit.CM -> { input * 100.0 }
                    Unit.M  -> { input }
                }
            }
            Unit.IN -> {
                return when(to){
                    Unit.MM -> { input * 25.4 }
                    Unit.CM -> { input * 2.54 }
                    Unit.M  -> { input * 0.0254 }
                    Unit.IN -> { input }
                    Unit.FT -> { input * 0.0833333 }
                    Unit.YD -> { input * 0.2777777 }
                }
            }
            Unit.FT -> {
                return when(to){
                    Unit.MM -> { input * 304.8 }
                    Unit.CM -> { input * 30.48 }
                    Unit.M  -> { input * 0.3048 }
                    Unit.IN -> { input * 12.0 }
                    Unit.FT -> { input }
                    Unit.YD -> { input * 0.33333 }
                }
            }
            Unit.YD -> {
                return when(to){
                    Unit.MM -> { input * 914.4 }
                    Unit.CM -> { input * 91.44 }
                    Unit.M  -> { input * 0.9144 }
                    Unit.IN -> { input * 36.0 }
                    Unit.FT -> { input * 3.0 }
                    Unit.YD -> { input }
                }
            }
        }
    }
}