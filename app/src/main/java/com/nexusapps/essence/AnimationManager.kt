package com.nexusapps.essence

import android.content.Context
import android.content.SharedPreferences
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.view.animation.AlphaAnimation
import android.view.animation.ScaleAnimation

/**
 * AnimationManager handles animation customization and settings
 */
class AnimationManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("animation_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val ANIMATION_SPEED_KEY = "animation_speed"
        private const val ANIMATION_STYLE_KEY = "animation_style"
        private const val PAGE_TRANSITION_KEY = "page_transition"
        private const val ICON_ANIMATION_KEY = "icon_animation"
        private const val LAUNCH_ANIMATION_KEY = "launch_animation"
        
        val DEFAULT_SPEED = AnimationSpeed.NORMAL
        val DEFAULT_STYLE = AnimationStyle.SMOOTH
        val DEFAULT_PAGE_TRANSITION = PageTransition.SLIDE
        val DEFAULT_ICON_ANIMATION = IconAnimation.SCALE
        val DEFAULT_LAUNCH_ANIMATION = LaunchAnimation.FADE
    }
    
    enum class AnimationSpeed(val id: String, val displayName: String, val multiplier: Float) {
        SLOW("slow", "Slow", 2.0f),
        NORMAL("normal", "Normal", 1.0f),
        FAST("fast", "Fast", 0.5f),
        INSTANT("instant", "Instant", 0.1f);
        
        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: DEFAULT_SPEED
        }
    }
    
    enum class AnimationStyle(val id: String, val displayName: String) {
        SMOOTH("smooth", "Smooth"),
        BOUNCE("bounce", "Bounce"),
        ELASTIC("elastic", "Elastic"),
        LINEAR("linear", "Linear");
        
        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: DEFAULT_STYLE
        }
    }
    
    enum class PageTransition(val id: String, val displayName: String) {
        SLIDE("slide", "Slide"),
        FADE("fade", "Fade"),
        SCALE("scale", "Scale"),
        NONE("none", "None");
        
        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: DEFAULT_PAGE_TRANSITION
        }
    }
    
    enum class IconAnimation(val id: String, val displayName: String) {
        SCALE("scale", "Scale"),
        ROTATE("rotate", "Rotate"),
        BOUNCE("bounce", "Bounce"),
        NONE("none", "None");
        
        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: DEFAULT_ICON_ANIMATION
        }
    }
    
    enum class LaunchAnimation(val id: String, val displayName: String) {
        FADE("fade", "Fade"),
        SCALE("scale", "Scale"),
        SLIDE("slide", "Slide"),
        NONE("none", "None");
        
        companion object {
            fun fromId(id: String) = values().firstOrNull { it.id == id } ?: DEFAULT_LAUNCH_ANIMATION
        }
    }
    
    fun setAnimationSpeed(speed: AnimationSpeed) {
        prefs.edit().putString(ANIMATION_SPEED_KEY, speed.id).apply()
    }
    
    fun getCurrentAnimationSpeed(): AnimationSpeed {
        val speedId = prefs.getString(ANIMATION_SPEED_KEY, DEFAULT_SPEED.id) ?: DEFAULT_SPEED.id
        return AnimationSpeed.fromId(speedId)
    }
    
    fun setAnimationStyle(style: AnimationStyle) {
        prefs.edit().putString(ANIMATION_STYLE_KEY, style.id).apply()
    }
    
    fun getCurrentAnimationStyle(): AnimationStyle {
        val styleId = prefs.getString(ANIMATION_STYLE_KEY, DEFAULT_STYLE.id) ?: DEFAULT_STYLE.id
        return AnimationStyle.fromId(styleId)
    }
    
    fun setPageTransition(transition: PageTransition) {
        prefs.edit().putString(PAGE_TRANSITION_KEY, transition.id).apply()
    }
    
    fun getCurrentPageTransition(): PageTransition {
        val transitionId = prefs.getString(PAGE_TRANSITION_KEY, DEFAULT_PAGE_TRANSITION.id) ?: DEFAULT_PAGE_TRANSITION.id
        return PageTransition.fromId(transitionId)
    }
    
    fun setIconAnimation(animation: IconAnimation) {
        prefs.edit().putString(ICON_ANIMATION_KEY, animation.id).apply()
    }
    
    fun getCurrentIconAnimation(): IconAnimation {
        val animationId = prefs.getString(ICON_ANIMATION_KEY, DEFAULT_ICON_ANIMATION.id) ?: DEFAULT_ICON_ANIMATION.id
        return IconAnimation.fromId(animationId)
    }
    
    fun setLaunchAnimation(animation: LaunchAnimation) {
        prefs.edit().putString(LAUNCH_ANIMATION_KEY, animation.id).apply()
    }
    
    fun getCurrentLaunchAnimation(): LaunchAnimation {
        val animationId = prefs.getString(LAUNCH_ANIMATION_KEY, DEFAULT_LAUNCH_ANIMATION.id) ?: DEFAULT_LAUNCH_ANIMATION.id
        return LaunchAnimation.fromId(animationId)
    }
    
    fun getAllAnimationSpeeds(): List<AnimationSpeed> = AnimationSpeed.values().toList()
    fun getAllAnimationStyles(): List<AnimationStyle> = AnimationStyle.values().toList()
    fun getAllPageTransitions(): List<PageTransition> = PageTransition.values().toList()
    fun getAllIconAnimations(): List<IconAnimation> = IconAnimation.values().toList()
    fun getAllLaunchAnimations(): List<LaunchAnimation> = LaunchAnimation.values().toList()
    
    fun createInterpolator(): android.view.animation.Interpolator {
        return when (getCurrentAnimationStyle()) {
            AnimationStyle.SMOOTH -> AccelerateDecelerateInterpolator()
            AnimationStyle.BOUNCE -> AccelerateInterpolator()
            AnimationStyle.ELASTIC -> DecelerateInterpolator()
            AnimationStyle.LINEAR -> LinearInterpolator()
        }
    }
    
    fun getAnimationDuration(baseDuration: Long): Long {
        val speed = getCurrentAnimationSpeed()
        return (baseDuration * speed.multiplier).toLong()
    }
    
    fun createPageTransitionAnimation(fromX: Float, toX: Float, fromY: Float, toY: Float): Animation {
        val transition = getCurrentPageTransition()
        val duration = getAnimationDuration(300)
        
        return when (transition) {
            PageTransition.SLIDE -> {
                val slideAnim = TranslateAnimation(fromX, toX, fromY, toY)
                slideAnim.duration = getAnimationDuration(300)
                slideAnim.interpolator = createInterpolator()
                slideAnim
            }
            PageTransition.FADE -> {
                val fadeAnim = AlphaAnimation(0f, 1f)
                fadeAnim.duration = getAnimationDuration(200)
                fadeAnim.interpolator = createInterpolator()
                fadeAnim
            }
            PageTransition.SCALE -> {
                val scaleAnim = ScaleAnimation(0.8f, 1f, 0.8f, 1f)
                scaleAnim.duration = getAnimationDuration(250)
                scaleAnim.interpolator = createInterpolator()
                scaleAnim
            }
            PageTransition.NONE -> {
                val noAnim = AlphaAnimation(1f, 1f)
                noAnim.duration = 0
                noAnim
            }
        }
    }
    
    fun createIconAnimation(): Animation {
        val animation = getCurrentIconAnimation()
        val duration = getAnimationDuration(200)
        
        return when (animation) {
            IconAnimation.SCALE -> {
                val scaleAnim = ScaleAnimation(1f, 1.1f, 1f, 1.1f)
                scaleAnim.duration = duration
                scaleAnim.interpolator = createInterpolator()
                scaleAnim.repeatCount = 1
                scaleAnim.repeatMode = Animation.REVERSE
                scaleAnim
            }
            IconAnimation.ROTATE -> {
                val rotateAnim = android.view.animation.RotateAnimation(0f, 360f)
                rotateAnim.duration = duration
                rotateAnim.interpolator = createInterpolator()
                rotateAnim
            }
            IconAnimation.BOUNCE -> {
                val bounceAnim = ScaleAnimation(1f, 1.2f, 1f, 1.2f)
                bounceAnim.duration = duration
                bounceAnim.interpolator = AccelerateInterpolator()
                bounceAnim.repeatCount = 2
                bounceAnim.repeatMode = Animation.REVERSE
                bounceAnim
            }
            IconAnimation.NONE -> {
                val noAnim = AlphaAnimation(1f, 1f)
                noAnim.duration = 0
                noAnim
            }
        }
    }
    
    fun createLaunchAnimation(): Animation {
        val animation = getCurrentLaunchAnimation()
        val duration = getAnimationDuration(300)
        
        return when (animation) {
            LaunchAnimation.FADE -> {
                val fadeAnim = AlphaAnimation(0f, 1f)
                fadeAnim.duration = duration
                fadeAnim.interpolator = createInterpolator()
                fadeAnim
            }
            LaunchAnimation.SCALE -> {
                val scaleAnim = ScaleAnimation(0.5f, 1f, 0.5f, 1f)
                scaleAnim.duration = duration
                scaleAnim.interpolator = createInterpolator()
                scaleAnim
            }
            LaunchAnimation.SLIDE -> {
                val slideAnim = TranslateAnimation(0f, 0f, 100f, 0f)
                slideAnim.duration = duration
                slideAnim.interpolator = createInterpolator()
                slideAnim
            }
            LaunchAnimation.NONE -> {
                val noAnim = AlphaAnimation(1f, 1f)
                noAnim.duration = 0
                noAnim
            }
        }
    }
    
    fun exportSettings(): String {
        val settings = mapOf(
            "animationSpeed" to getCurrentAnimationSpeed().id,
            "animationStyle" to getCurrentAnimationStyle().id,
            "pageTransition" to getCurrentPageTransition().id,
            "iconAnimation" to getCurrentIconAnimation().id,
            "launchAnimation" to getCurrentLaunchAnimation().id
        )
        return org.json.JSONObject(settings).toString(2)
    }
    
    fun importSettings(jsonString: String) {
        try {
            val json = org.json.JSONObject(jsonString)
            
            json.optString("animationSpeed")?.let { speedId ->
                AnimationSpeed.fromId(speedId).let { setAnimationSpeed(it) }
            }
            
            json.optString("animationStyle")?.let { styleId ->
                AnimationStyle.fromId(styleId).let { setAnimationStyle(it) }
            }
            
            json.optString("pageTransition")?.let { transitionId ->
                PageTransition.fromId(transitionId).let { setPageTransition(it) }
            }
            
            json.optString("iconAnimation")?.let { animationId ->
                IconAnimation.fromId(animationId).let { setIconAnimation(it) }
            }
            
            json.optString("launchAnimation")?.let { animationId ->
                LaunchAnimation.fromId(animationId).let { setLaunchAnimation(it) }
            }
        } catch (e: Exception) {
            // Invalid JSON, keep current settings
        }
    }
}
