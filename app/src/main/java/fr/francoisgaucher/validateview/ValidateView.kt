package fr.francoisgaucher.validateview

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import fr.francoisgaucher.validateview.extensions.waitForLayout


class ValidateView : ConstraintLayout {

    private val validateView: View
    private lateinit var koView: ValidateTextView
    private lateinit var okView: ValidateTextView

    private var validateViewContrat: ValidateViewContrat? = null

    // ####################################################
    // ##################### PAINT ########################
    // ####################################################
    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintDEBUG: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintAnimate: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ####################################################
    // ###################### RECT ########################
    // ####################################################
    private val initialRect: RectF = RectF()
    private val movingRect: RectF = RectF()

    private val koRect: Rect = Rect()
    private val okrect: Rect = Rect()

    // ####################################################
    // #################### RADIUS ########################
    // ####################################################
    /**
     * Initial Circle radius in pixel
     */
    private var initialPixelRadius = 0

    /**
     * Moving Circle radius in pixel
     */
    private var movingPixelRadius = 0

    /**
     * Moving Circle radius in pixel
     */
    private var movingAnimatePixelRadius = 0

    /**
     * MAX Circle radius in pixel
     */
    private var maxPixelRadius = 0

    // ####################################################
    // #################### THREAD ########################
    // ####################################################
    /**
     * Thread that start animation to validate the action
     */
    private var threadStartAnimationToValidate: Thread? = null

    /**
     * Thread that start the initiate animation
     */
    private var threadStartInitiateAnimation: Thread? = null

    private var handlerAnimation: Handler? = null
    private var handlerAnimationInitiate: Handler? = null

    private var runnableAnimation: Runnable? = null

    // ####################################################
    // ###################### VARS ########################
    // ####################################################
    /**
     * Direction about circle when is it animate
     */
    private var directionMovingCircle: DirectionEnum = DirectionEnum.NO
        set(value) {
            field = value
            when (value) {
                DirectionEnum.LEFT -> {
                    calculateLimitLeftPixelToDrawCircle()
                }
                DirectionEnum.RIGHT -> {
                    calculateLimitRightPixelToDrawCircle()
                }
                else -> {
                }
            }
        }

    /**
     * State about circle (initial position, moving, etc)
     */
    private var stateCircle: StateEnum = StateEnum.INITIAL_POSITION
        set(value) {
            field = value
            if (field == StateEnum.INITIAL_POSITION) {
                movingAnimatePixelRadius = initialPixelRadius
                movingPixelRadius = initialPixelRadius
                movingRect.set(initialRect)
            }
        }

    /**
     * pixel limit to draw circle
     */
    private var limitXToDrawCircleInPixel = 0

    private var lastRatioSaved = 0f

    // ################################################
    // ################################################
    // ################### DEBUG ######################
    private val annule = "ANNULER"
    private val ok = "OK"

