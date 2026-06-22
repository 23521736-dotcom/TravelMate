# Phân tích Kỹ thuật: Các Tính năng Nâng cao trong TravelMate

Tài liệu này đi sâu vào cơ chế hoạt động của các tính năng phức tạp như Tìm kiếm giọng nói, Quản lý hình ảnh và Hệ thống mời thành viên.

---

## 1. Tìm kiếm bằng Giọng nói (Voice Search)

Tính năng này cho phép người dùng đọc tên địa điểm thay vì gõ phím.

### A. Thành phần cốt lõi (`VoiceToTextParser.kt`)
Ứng dụng sử dụng bộ thư viện **SpeechRecognizer** của Android để xử lý âm thanh.
- **Trạng thái (`VoiceToTextParserState`):** Quản lý văn bản đang nhận diện (`spokenText`), trạng thái đang nghe (`isListening`) và cường độ âm thanh (`rmsDb`).
- **Xử lý sự kiện:** 
    - `onReadyForSpeech`: Sẵn sàng nhận âm thanh.
    - `onResults`: Trả về kết quả văn bản cuối cùng sau khi phân tích xong.
    - `onPartialResults`: Trả về kết quả tạm thời (giúp giao diện cập nhật chữ ngay khi người dùng đang nói).

### B. Luồng hoạt động
1. Người dùng nhấn giữ biểu tượng Micro.
2. `startListening()` được gọi với ngôn ngữ mặc định (thường là tiếng Việt `vi-VN`).
3. Dữ liệu âm thanh được gửi lên Google Speech Services để chuyển đổi.
4. Văn bản trả về được đổ vào ô tìm kiếm và kích hoạt tìm kiếm tự động.

---

## 2. Quản lý Hình ảnh: Tải lên và Hiển thị

TravelMate sử dụng kết hợp giữa **Supabase Storage** (để chứa file ảnh) và **Firebase** (để chứa link ảnh).

### A. Quy trình Tải ảnh (Upload)
Khi bạn chọn ảnh từ máy để làm ảnh bìa chuyến đi hoặc ảnh gallery:
1. **Chọn ảnh:** Sử dụng `ActivityResultContracts.PickMultipleVisualMedia` để chọn nhiều ảnh cùng lúc.
2. **Chuyển đổi:** Ảnh được đọc dưới dạng mảng byte (`ByteArray`).
3. **Upload lên Supabase (`SupabaseService.kt`):** 
    - Ảnh được đẩy vào bucket `travel_mate`.
    - Supabase trả về một **URL công khai**.
4. **Lưu vào Firebase (`TripRepositoryImpl.kt`):** 
    - Link URL từ Supabase được lưu vào node `trips/{id}/galleryUrls`.

### B. Lấy ảnh từ Database
- Ứng dụng sử dụng thư viện **Coil** thông qua hàm `AsyncImage`. 
- Nó chỉ cần lấy chuỗi URL từ Firebase và tự động tải ảnh về, xử lý cache (lưu nháp) để lần sau mở lại không tốn mạng.

---

## 3. Quản lý Chuyến đi và Mã mời (Trip & Invitation)

Đây là cơ chế cốt lõi để người dùng có thể cùng nhau lên kế hoạch.

### A. Cơ chế Tạo mã mời
Khi một chuyến đi mới được tạo (`createTrip`), hệ thống sẽ tự động sinh ra 2 loại mã:
- **Invite Code (6 ký tự):** Dùng để nhập thủ công.
    - Công thức: `UUID.randomUUID().take(6).uppercase()`. 
    - Ví dụ: `A1B2C3`.
- **Share Token (12 ký tự):** Dùng cho link chia sẻ.
    - Ví dụ: `t_abc123def456`.

### B. Cơ chế Tham gia (Join Trip)
Để đảm bảo người dùng luôn tìm thấy chuyến đi, hệ thống thực hiện **"Quét 3 tầng"**:
1. **Tầng 1 (Index Lookup):** Tìm trực tiếp trong node `inviteCodes/{code}`. Đây là cách nhanh nhất.
2. **Tầng 2 (Property Search):** Nếu tầng 1 lỗi, nó sẽ tìm trong toàn bộ danh sách `trips` xem có Trip nào có trường `inviteCode` trùng khớp không.
3. **Tầng 3 (Deep Scan):** Nếu vẫn không thấy, nó sẽ duyệt thủ công và so sánh không phân biệt chữ hoa chữ thường.

### C. Phân quyền (Role Management)
Mỗi thành viên trong chuyến đi được gắn một vai trò (`TripMember.kt`):
- **OWNER (Trưởng nhóm):** Có toàn quyền (Xóa trip, đuổi thành viên, chỉnh sửa mọi thứ).
- **EDITOR (Biên tập viên):** Có thể thêm hoạt động, chi tiêu nhưng không thể xóa trip hay quản lý thành viên.
- **VIEWER (Người xem):** Chỉ có quyền xem thông tin.

### D. Tính nhất quán dữ liệu (Atomic Updates)
Hệ thống sử dụng hàm `updateChildren` của Firebase để cập nhật nhiều node cùng lúc. Ví dụ: khi bạn tham gia chuyến đi, hệ thống sẽ đồng thời:
- Thêm bạn vào danh sách thành viên của Trip.
- Thêm ID của Trip vào danh sách chuyến đi của bạn.
- *Điều này đảm bảo hoặc cả hai đều thành công, hoặc không có gì thay đổi, tránh tình trạng dữ liệu bị "lệch".*
