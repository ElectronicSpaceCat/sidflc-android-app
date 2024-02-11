package com.android.app.utils.converters

object ConvertWeight: IConvert<ConvertWeight.Unit> {
    enum class Unit{
        MG,
        CG,
        G,
        OZ
    }
    
    enum class Metric(val unit: Unit){
        MG(Unit.MG),
        CG(Unit.CG),
        G(Unit.G)
    }

    enum class Std(val unit: Unit){
        OZ(Unit.OZ)
    }

    override fun getUnit(unit: String): Unit {
        return Unit.valueOf(unit)
    }

    override fun convert(from: Unit, to: Unit, input: Double): Double {
        when(from){
            Unit.MG -> {
                return when(to){
                    Unit.MG ->{ input }
                    Unit.CG ->{ input / 10.0 }
                    Unit.G ->{ input / 1000.0 }
                    Unit.OZ ->{ input * 0.00003527 }
                }
            }
            Unit.CG -> {
                return when(to){
                    Unit.MG ->{ input * 10.0 }
                    Unit.CG ->{ input }
                    Unit.G ->{ input / 100.0 }
                    Unit.OZ ->{ input / 0.00035274 }
                }
            }
            Unit.G -> {
                return when(to){
                    Unit.MG ->{ input * 1000.0 }
                    Unit.CG ->{ input * 100.0 }
                    Unit.G ->{ input }
                    Unit.OZ ->{ input * 0.03527396 }
                }
            }
            Unit.OZ -> {
                return when(to){
                    Unit.MG ->{ input * 28349.5231 }
                    Unit.CG ->{ input * 2834.95231 }
                    Unit.G ->{ input * 28.3495231 }
                    Unit.OZ ->{ input }
                }
            }
        }
    }
}
