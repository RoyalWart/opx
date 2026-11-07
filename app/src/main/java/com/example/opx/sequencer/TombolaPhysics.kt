package com.example.opx.sequencer

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class TombolaBall(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float = 14f,
    var midiNote: Int = 60,
    var colorHex: Long = 0xFF00A3E0
)

data class TombolaTriggerEvent(
    val ballId: Int,
    val midiNote: Int,
    val velocity: Float,
    val impactCutoffHz: Float
)

/**
 * OP-1 Tombola-style gravity and tumbler physics simulation.
 * Simulates virtual bouncing balls inside a rotating hexagonal drum.
 * Wall collisions generate organic musical note triggers with impact velocity.
 */
class TombolaPhysics(
    val drumRadius: Float = 110f
) {
    val balls = mutableListOf<TombolaBall>()

    var gravity: Float = 0.35f
    var restitution: Float = 0.88f // Bounciness elasticity
    var drumAngleRad: Float = 0.0f
    var drumRotationSpeed: Float = 0.025f // Tumbler rotation speed
    var isRunning: Boolean = false

    private val pentatonicScale = listOf(48, 51, 53, 55, 58, 60, 63, 65, 67, 70, 72)
    private val ballColors = listOf(0xFF00A3E0, 0xFFE5A100, 0xFF8899A6, 0xFFFF5500, 0xFF4EFA8C, 0xFFD4D8E0)

    init {
        // Initialize with 3 bouncing balls
        resetBalls(3)
    }

    fun resetBalls(count: Int) {
        balls.clear()
        val rng = Random(42)
        val num = count.coerceIn(1, 6)
        for (i in 0 until num) {
            val angle = (i * (2.0 * Math.PI / num)).toFloat()
            val dist = drumRadius * 0.45f
            balls.add(
                TombolaBall(
                    id = i,
                    x = dist * cos(angle),
                    y = dist * sin(angle),
                    vx = (rng.nextFloat() - 0.5f) * 4f,
                    vy = (rng.nextFloat() - 0.5f) * 4f,
                    radius = 12f,
                    midiNote = pentatonicScale[i % pentatonicScale.size],
                    colorHex = ballColors[i % ballColors.size]
                )
            )
        }
    }

    fun addBall() {
        if (balls.size < 6) {
            val id = balls.size
            balls.add(
                TombolaBall(
                    id = id,
                    x = 0f,
                    y = -drumRadius * 0.3f,
                    vx = (Random.nextFloat() - 0.5f) * 5f,
                    vy = 2f,
                    radius = 12f,
                    midiNote = pentatonicScale[id % pentatonicScale.size],
                    colorHex = ballColors[id % ballColors.size]
                )
            )
        }
    }

    fun removeBall() {
        if (balls.size > 1) {
            balls.removeAt(balls.size - 1)
        }
    }

    /**
     * Steps the physics simulation and returns any note trigger events caused by wall impacts.
     */
    fun update(): List<TombolaTriggerEvent> {
        if (!isRunning) return emptyList()

        val events = mutableListOf<TombolaTriggerEvent>()
        drumAngleRad += drumRotationSpeed

        for (ball in balls) {
            // Apply gravity
            ball.vy += gravity

            // Apply slight air damping
            ball.vx *= 0.998f
            ball.vy *= 0.998f

            // Update position
            ball.x += ball.vx
            ball.y += ball.vy

            // Boundary collision against circular tumbler wall
            val distFromCenter = hypot(ball.x, ball.y)
            val maxAllowedDist = drumRadius - ball.radius

            if (distFromCenter >= maxAllowedDist) {
                // Normal vector from center to ball
                val nx = ball.x / distFromCenter
                val ny = ball.y / distFromCenter

                // Reposition ball on surface boundary
                ball.x = nx * maxAllowedDist
                ball.y = ny * maxAllowedDist

                // Dot product of velocity and normal
                val dot = ball.vx * nx + ball.vy * ny

                if (dot > 0f) {
                    // Reflect velocity vector across collision normal
                    val impactSpeed = hypot(ball.vx, ball.vy)
                    ball.vx = (ball.vx - 2f * dot * nx) * restitution
                    ball.vy = (ball.vy - 2f * dot * ny) * restitution

                    // Add rotational velocity boost from spinning drum wall
                    val tangentialVx = -ny * drumRotationSpeed * drumRadius * 0.15f
                    val tangentialVy = nx * drumRotationSpeed * drumRadius * 0.15f
                    ball.vx += tangentialVx
                    ball.vy += tangentialVy

                    // Trigger note event if impact has sufficient kinetic force
                    if (impactSpeed > 0.8f) {
                        val velocityNorm = (impactSpeed / 12f).coerceIn(0.4f, 1.0f)
                        val cutoffExcitation = 800f + (velocityNorm * 8000f)
                        events.add(
                            TombolaTriggerEvent(
                                ballId = ball.id,
                                midiNote = ball.midiNote,
                                velocity = velocityNorm,
                                impactCutoffHz = cutoffExcitation
                            )
                        )
                    }
                }
            }
        }

        // Ball-to-ball collisions
        for (i in 0 until balls.size) {
            for (j in i + 1 until balls.size) {
                val b1 = balls[i]
                val b2 = balls[j]
                val dx = b2.x - b1.x
                val dy = b2.y - b1.y
                val dist = hypot(dx, dy)
                val minDist = b1.radius + b2.radius

                if (dist < minDist && dist > 0.001f) {
                    val nx = dx / dist
                    val ny = dy / dist
                    val overlap = 0.5f * (minDist - dist)
                    b1.x -= nx * overlap
                    b1.y -= ny * overlap
                    b2.x += nx * overlap
                    b2.y += ny * overlap

                    val kx = b1.vx - b2.vx
                    val ky = b1.vy - b2.vy
                    val p = 2f * (nx * kx + ny * ky) / 2f
                    b1.vx -= p * nx
                    b1.vy -= p * ny
                    b2.vx += p * nx
                    b2.vy += p * ny
                }
            }
        }

        return events
    }
}