    private val labelRectKoRect: RectF = RectF()
    private val labelRectOkRect: RectF = RectF()
    // ################################################
    // ################################################

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    )

    init {
        validateView = LayoutInflater.from(context).inflate(R.layout.validate_view_layout, this, true)

        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL

        paintAnimate.color = Color.WHITE
        paintAnimate.alpha = ANIMATE_MAX_ALPHA
        paintAnimate.style = Paint.Style.FILL

        paintText.color = Color.WHITE
        paintText.style = Paint.Style.FILL
        paintText.textSize = 40f

        paintDEBUG.color = Color.MAGENTA
        paintDEBUG.alpha = 65
        paintDEBUG.style = Paint.Style.FILL

        setWillNotDraw(false)

        handlerAnimationInitiate = Handler {
            if (it.what == 1) {
                threadStartInitiateAnimation?.interrupt()
                threadStartInitiateAnimation = null
                threadStartInitiateAnimation = Thread(Runnable {
                    try {
                        Log.e("FRANCOIS", Thread.currentThread().name + " STARTED !")

                        while (Thread.interrupted().not() && StateEnum.INITIAL_POSITION == stateCircle) {

                            Thread.sleep(ANIMATE_THREAD_TIME_TO_SLEEP)
                            val message = handlerAnimationInitiate?.obtainMessage()
                            message?.let {
                                it.data = Bundle()
                                it.data.putBoolean(MESSAGE_START_INITIATE_ANIMATION, true)
                                handlerAnimationInitiate?.sendMessage(it)
                            }
                        }
                    } catch (e: InterruptedException) {
                        Log.e("FRANCOIS", Thread.currentThread().name + " STOPPED !")
                    }

                })
                threadStartInitiateAnimation?.start()
            } else {
                val bundle = it?.data
                bundle?.let {
                    val start = it.getBoolean(MESSAGE_START_INITIATE_ANIMATION, false)
                    if (start && StateEnum.INITIAL_POSITION == stateCircle) {
                        animateCircle()
                    }
                }
            }
            true
        }

        postDelayed({
            if (threadStartInitiateAnimation == null) {
                threadStartInitiateAnimation = Thread(Runnable {
                    try {
                        Log.e("FRANCOIS", Thread.currentThread().name + " STARTED !")
                        while (Thread.interrupted().not() && StateEnum.INITIAL_POSITION == stateCircle) {

                            Thread.sleep(ANIMATE_THREAD_TIME_TO_SLEEP)
                            val message = handlerAnimationInitiate?.obtainMessage()
                            message?.let {
                                it.data = Bundle()
                                it.data.putBoolean(MESSAGE_START_INITIATE_ANIMATION, true)
                                handlerAnimationInitiate?.sendMessage(it)
                            }
                        }
                    } catch (e: InterruptedException) {
                        Log.e("FRANCOIS", Thread.currentThread().name + " STOPPED !")
                    }

                })
            }
            threadStartInitiateAnimation?.start()
        }, 300)
    }

    // ################################################################
    // ######################### ACTIONS ##############################
    // ################################################################
    private fun updateMeasure() {
        val maxWidth = measuredWidth.toFloat()
        val maxHeight = measuredHeight.toFloat()
        initialPixelRadius =
                if (maxWidth > maxHeight) maxHeight.div(2).times(0.7).toInt() else maxWidth.div(2).times(0.7).toInt()

        maxPixelRadius = initialPixelRadius.plus(initialPixelRadius.times(0.3)).toInt()

        okView.updateRadius(initialPixelRadius)
        koView.updateRadius(initialPixelRadius)

        labelRectKoRect.set(3f, 3f, paintText.measureText(annule).times(2), maxHeight-3)
        labelRectOkRect.set(maxWidth - paintText.measureText(annule).times(2), 3f, maxWidth - 3, maxHeight-3)

        this.waitForLayout {
            initialRect.left = maxWidth.div(2) - initialPixelRadius.div(2)
            initialRect.right = maxWidth.div(2) + initialPixelRadius.div(2)
            initialRect.top = maxHeight.div(2) - initialPixelRadius.div(2)
            initialRect.bottom = maxHeight.div(2) + initialPixelRadius.div(2)
        }

        var location = IntArray(2)
        var x = location[0]
        var y = location[1]

        // POSITION ABOUT OK VIEW
        okView.waitForLayout {
            val isLandScape = resources.getBoolean(R.bool.is_landscape)

            val dim = if (isLandScape) {
                val resources = context.resources
                val resourceId = resources.getIdentifier("navigation_bar_width", "dimen", "android")
                if (resourceId > 0) {
                    resources.getDimensionPixelSize(resourceId)
                } else 0
            } else {
                0
            }

            okView.getLocationOnScreen(location)
            x = location[0]
            y = location[1]

            okrect.set(
                x.minus(okView.width.div(2)).minus(dim),
                initialRect.top.toInt(),
                x.plus(okView.width.div(2)).minus(dim),
                initialRect.bottom.toInt()
            )
        }

        // POSITION ABOUT KO VIEW
        koView.waitForLayout {
            val isLandScape = resources.getBoolean(R.bool.is_landscape)

            val dim = if (isLandScape) {
                val resources = context.resources
                val resourceId = resources.getIdentifier("navigation_bar_width", "dimen", "android")
                if (resourceId > 0) {
                    resources.getDimensionPixelSize(resourceId)
                } else 0
            } else {
                0
            }

            koView.getLocationOnScreen(location)
            x = location[0]
            y = location[1]

            koRect.set(
                x.minus(koView.width.div(2)).minus(dim),
                initialRect.top.toInt(),
                x.plus(koView.width.div(2)).minus(dim),
                initialRect.bottom.toInt()
            )
        }
    }

    /**
     * calcul the limit pixel to drawn the circle
     *
     */
    private fun calculateLimitRightPixelToDrawCircle() {
        val distanceToAdd = okrect.centerX().minus(initialRect.centerX())
            .times(DISTANCE_MAX_TO_DRAW_CIRCLE_IN_PERCENT)

        limitXToDrawCircleInPixel = initialRect.centerX().plus(distanceToAdd).toInt()
    }

    /**
     * calcul the limit pixel to drawn the circle
     *
     */
    private fun calculateLimitLeftPixelToDrawCircle() {
        val distanceToAdd = initialRect.centerX().minus(koRect.centerX())
            .times(DISTANCE_MAX_TO_DRAW_CIRCLE_IN_PERCENT)

        limitXToDrawCircleInPixel = initialRect.centerX().minus(distanceToAdd).toInt()
    }

    private fun movingCircle(newCircleXPosition: Float) {
        // ######################################################
        // There are 4 step to drawing the circle
        // See this scheme:
        //
        // LEGENDS =    V -> View (NO/YES)
        //              X -> NEW CIRCLE X POSITION
        //              L -> LIMIT TO DRAW CIRCLE BEFORE DISAPEAR
        //              O -> CENTER CIRCLE
        //
        // --V---X--L--------O--------L------V--
        // --V------L----X---O--------L------V--
        // --V------L--------O----X---L------V--
        // --V------L--------O--------L--X---V--
        // ######################################################

        // WE HIDE THE CIRCLE
        // IF WE ARE ON THE TOTAL RIGHT POSITION
        if (newCircleXPosition >= limitXToDrawCircleInPixel
            && newCircleXPosition > initialRect.centerX()
        ) {
            finishAnimation(newCircleXPosition)
        }
        // WE HIDE THE CIRCLE
        // IF WE ARE ON THE TOTAL LEFT POSITION
        else if (newCircleXPosition <= limitXToDrawCircleInPixel
            && newCircleXPosition < initialRect.centerX()
        ) {
            finishAnimation(newCircleXPosition)
        }
        // WE GROWING UP THE RADIUS CIRCLE
        else if (newCircleXPosition > limitXToDrawCircleInPixel
            && newCircleXPosition < initialRect.centerX()
        ) {
            lastRatioSaved = newCircleXPosition
                .minus(limitXToDrawCircleInPixel)
                .div(
                    initialRect.centerX()
                        .minus(limitXToDrawCircleInPixel)
                )

            if (lastRatioSaved < LIMIT_RATIO_TO_BEGIN_DRAW_VALIDATE_CIRCLE) {
                changeValidateViewCircle(lastRatioSaved)
            }

            movingPixelRadius = initialPixelRadius.times(lastRatioSaved).toInt()

            movingRect.set(
                newCircleXPosition.minus(movingPixelRadius)
                , initialRect.centerY().minus(movingPixelRadius)
                , newCircleXPosition.plus(movingPixelRadius)
                , initialRect.centerY().plus(movingPixelRadius)
            )
        }
        // WE DICREASE DOWN THE RADIUS CIRCLE
        else {
            lastRatioSaved = 1.minus(
                newCircleXPosition
                    .minus(initialRect.centerX())
                    .div(
                        limitXToDrawCircleInPixel
                            .minus(initialRect.centerX())
                    )
            )

            if (lastRatioSaved < LIMIT_RATIO_TO_BEGIN_DRAW_VALIDATE_CIRCLE) {
                changeValidateViewCircle(lastRatioSaved)
            }

            movingPixelRadius = initialPixelRadius.times(lastRatioSaved).toInt()

            movingRect.set(
                newCircleXPosition.minus(movingPixelRadius)
                , initialRect.centerY().minus(movingPixelRadius)
                , newCircleXPosition.plus(movingPixelRadius)
                , initialRect.centerY().plus(movingPixelRadius)
            )
        }
    }

    private fun initializeMovingCircle() {
        movingPixelRadius = initialPixelRadius
        movingRect.set(initialRect)
        lastRatioSaved = 1f
    }

    private fun finishAnimation(xPosition: Float) {
        movingPixelRadius = 0
        movingRect.set(xPosition, initialRect.centerY(), xPosition, initialRect.centerY())
        lastRatioSaved = 0f
        changeValidateViewCircle(lastRatioSaved)
        stateCircle = if (directionMovingCircle == DirectionEnum.LEFT) {
            validateViewContrat?.validateViewCancelSelected()
            StateEnum.LEFT_FINISHED
        } else {
            validateViewContrat?.validateViewOkSelected()
            StateEnum.RIGHT_FINISHED
        }
    }

    private fun changeValidateViewCircle(ratio: Float) {
        when (directionMovingCircle) {
            DirectionEnum.RIGHT -> {
                okView.changeRatio(ratio)
                koView.resetCircle()
            }
            DirectionEnum.LEFT -> {
                koView.changeRatio(ratio)
                okView.resetCircle()
            }
            else -> {
                okView.resetCircle()
                koView.resetCircle()
            }
        }
    }

    private fun startAnimationToValidateAction() {
        if (runnableAnimation == null || handlerAnimation == null) {
            initHandlerAndRunnable()
        }
        stateCircle = StateEnum.AUTOMATIC_MOVING
        threadStartAnimationToValidate = Thread(runnableAnimation)
        threadStartAnimationToValidate?.start()
    }

    private fun initHandlerAndRunnable() {
        runnableAnimation = Runnable {
            while (Thread.interrupted().not() && stateCircle == StateEnum.AUTOMATIC_MOVING) {
                Thread.sleep(THREAD_TIME_TO_SLEEP)
                val message = handlerAnimation?.obtainMessage()
                message?.let {
                    it.data = Bundle()
                    it.data.putInt(MESSAGE_START_ANIMATION, AnimationEnum.VALIDATE_ANIMATION.ordinal)
                    handlerAnimation?.sendMessage(it)
                }
            }
        }

        handlerAnimation = Handler(fun(it: Message?): Boolean {
            val bundle = it?.data
            bundle?.let {
                val animation = it.getInt(MESSAGE_START_ANIMATION, -1)
                return when (animation) {
                    AnimationEnum.VALIDATE_ANIMATION.ordinal -> {
                        continueMoving()
                        true
                    }
                    else -> {

                        false
                    }
                }
            }
            return false
        })
    }

    private fun continueMoving() {
        if (directionMovingCircle == DirectionEnum.LEFT) {
            movingCircle(movingRect.centerX() - MOVING_CIRCLE_PIXEL)
        } else {
            movingCircle(movingRect.centerX() + MOVING_CIRCLE_PIXEL)
        }
        invalidate()
    }

    private fun animateCircle() {
        if (StateEnum.INITIAL_POSITION == stateCircle) {
            movingAnimatePixelRadius += ANIMATE_CIRCLE_VALUE_ADDED
            paintAnimate.alpha = paintAnimate.alpha -
                    ANIMATE_MAX_ALPHA.div(((maxPixelRadius.minus(initialPixelRadius)).div(ANIMATE_CIRCLE_VALUE_ADDED)))
            if (paintAnimate.alpha < 0 || paintAnimate.alpha > ANIMATE_MAX_ALPHA) {
                paintAnimate.alpha = 0
            }
            if (movingAnimatePixelRadius > maxPixelRadius) {
                movingAnimatePixelRadius = initialPixelRadius
                paintAnimate.alpha = ANIMATE_MAX_ALPHA
            }
        }
        invalidate()
    }

    // ################################################################
    // ####################### OVERRIDES ##############################
    // ################################################################

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || (StateEnum.LEFT_FINISHED == stateCircle || StateEnum.RIGHT_FINISHED == stateCircle)) {
            return super.onTouchEvent(event)
        }
        if (StateEnum.AUTOMATIC_MOVING != stateCircle) {
            threadStartInitiateAnimation?.interrupt()
            movingAnimatePixelRadius = initialPixelRadius
            return when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val isInside = isInsideCircle(initialPixelRadius.toFloat(), event.x, event.y, initialRect)
                    stateCircle = if (isInside) StateEnum.MOVING
                    else {
                        directionMovingCircle = DirectionEnum.NO
                        StateEnum.INITIAL_POSITION
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (lastRatioSaved < LIMIT_RATIO_TO_BEGIN_DRAW_VALIDATE_CIRCLE) {
                        startAnimationToValidateAction()
                    } else {
                        initializeMovingCircle()
                        stateCircle = StateEnum.INITIAL_POSITION
                        directionMovingCircle = DirectionEnum.NO
                        okView.resetCircle()
                        koView.resetCircle()
                        handlerAnimationInitiate?.sendEmptyMessage(1)
                    }
                    invalidate()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    return if (StateEnum.MOVING == stateCircle) {
                        val tmp = if (event.x < initialRect.centerX()) {
                            DirectionEnum.LEFT
                        } else {
                            DirectionEnum.RIGHT
                        }
                        if (tmp != directionMovingCircle) {
                            directionMovingCircle = tmp
                        }
                        if(DirectionEnum.NO != directionMovingCircle){
                            movingCircle(event.x)
                            invalidate()
                        }
                        true
                    } else {
                        false
                    }
                }
                else -> {
                    false
                }
            }
        }
        return false
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        koView = validateView.findViewById(R.id.validate_view_no_text)
        okView = validateView.findViewById(R.id.validate_view_yes_text)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        updateMeasure()
    }

    override fun onDraw(canvas: Canvas?) {

        when (stateCircle) {
            StateEnum.INITIAL_POSITION -> {
                canvas?.let { finalCanvas ->
                    finalCanvas.drawCircle(
                        initialRect.centerX(),
                        initialRect.centerY(),
                        movingAnimatePixelRadius.toFloat(),
                        paintAnimate
                    )
                    finalCanvas.drawCircle(
                        initialRect.centerX(),
                        initialRect.centerY(),
                        initialPixelRadius.toFloat(),
                        paint
                    )
                }
            }
            StateEnum.MOVING -> {
                canvas?.let { finalCanvas ->
                    finalCanvas.drawCircle(
                        movingRect.centerX(),
                        movingRect.centerY(),
                        movingPixelRadius.toFloat(),
                        paint
                    )
                }
            }
            StateEnum.AUTOMATIC_MOVING -> {
                canvas?.let { finalCanvas ->
                    finalCanvas.drawCircle(
                        movingRect.centerX(),
                        movingRect.centerY(),
                        movingPixelRadius.toFloat(),
                        paint
                    )
                }
            }
            else -> {
            }
        }
        canvas?.let {

            it.drawRect(labelRectKoRect, paintDEBUG)
            it.drawRect(labelRectOkRect, paintDEBUG)
            it.drawText(annule, labelRectKoRect.centerX() - paintText.measureText(annule).div(2), labelRectKoRect.centerY(), paintText)
            it.drawText(ok, labelRectOkRect.centerX() - paintText.measureText(ok).div(2), labelRectOkRect.centerY(), paintText)

        }
        super.onDraw(canvas)
    }

    // #######################################################################
    // ######################### LISTENERS ###################################
    // #######################################################################
    fun subscribeListener(validateViewContrat: ValidateViewContrat) {
        this.validateViewContrat = validateViewContrat
    }

    fun reset() {
        stateCircle = StateEnum.INITIAL_POSITION
        okView.resetCircle()
        koView.resetCircle()
        handlerAnimationInitiate?.sendEmptyMessage(1)
        invalidate()
    }

    companion object {
        /**
         * Distance from center view and the no/yes center view in percent
         * where the circle is displayed, after this percent value the circle is not drawned anymore
         * AND
         * For animation, if the user release his finger, if the release point is less than that value, so
         * the animation start to posisionate the circle at his initial center
         * otherwise, the endding animation is started and listener fired ! (NO/YES CLICKED)
         */
        private const val DISTANCE_MAX_TO_DRAW_CIRCLE_IN_PERCENT = 0.90f

        /**
         * When the circle moving, a ratio is calculate to determine his radius.
         * when this ratio is less than XXX (BELLOW) so we begin to growin up the circle validation associate at the view
         * EX: NO or YES View
         */
        const val LIMIT_RATIO_TO_BEGIN_DRAW_VALIDATE_CIRCLE = 0.4f

        /**
         * keys that receive the animation handler to start validate animation or rollback animation
         */
        private const val MESSAGE_START_ANIMATION = "MESSAGE_START_ANIMATION"
        private const val MESSAGE_START_INITIATE_ANIMATION = "MESSAGE_START_INITIATE_ANIMATION"

        private const val THREAD_TIME_TO_SLEEP = 20L

        private const val MOVING_CIRCLE_PIXEL = 15

        private const val ANIMATE_THREAD_TIME_TO_SLEEP = 20L
        private const val ANIMATE_CIRCLE_VALUE_ADDED = 1
        private const val ANIMATE_MAX_ALPHA = 120
        private const val DISTANCE_FROM_INITIAL_CIRCLE = 4

        /**
         * This method converts dp unit to equivalent pixels, depending on device density.
         *
         * @param dp      A value in dp (density independent pixels) unit. Which we need to convert into pixels
         * @param context Context to get resources and device specific display metrics
         * @return A float value to represent px equivalent to dp depending on device density
         */
        fun convertDpToPx(context: Context, dp: Int): Int {
            return Math.round(dp * context.resources.displayMetrics.density)
        }

        /**
         *
         */
        private fun isInsideCircle(radius: Float, x: Float, y: Float, rectCircle: RectF): Boolean {
            // Calculate de la distance entre le centre du cercle et le point qui a ete toucher par l'utilisateur
            // la formule mathematique utilisée est le theoreme de pythagore
            // RACINE_CARRE(CARRE(x1-x2)+CARRE(y1-y2))
            val polarradius =
                Math.sqrt(
                    Math.pow(
                        (x - rectCircle.centerX()).toDouble(),
                        2.0
                    ) + Math.pow((y - rectCircle.centerY()).toDouble(), 2.0)
                )

            return polarradius <= radius
        }
    }

    interface ValidateViewContrat {
        fun validateViewOkSelected()
        fun validateViewCancelSelected()
    }
}

/**
 * Different states, whose values allow to know if the circle is drawing at his center or moving, or if it arrived
 * to final position
 */
private enum class StateEnum {
    INITIAL_POSITION,
    MOVING,
    AUTOMATIC_MOVING,
    LEFT_FINISHED,
    RIGHT_FINISHED
}

/**
 * That enum allow to know into witch direction the circle is moving
 */
private enum class DirectionEnum {
    NO,
    LEFT,
    RIGHT
}

/**
 * That enum allow to know if we have to stat validate animation or rollback
 */
private enum class AnimationEnum {
    VALIDATE_ANIMATION,
    ROLLBACK_ANIMATION,
}