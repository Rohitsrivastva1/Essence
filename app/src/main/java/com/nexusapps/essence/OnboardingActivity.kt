package com.nexusapps.essence

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var skipButton: Button
    private lateinit var nextButton: Button
    private lateinit var finishButton: Button
    private lateinit var pageIndicator: LinearLayout
    
    private val onboardingPages = listOf(
        OnboardingPage(
            title = "Welcome to Essence",
            description = "A distraction-free launcher designed to help you focus on what matters most.",
            icon = "🎯"
        ),
        OnboardingPage(
            title = "Focus Modes",
            description = "Switch between Work, Personal, and Emergency modes to show only relevant apps.",
            icon = "⚡"
        ),
        OnboardingPage(
            title = "Analytics & Insights",
            description = "Track your app usage and get insights to improve your digital wellness.",
            icon = "📊"
        ),
        OnboardingPage(
            title = "Gestures & Shortcuts",
            description = "Use swipe gestures to quickly switch modes and access settings.",
            icon = "👆"
        ),
        OnboardingPage(
            title = "Customization",
            description = "Personalize your experience with themes, categories, and preferences.",
            icon = "🎨"
        )
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        
        // Check if onboarding is already completed
        val prefs = getSharedPreferences("essence_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_completed", false)) {
            startMainActivity()
            return
        }
        
        setupUI()
    }
    
    private fun setupUI() {
        viewPager = findViewById(R.id.viewPager)
        skipButton = findViewById(R.id.skipButton)
        nextButton = findViewById(R.id.nextButton)
        finishButton = findViewById(R.id.finishButton)
        pageIndicator = findViewById(R.id.pageIndicator)
        
        val adapter = OnboardingAdapter(onboardingPages)
        viewPager.adapter = adapter
        
        setupPageIndicator()
        setupButtons()
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
                updatePageIndicator(position)
            }
        })
    }
    
    private fun setupPageIndicator() {
        for (i in onboardingPages.indices) {
            val dot = ImageView(this)
            val layoutParams = LinearLayout.LayoutParams(24, 24)
            layoutParams.setMargins(8, 0, 8, 0)
            dot.layoutParams = layoutParams
            dot.setImageResource(android.R.drawable.ic_menu_circle)
            dot.alpha = 0.3f
            pageIndicator.addView(dot)
        }
        updatePageIndicator(0)
    }
    
    private fun updatePageIndicator(position: Int) {
        for (i in 0 until pageIndicator.childCount) {
            val dot = pageIndicator.getChildAt(i) as ImageView
            dot.alpha = if (i == position) 1.0f else 0.3f
        }
    }
    
    private fun setupButtons() {
        skipButton.setOnClickListener {
            completeOnboarding()
        }
        
        nextButton.setOnClickListener {
            if (viewPager.currentItem < onboardingPages.size - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                completeOnboarding()
            }
        }
        
        finishButton.setOnClickListener {
            completeOnboarding()
        }
    }
    
    private fun updateButtons(position: Int) {
        val isLastPage = position == onboardingPages.size - 1
        
        skipButton.visibility = if (isLastPage) View.GONE else View.VISIBLE
        nextButton.visibility = if (isLastPage) View.GONE else View.VISIBLE
        finishButton.visibility = if (isLastPage) View.VISIBLE else View.GONE
    }
    
    private fun completeOnboarding() {
        val prefs = getSharedPreferences("essence_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        startMainActivity()
    }
    
    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String
)

class OnboardingAdapter(private val pages: List<OnboardingPage>) : 
    androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {
    
    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(pages[position])
    }
    
    override fun getItemCount(): Int = pages.size
    
    class OnboardingViewHolder(itemView: android.view.View) : 
        androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
        private val iconText: TextView = itemView.findViewById(R.id.iconText)
        
        fun bind(page: OnboardingPage) {
            titleText.text = page.title
            descriptionText.text = page.description
            iconText.text = page.icon
        }
    }
}
