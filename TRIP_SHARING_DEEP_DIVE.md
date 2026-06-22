# Deep Dive: Cơ chế Chia sẻ và Tham gia Chuyến đi (Siêu Chi Tiết)

Tài liệu này giải thích "chân tơ kẽ tóc" cách ứng dụng TravelMate kết nối các thành viên vào cùng một chuyến đi. Đây là phần logic phức tạp nhất trong hệ thống dữ liệu của app.

---

## 1. Hai loại "Chìa khóa" mở cửa Chuyến đi

Khi bạn tạo một Trip, hệ thống tự động đúc ra 2 loại chìa khóa:

1.  **Mã mời (Invite Code - 6 ký tự):** Dành cho việc nhập tay. 
    *   *Ví dụ:* `X7R2B9`. Ngắn gọn, dễ đọc, dễ nhớ.
2.  **Link chia sẻ (Share Token - 12 ký tự):** Dành cho việc gửi qua Zalo, Messenger. 
    *   *Ví dụ:* `t_abc123...`. Dài và bảo mật hơn.

**Tại sao cần cả hai?** Mã mời giúp bạn đọc cho bạn bè nhập nhanh, còn Link giúp bạn chia sẻ lên nhóm mà người khác chỉ cần nhấn là vào luôn.

---

## 2. Thuật toán "Quét 3 Tầng" (Triple-Scan Search)

Đây là phần "thông minh" nhất. Khi bạn nhập một mã mời, hệ thống không chỉ tìm một lần rồi thôi, mà nó thực hiện chiến dịch tìm kiếm qua 3 giai đoạn để đảm bảo **không bao giờ bỏ sót**:

### Tầng 1: Tìm nhanh bằng Chỉ mục (Index Lookup)
*   **Cơ chế:** Hệ thống nhìn vào một danh sách riêng biệt gọi là `inviteCodes`. Danh sách này giống như mục lục của cuốn sách.
*   **Ưu điểm:** Tốc độ cực nhanh (gần như tức thì).
*   **Nhược điểm:** Đôi khi mục lục này bị hỏng hoặc chưa kịp cập nhật.

### Tầng 2: Tìm theo Thuộc tính (Property Search)
*   **Cơ chế:** Nếu Tầng 1 không thấy, hệ thống sẽ yêu cầu Firebase: "Hãy tìm trong tất cả các chuyến đi, xem cái nào có trường `inviteCode` trùng với mã này".
*   **Tại sao cần?** Để đề phòng trường hợp "Mục lục" ở Tầng 1 bị lỗi nhưng dữ liệu gốc trong Trip vẫn còn.

### Tầng 3: Quét sâu toàn bộ (Ultimate Deep Scan)
*   **Cơ chế:** Đây là biện pháp cuối cùng. Hệ thống tải danh sách Trip về và tự tay kiểm tra từng cái một, **không phân biệt chữ hoa chữ thường**.
*   **Lợi ích:** Đảm bảo 100% tìm thấy nếu mã đó thực sự tồn tại, kể cả khi bạn nhập chữ thường hay chữ hoa.

> [!TIP]
> **Cơ chế Tự chữa lành (Self-Healing):** Nếu hệ thống tìm thấy mã ở Tầng 2 hoặc 3 nhưng Tầng 1 (Mục lục) lại không có, nó sẽ **tự động sửa lại mục lục** ngay lúc đó để lần sau bạn hoặc người khác tìm sẽ nhanh như chớp (Tầng 1).

---

## 3. Cập nhật Nguyên tử (Atomic Updates - "Tất cả hoặc Không có gì")

Khi một người nhấn "Tham gia", dữ liệu không chỉ thay đổi ở 1 chỗ. Nó phải thay đổi ở **nhiều nơi cùng lúc**:

1.  **Tại Chuyến đi:** Thêm tên và ảnh của bạn vào danh sách thành viên.
2.  **Tại Cá nhân bạn:** Thêm ID của chuyến đi vào danh sách "Chuyến đi của tôi".

**Rủi ro:** Nếu đang cập nhật (1) mà mất mạng, không cập nhật được (2) thì sao? Bạn sẽ ở trong nhóm nhưng khi mở app lên lại không thấy nhóm đó đâu.

**Giải pháp của TravelMate:** Sử dụng lệnh `updateChildren`. 
Hệ thống đóng gói tất cả các thay đổi vào một "kiện hàng" duy nhất và gửi lên Firebase. Firebase cam kết: **Hoặc là tất cả đều thành công, hoặc là không có gì thay đổi.** Điều này giúp dữ liệu luôn đồng bộ tuyệt đối.

---

## 4. Bảo mật và Phân quyền sau khi vào nhóm

Khi bạn vừa nhập mã thành công:
- **Vai trò mặc định:** Bạn sẽ là `VIEWER` (Chỉ xem). Điều này giúp tránh việc người lạ có mã mời vào phá phách chuyến đi của bạn.
- **Nâng quyền:** Chỉ có `OWNER` (Trưởng nhóm - người tạo Trip) mới có quyền vào mục "Quản lý thành viên" để nâng bạn lên thành `EDITOR` (Người chỉnh sửa).
- **Rời nhóm:** Bạn có quyền rời nhóm bất cứ lúc nào. Khi đó, hệ thống lại dùng lệnh "Cập nhật nguyên tử" để xóa tên bạn khỏi Trip và xóa Trip khỏi danh sách của bạn cùng một lúc.

---

## 5. Tóm tắt luồng đi của Code (Cho người tò mò)

1.  **`TripRepositoryImpl.kt`**: Chứa thuật toán Quét 3 tầng và lệnh Cập nhật nguyên tử.
2.  **`JoinTripViewModel.kt`**: Nhận mã từ người dùng, gọi Repository và thông báo kết quả (Thành công/Thất bại) cho màn hình.
3.  **`JoinTripScreen.kt`**: Ô nhập mã mời và nút "Tham gia".

Hy vọng bản Deep Dive này giúp bạn hiểu rõ "bộ não" xử lý dữ liệu đằng sau tính năng chia sẻ cực kỳ mạnh mẽ của TravelMate!
