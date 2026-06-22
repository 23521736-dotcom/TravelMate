# Tài liệu Kỹ thuật Dự án TravelMate

## 1. Tổng quan
TravelMate là một ứng dụng hỗ trợ du lịch toàn diện, cho phép người dùng lên kế hoạch chuyến đi, quản lý chi tiêu, khám phá địa điểm và tương tác với cộng đồng du lịch.

---

## 2. Kiến trúc Hệ thống
Dự án được xây dựng dựa trên **Clean Architecture** kết hợp với mô hình **MVVM (Model-View-ViewModel)**.

### Sơ đồ luồng hoạt động:
`UI (Compose)` ↔ `ViewModel` ↔ `UseCase (Domain)` ↔ `Repository (Domain/Data)` ↔ `Data Source (Remote/Local)`

- **UI Layer (Jetpack Compose):** Chịu trách nhiệm hiển thị giao diện và nhận tương tác từ người dùng.
- **ViewModel Layer:** Quản lý trạng thái giao diện (UI State) và xử lý logic nghiệp vụ từ UI.
- **Domain Layer:** Chứa các Business Logic thuần túy (Models, Repository Interfaces, UseCases). Không phụ thuộc vào bất kỳ thư viện Android nào.
- **Data Layer:** Triển khai các Repository, xử lý kết nối Firebase, Supabase, Groq AI và DataStore.

---

## 3. Cấu trúc Thư mục Chi tiết

```text
com.example.travelmate/
├── data/                       # Lớp dữ liệu (Implementation)
│   ├── local/                  # Lưu trữ cục bộ (PreferenceDataStore)
│   ├── remote/                 # Kết nối API & Cloud (Supabase, Groq)
│   ├── repository/             # Triển khai thực tế của các Repository
│   └── model/                  # DTOs (Data Transfer Objects) cho API
├── domain/                     # Lớp nghiệp vụ (Interfaces & Logic)
│   ├── model/                  # Các thực thể chính (User, Trip, Destination...)
│   ├── repository/             # Định nghĩa các Interface Repository
│   └── usecase/                # Các logic nghiệp vụ riêng lẻ (Auth, Logic xử lý)
├── di/                         # Dependency Injection (Hilt Modules)
├── ui/                         # Lớp giao diện (Presentation)
│   ├── navigation/             # Cấu hình NavGraph & Screen routes
│   ├── screen/                 # Các màn hình Compose (HomeScreen, Profile...)
│   │   └── components/         # Các thành phần giao diện dùng chung
│   ├── viewmodel/              # Các ViewModel xử lý logic cho Screen
│   ├── state/                  # Định nghĩa UI State dùng chung (AuthUiState...)
│   └── theme/                  # Cấu hình Theme, Color, Type cho ứng dụng
├── util/                       # Các lớp tiện ích (Email, String, Voice...)
├── MainActivity.kt             # Entry point của ứng dụng
└── TravelMateApp.kt            # Application class, khởi tạo Hilt
```

---

## 4. Giải thích Code & Luồng Dữ liệu Quan trọng

### A. Luồng Authentication (Xác thực người dùng)
- **`AuthRepositoryImpl.kt`**: Kết hợp **Firebase Auth** để quản lý Session và **Supabase** để lưu trữ thông tin bổ sung.
- **`LoginUseCase.kt`**: Đóng gói logic đăng nhập, giúp ViewModel không cần biết chi tiết về Firebase.
- **`AuthViewModel.kt`**: Quản lý `AuthUiState`. Khi trạng thái `user` thay đổi, `NavGraph` sẽ tự động điều hướng người dùng giữa màn hình Login và Main.

### B. Màn hình Chính (HomeScreen & HomeViewModel)
Đây là màn hình phức tạp nhất, tổng hợp dữ liệu từ nhiều nguồn:
1. **`HomeViewModel.kt`**:
   - Sử dụng `StateFlow<HomeUiState>` để cung cấp dữ liệu cho UI.
   - Hàm `observeUserAndRefresh()`: Lắng nghe sự thay đổi thông tin người dùng từ Firebase Realtime Database.
   - `fetchRecommendedDestinations()`: Lấy danh sách địa điểm gợi ý dựa trên sở thích (Interests) của người dùng.
   - `fetchUserTrips()`: Tính toán số lượng chuyến đi, tổng chi tiêu và tìm chuyến đi đang diễn ra (`ongoingTrip`).

2. **`HomeScreen.kt`**:
   - Sử dụng `collectAsState()` để quan sát dữ liệu từ ViewModel.
   - Giao diện chia thành các thành phần nhỏ (`HomeHeader`, `StatCard`, `OngoingTripCard`) để dễ bảo trì.
   - Tận dụng `AsyncImage` (Coil) để tải ảnh từ URL một cách mượt mà.

### C. Quản lý Chuyến đi (TripRepositoryImpl)
Đây là "trái tim" của ứng dụng với hơn 800 dòng code xử lý logic phức tạp:
- **Firebase Database**: Lưu trữ dữ liệu thời gian thực cho các chuyến đi (itinerary, activities, members).
- **Supabase Storage**: Dùng để upload và lưu trữ hình ảnh trong Gallery của chuyến đi.
- **Logic đồng bộ**: Xử lý việc tham gia chuyến đi bằng mã mời (`joinTripByInviteCode`) hoặc token chia sẻ.

---

## 5. Công nghệ sử dụng (Tech Stack)
- **Language:** Kotlin
- **UI:** Jetpack Compose (100% Declarative UI)
- **DI:** Google Hilt (Dependency Injection)
- **Async:** Kotlin Coroutines & Flow
- **Backend:** 
    - **Firebase:** Auth, Realtime Database, Analytics.
    - **Supabase:** Storage (Lưu trữ ảnh), PostgreSQL (Backup/Metadata).
    - **Groq AI:** Tích hợp AI hỗ trợ chat và lên kế hoạch.
- **Network:** Retrofit & OkHttp
- **Local Storage:** Jetpack DataStore (Preferences)
- **Image Loading:** Coil

---

## 6. Hướng dẫn mở rộng
Để thêm một tính năng mới (ví dụ: Booking):
1. Tạo Model trong `domain/model`.
2. Định nghĩa Interface trong `domain/repository`.
3. Triển khai logic trong `data/repository`.
4. Viết UseCase trong `domain/usecase` (nếu cần).
5. Tạo ViewModel và UI Screen tương ứng.
6. Đăng ký Screen trong `ui/navigation/NavGraph.kt`.
