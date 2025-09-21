# 🎯 Essence Launcher

A distraction-free, dark-themed Android launcher that replaces the default home screen with a minimal interface showing only required apps, improving focus and reducing digital distractions.

## 🚀 Features

### Core Functionality
- **HOME Launcher**: Replaces default Android home screen
- **App Discovery**: Automatically finds and categorizes installed apps
- **Smart Whitelist**: Show only essential apps you choose
- **Dark Minimalist UI**: Black background with white text for minimal eye strain

### Advanced Features
- **Focus Modes**: Switch between Work, Personal, Emergency, and All modes
- **Smart Categorization**: 8 categories (Productivity, Communication, Entertainment, etc.)
- **Usage Analytics**: Track time spent on each app with detailed insights
- **Gesture Controls**: Swipe gestures for quick mode switching and actions
- **Performance Monitoring**: Real-time memory, CPU, and battery tracking
- **Onboarding System**: 5-step guided setup for new users

### Customization Options
- **Multiple Themes**: Dark, Light, High Contrast, AMOLED Black
- **Grayscale Mode**: Convert screen to grayscale to reduce appeal
- **Category Filtering**: Filter apps by category in settings
- **Backup/Restore**: Export and import your settings
- **Search Functionality**: Real-time app search with filters

## 🏆 Competitive Advantages

### vs. Nova Launcher
- ✅ **Focus-first design** (vs. customization-heavy)
- ✅ **Built-in analytics** (vs. third-party apps needed)
- ✅ **Digital wellness** (vs. feature bloat)

### vs. Microsoft Launcher
- ✅ **Privacy-focused** (vs. data collection)
- ✅ **Minimalist approach** (vs. productivity overload)
- ✅ **Local storage** (vs. cloud dependency)

### vs. Niagara Launcher
- ✅ **Smart categorization** (vs. alphabetical only)
- ✅ **Focus modes** (vs. single view)
- ✅ **Analytics integration** (vs. basic launcher)

## 📱 Screenshots

### Main Interface
- Clean black home screen with white text
- Only whitelisted apps visible
- Settings button for configuration

### Settings
- App selection with categories
- Focus mode switching
- Theme and grayscale options
- Analytics and backup buttons

### Analytics Dashboard
- Usage statistics per app
- Time tracking with hours/minutes
- Category breakdown analysis
- Performance metrics

## 🛠️ Technical Implementation

### Architecture
- **MVVM Pattern**: Clean architecture with separation of concerns
- **Room Database**: Local data storage for analytics
- **Coroutines**: Async operations for smooth performance
- **Material Design 3**: Modern UI components

### Dependencies
- Room Database (2.6.1) - Local data storage
- RecyclerView (1.3.2) - Efficient list rendering
- ViewPager2 (1.0.0) - Onboarding pages
- WorkManager (2.9.0) - Background tasks
- Lifecycle Components (2.7.0) - UI lifecycle management
- Coroutines (1.7.3) - Asynchronous programming

### Performance Specifications
- **Home Screen Load**: <500ms
- **Memory Usage**: <50MB
- **Battery Drain**: <2% idle
- **App Launch Time**: <200ms

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Java 11 or later
- Android SDK API 26+ (Android 8.0)

### Installation
1. Clone the repository:
```bash
git clone https://github.com/Rohitsrivastva1/Essence.git
cd Essence
```

2. Open in Android Studio
3. Sync project with Gradle files
4. Build and run on device or emulator

### Building APK
```bash
./gradlew assembleDebug
```

### Installing on Device
```bash
./gradlew installDebug
```

## 📖 Usage

### First Time Setup
1. **Complete Onboarding**: 5-step guided setup
2. **Set as Default Launcher**: When prompted by Android
3. **Add Apps**: Use settings to whitelist your essential apps
4. **Choose Focus Mode**: Select Work, Personal, or Emergency mode

### Daily Usage
- **Launch Apps**: Tap on app names to open
- **Switch Modes**: Swipe left/right to change focus modes
- **Quick Actions**: Long press for quick settings
- **View Analytics**: Check usage statistics in settings

