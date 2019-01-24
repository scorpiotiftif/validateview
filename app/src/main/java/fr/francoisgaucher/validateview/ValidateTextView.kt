package fr.francoisgaucher.validateview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.TextView

class ValidateTextView : TextView, ValidateTextViewContrat {


    private val paintTextView = Paint(Paint.ANTI_ALIAS_FLAG)
    private var ratio: Float = 0f
    private var radius: Int = 0

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )

    init {
        paintTextView.color = Color.WHITE
        paintTextView.style = Paint.Style.FILL
        paintTextView.xfermode = PorterDuffXfermode(PorterDuff.Mode.XOR)
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        setWillNotDraw(false)
    }

    fun updateRadius(radius: Int){
        this.radius = radius
    }

    override fun changeRatio(ratio: Float) {
        this.ratio = if (ratio > ValidateView.LIMIT_RATIO_TO_BEGIN_DRAW_VALIDATE_CIRCLE) {
            MIN_RATIO
        } else if (ratio < MIN_RATIO) {
            MAX_RATIO
        } else {
            ValidateView.LIMIT_RATIO_TO_BEGIN_DRAW_VALIDATE_CIRCLE.minus(ratio)
                .times(MAX_RATIO.div(ValidateView.LIMIT_RATIO_TO_BEGIN_DRAW_VALIDATE_CIRCLE))
        }
        invalidate()
    }

    override fun resetCircle() {
        this.ratio = MIN_RATIO
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (ratio > MIN_RATIO) {
            canvas?.let {
                it.drawCircle(0f + (width.div(2)), 0f + (height.div(2)), radius * ratio, paintTextView)
            }
        }
    }

    companion object {
        private const val MIN_RATIO = 0f
        private const val MAX_RATIO = 1f
    }
}

public interface ValidateTextViewContrat {
    fun changeRatio(ratio: Float)
    fun resetCircle()
}