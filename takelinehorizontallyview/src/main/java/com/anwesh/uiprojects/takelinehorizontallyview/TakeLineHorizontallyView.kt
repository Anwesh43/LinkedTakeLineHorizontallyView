package com.anwesh.uiprojects.takelinehorizontallyview

/**
 * Created by anweshmishra on 19/02/20.
 */

import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.content.Context
import android.app.Activity

val nodes : Int = 5
val lines : Int = 2
val scGap : Float = 0.02f
val strokeFactor : Int = 90
val sizeFactor : Float = 2.9f
val foreColor : Int = Color.parseColor("#3F51B5")
val backColor : Int = Color.parseColor("#BDBDBD")
val delay : Long = 20

fun Int.inverse() : Float = 1f / this
fun Float.maxScale(i : Int, n : Int) : Float = Math.max(0f, this - i * n.inverse())
fun Float.divideScale(i : Int, n : Int) : Float = Math.min(n.inverse(), maxScale(i, n)) * n
fun Float.sinify() : Float = Math.sin(this * Math.PI).toFloat()

fun Canvas.drawTakeLineHorizontally(i : Int, scale : Float, w : Float, paint : Paint) {
    val sf : Float = scale.sinify().divideScale(i, lines)
    var sc : Float = 0f
    if (scale > 0.5f) {
        sc = sf
    }
    val size : Float = w / sizeFactor
    val x : Float = (w / 2) * (1f - 2 * i) * sc
    save()
    drawLine(0f, 0f, (w / 2) * sf * (1f - 2 * i), 0f, paint)
    translate(x, 0f)
    drawLine(0f, -size, 0f, size, paint)
    restore()
}

fun Canvas.drawTakeLinesHorizontally(scale : Float, w : Float, paint : Paint) {
    for (j in 0..(lines - 1)) {
        drawTakeLineHorizontally(j, scale,  w, paint)
    }
}

fun Canvas.drawTLHNode(i : Int, scale : Float, paint : Paint) {
    val w : Float = width.toFloat()
    val h : Float = height.toFloat()
    val gap : Float = h / (nodes + 1)
    val size : Float = gap / sizeFactor
    paint.color = foreColor
    paint.strokeCap = Paint.Cap.ROUND
    paint.strokeWidth = Math.min(w, h) / strokeFactor
    save()
    translate(0f, gap * (i + 1))
    drawTakeLinesHorizontally(scale, w, paint)
    restore()
}

class TakeLineHorizontallyView(ctx : Context) : View(ctx) {

    override fun onDraw(canvas : Canvas) {

    }

    override fun onTouchEvent(event : MotionEvent) : Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

            }
        }
        return true
    }

    data class State(var scale : Float = 0f, var dir : Float = 0f, var prevScale : Float = 0f) {

        fun update(cb : (Float) -> Unit) {
            scale += scGap * dir
            if (Math.abs(scale - prevScale) > 1) {
                scale = prevScale + dir
                dir = 0f
                prevScale = scale
                cb(prevScale)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            if (dir == 0f) {
                dir = 1f - 2 * prevScale
                cb()
            }
        }
    }

    data class Animator(var view : View, var animated : Boolean = false) {

        fun animate(cb : () -> Unit) {
            if (animated) {
                cb()
                try {
                    Thread.sleep(delay)
                    view.invalidate()
                } catch(ex : Exception) {

                }
            }
        }

        fun start() {
            if (!animated) {
                animated = true
                view.postInvalidate()
            }
        }

        fun stop() {
            if (animated) {
                animated = false
            }
        }
    }

    data class TLHNode(var i : Int, val state : State = State()) {

        private var next : TLHNode? = null
        private var prev : TLHNode? = null

        init {

        }

        fun addNeighbor() {
            if (i < nodes - 1) {
                next = TLHNode(i + 1)
                next?.prev = this
            }
        }

        fun draw(canvas : Canvas, paint : Paint) {
            canvas.drawTLHNode(i, state.scale, paint)
            next?.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            state.update(cb)
        }

        fun startUpdating(cb : () -> Unit) {
            state.startUpdating(cb)
        }

        fun getNext(dir : Int, cb : () -> Unit) : TLHNode {
            var curr : TLHNode? = prev
            if (dir == 1) {
                curr = next
            }
            if (curr != null) {
                return curr
            }
            cb()
            return this
        }
    }

    data class TakeLineHorizontally(var i : Int) {

        private val root : TLHNode = TLHNode(0)
        private var curr : TLHNode = root
        private var dir : Int = 1

        fun draw(canvas : Canvas, paint : Paint) {
            root.draw(canvas, paint)
        }

        fun update(cb : (Float) -> Unit) {
            curr.update {
                curr = curr.getNext(dir) {
                    dir *= -1
                }
                cb(it)
            }
        }

        fun startUpdating(cb : () -> Unit) {
            curr.startUpdating(cb)
        }
    }

    data class Renderer(var view : TakeLineHorizontallyView) {

        private val animator : Animator = Animator(view)
        private val tlh : TakeLineHorizontally = TakeLineHorizontally(0)
        private val paint : Paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun render(canvas : Canvas) {
            canvas.drawColor(backColor)
            tlh.draw(canvas, paint)
            animator.animate {
                tlh.update {
                    animator.stop()
                }
            }
        }

        fun handleTap() {
            tlh.startUpdating {
                animator.start()
            }
        }
    }
}