### Gesture Controls
- **Swipe Up**: Show app drawer
- **Swipe Down**: Quick settings
- **Swipe Left/Right**: Switch focus modes
- **Long Press**: Quick actions menu

## 🎯 Focus Modes

### Work Mode
Shows only productivity apps:
- Productivity (Office, Notes, Calendar)
- Communication (Email, Slack, Teams)
- Utilities (Calculator, File Manager)

### Personal Mode
Shows only personal apps:
- Entertainment (YouTube, Netflix, Games)
- Health (Fitness, Meditation, Sleep)
- Education (Learning, Books, Courses)
- Finance (Banking, Budget, Investment)

### Emergency Mode
Shows only essential apps:
- Phone (Dialer, Contacts)
- Messages (SMS, Emergency contacts)
- Camera (Emergency photos)

### All Mode
Shows all whitelisted apps

## 📊 Analytics Features

### Usage Tracking
- Time spent per app
- Launch count statistics
- Last used timestamps
- Category-based analysis

### Performance Monitoring
- Memory usage tracking
- CPU usage monitoring
- Battery level tracking
- Performance recommendations

### Insights
- Most used apps
- Recently used apps
- Category usage breakdown
- Digital wellness metrics

## 🔧 Configuration

### Settings Options
- **App Management**: Add/remove apps from whitelist
- **Category Filtering**: Filter apps by category
- **Focus Modes**: Configure mode-specific app lists
- **Themes**: Switch between different visual themes
- **Grayscale Mode**: Enable/disable grayscale display
- **Backup/Restore**: Export/import settings

### Customization
- **Theme Selection**: Dark, Light, High Contrast, AMOLED
- **Font Options**: Different text sizes and styles
- **Layout Options**: Grid, list, compact views
- **Color Schemes**: Custom accent colors

## 🛡️ Privacy & Security

### Privacy-First Design
- **No Data Collection**: Zero telemetry or tracking
- **Local Storage**: All data stored locally on device
- **No Cloud Sync**: No external data transmission
- **Open Source**: Transparent codebase

### Security Features
- **App Lock**: Password protect sensitive apps
- **Hidden Apps**: Completely hide apps from system
- **Privacy Mode**: Hide sensitive information
- **Secure Storage**: Encrypted local data storage

## 🧪 Testing

### Manual Testing
- [ ] Installation and setup
- [ ] App selection and whitelisting
- [ ] Focus mode switching
- [ ] Gesture controls
- [ ] Analytics functionality
- [ ] Settings configuration
- [ ] Performance monitoring

### Automated Testing
- [ ] Unit tests for core logic
- [ ] Integration tests for database
- [ ] UI tests for navigation
- [ ] Performance tests for optimization

## 📈 Roadmap

### Phase 1 (Current)
- ✅ Core launcher functionality
- ✅ Focus modes and categorization
- ✅ Analytics and performance monitoring
- ✅ Gesture controls and customization

### Phase 2 (Future)
- 🔄 Widget support for information display
- 🔄 Voice commands for app launching
- 🔄 Advanced gesture patterns
- 🔄 Custom icon pack support

### Phase 3 (Advanced)
- 🔄 AI-powered app recommendations
- 🔄 Smart scheduling based on usage patterns
- 🔄 Integration with task managers
- 🔄 Advanced digital wellness features

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Material Design 3 for UI components
- Android Room for database management
- Kotlin Coroutines for async operations
- The Android community for inspiration

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Rohitsrivastva1/Essence/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Rohitsrivastva1/Essence/discussions)
- **Email**: support@essencelauncher.com

## 🎯 Mission

Essence Launcher is designed to help users:
- **Focus** on what matters most
- **Reduce** digital distractions
- **Improve** productivity and well-being
- **Maintain** privacy and security

---

**Ready to compete with Nova Launcher, Microsoft Launcher, and Niagara Launcher!** 🏆

*Built with ❤️ for digital wellness and productivity*
