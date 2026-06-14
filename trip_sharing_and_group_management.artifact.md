# [SIÊU CHI TIẾT] Tài liệu Thiết kế Hệ thống Chia sẻ & Quản lý Nhóm Du lịch

Tài liệu này không chỉ là một bản kế hoạch, mà là một **"Sách trắng" (Whitepaper)** hướng dẫn bạn tư duy và xây dựng tính năng quản lý nhóm từ con số 0. Tài liệu tập trung vào logic cốt lõi để bạn có thể code bằng bất kỳ ngôn ngữ nào (Java, Python, Go, C#, PHP...).

---

## PHẦN 1: TƯ DUY KIẾN TRÚC (THE ARCHITECTURE)

### 1.1. Mô hình "Nhà và Chìa khóa"
Hãy tưởng tượng mỗi Chuyến đi là một **Căn nhà**:
- **Chủ nhà (Owner)**: Người xây nhà, có quyền đập nhà, đổi khóa, hoặc bán nhà.
- **Người thuê (Editor)**: Được quyền sắp xếp đồ đạc, sơn lại tường (sửa lịch trình) nhưng không được bán nhà.
- **Khách tham quan (Viewer)**: Chỉ được đi dạo và xem, không được chạm vào đồ vật.

### 1.2. Cấu trúc dữ liệu (Data Schema - Ngôn ngữ chung)
Bạn cần 3 bảng (table) chính phối hợp với nhau:

#### Bảng A: NGƯỜI DÙNG (User)
- `ID`: Định danh duy nhất.
- `Tên`, `Email`, `Avatar`.

#### Bảng B: CHUYẾN ĐI (Trip) - "Căn nhà"
- `ID`: Định danh chuyến đi.
- `Tiêu đề`, `Ngày bắt đầu`, `Ngày kết thúc`.
- `Chế độ hiển thị`: (Riêng tư / Chỉ người có link / Công khai).
- `Share_Token`: Một mã ngẫu nhiên cực dài (Ví dụ: `t_982hjs82js...`) dùng cho link chia sẻ.
- `Invite_Code`: Một mã ngắn (Ví dụ: `GO-2024`) dùng để gõ tay.

#### Bảng C: THÀNH VIÊN (TripMember) - "Sổ hộ khẩu"
Đây là bảng quan trọng nhất, lưu mối quan hệ giữa Người và Nhà:
- `Trip_ID`: Chuyến đi nào?
- `User_ID`: Người nào?
- `Vai trò`: (OWNER, EDITOR, VIEWER).
- `Trạng thái`: (Đã mời / Đã tham gia / Đã rời đi).

---

## PHẦN 2: CHI TIẾT PHÂN QUYỀN (ROLE & PERMISSION)

Bạn cần một "Ma trận quyền hạn" để lập trình:

| Hành động | Owner | Editor | Viewer |
| :--- | :---: | :---: | :---: |
| Xem lịch trình | ✅ | ✅ | ✅ |
| Thêm/Sửa/Xóa địa điểm | ✅ | ✅ | ❌ |
| Mời người mới | ✅ | ✅ | ❌ |
| Đổi vai trò thành viên khác | ✅ | ❌ | ❌ |
| Đuổi thành viên ra khỏi nhóm | ✅ | ❌ | ❌ |
| Chuyển quyền Chủ nhóm | ✅ | ❌ | ❌ |
| Xóa vĩnh viễn Chuyến đi | ✅ | ❌ | ❌ |

---

## PHẦN 3: CÁC LUỒNG XỬ LÝ LOGIC (STEP-BY-STEP)

### 3.1. Luồng "Tham gia bằng Link" (Flow chi tiết)
Khi ai đó nhấn vào link `myapp.com/join/t_982hjs82js`:
1.  **Bước 1**: Server nhận mã `t_982hjs82js`.
2.  **Bước 2**: Tìm trong Bảng B (Trip) xem có chuyến đi nào khớp mã này không?
    - Nếu KHÔNG: Báo lỗi "Link không tồn tại".
3.  **Bước 3**: Nếu CÓ, kiểm tra xem người dùng hiện tại đã có tên trong Bảng C (TripMember) của chuyến đi này chưa?
    - Nếu CÓ rồi: Chuyển hướng thẳng vào trang lịch trình.
4.  **Bước 4**: Nếu CHƯA:
    - Tạo một dòng mới vào Bảng C: `User_ID` = Người hiện tại, `Trip_ID` = ID vừa tìm được, `Vai trò` = VIEWER (mặc định), `Trạng thái` = Đã tham gia.
5.  **Bước 5**: Thông báo cho Chủ nhóm: "Có thành viên mới vừa tham gia qua link".

### 3.2. Luồng "Bảo vệ API" (Middleware Logic)
Trước khi thực hiện bất kỳ lệnh sửa đổi nào (ví dụ: `Xóa địa điểm`), Server phải chạy đoạn code logic này:
```text
HÀM KIỂM_TRA_QUYỀN(Người_Dùng_Hiện_Tại, Chuyến_Đi_ID, Quyền_Yêu_Cầu):
    1. Tìm trong bảng TripMember dòng nào có (User_ID = Người_Dùng_Hiện_Tại AND Trip_ID = Chuyến_Đi_ID)
    2. Nếu không tìm thấy -> BÁO LỖI: "Bạn không thuộc nhóm này".
    3. Nếu tìm thấy:
       - Lấy ra "Vai trò" của người này.
       - Nếu Vai trò là OWNER -> Cho phép làm tất cả.
       - Nếu Vai trò là EDITOR:
           - Nếu Quyền_Yêu_Cầu là "SỬA_LỊCH_TRÌNH" -> Cho phép.
           - Nếu Quyền_Yêu_Cầu là "QUẢN_LÝ_NHÓM" -> BÁO LỖI.
       - Nếu Vai trò là VIEWER -> BÁO LỖI cho mọi hành động sửa đổi.
```

---

## PHẦN 4: XỬ LÝ CÁC TÌNH HUỐNG KHÓ (EDGE CASES)

### 4.1. Khi Chủ nhóm (Owner) muốn rời nhóm hoặc xóa tài khoản
Đây là lỗi logic rất hay gặp khiến dữ liệu bị mồ côi.
- **Giải pháp**: Nếu Owner nhấn "Rời nhóm", hệ thống phải kiểm tra xem trong nhóm còn ai khác không?
    - Nếu còn: Bắt buộc Owner phải chọn một người (Editor hoặc Viewer) để "Truyền ngôi" (Promote lên Owner) trước khi được đi.
    - Nếu không còn ai (nhóm chỉ có 1 mình): Hỏi "Bạn có muốn xóa luôn chuyến đi này không?".

### 4.2. Xung đột dữ liệu khi 2 người cùng sửa (Concurrency)
- **Tình huống**: A và B cùng sửa một lúc.
- **Giải pháp (Ngôn ngữ bình thường)**:
    Mỗi khi lưu dữ liệu, hãy gửi kèm một con số gọi là `version` hoặc `last_updated_at`.
    - Server kiểm tra: "Số version mà máy A gửi lên có khớp với số version đang có trên Server không?".
    - Nếu khớp: Lưu dữ liệu và tăng version lên +1.
    - Nếu KHÔNG khớp (nghĩa là đã có ai đó lưu trước đó rồi): Báo lỗi cho máy A "Dữ liệu đã cũ, vui lòng cập nhật lại trước khi sửa".

---

## PHẦN 5: TRẢI NGHIỆM NGƯỜI DÙNG (UX TIPS)

1.  **Hiển thị "Ai đang ở trong nhà"**: Ở góc trên màn hình, hãy hiện danh sách Avatar của các thành viên đang tham gia.
2.  **Gắn nhãn "Chủ nhóm"**: Luôn có một icon nhỏ (vương miện) bên cạnh tên của Owner để mọi người biết ai là người có quyền cao nhất.
3.  **Thông báo thay đổi**: Khi một thành viên thêm địa điểm mới, hãy hiện một dòng nhỏ: *"Nguyễn Văn A vừa thêm khách sạn X vào ngày 2"*. Điều này tạo cảm giác kết nối.

---

## PHẦN 6: DANH SÁCH CÁC CÔNG VIỆC CẦN LÀM (CHECKLIST)

- [ ] **Database**: Thiết kế 3 bảng dữ liệu như mục 1.2.
- [ ] **Auth**: Viết hàm kiểm tra quyền truy cập (mục 3.2).
- [ ] **Feature 1**: Tạo chuyến đi (Người tạo mặc định là Owner).
- [ ] **Feature 2**: Mời thành viên (Gửi lời mời trực tiếp hoặc qua link).
- [ ] **Feature 3**: Phê duyệt lời mời (Dành cho thành viên mới).
- [ ] **Feature 4**: Quản lý thành viên (Owner đổi vai trò người khác hoặc đuổi người).
- [ ] **Feature 5**: Rời nhóm & Xóa chuyến đi (Xử lý logic bàn giao quyền lực).
- [ ] **Feature 6**: Hệ thống thông báo khi có thay đổi trong nhóm.

---

> [!NOTE]
> **Lời khuyên**: Hãy bắt đầu từ việc nhỏ nhất là **Chia sẻ quyền xem (Viewer)**. Sau khi luồng này chạy ổn định, bạn hãy mở rộng sang quyền **Chỉnh sửa (Editor)**. Đừng cố gắng làm tất cả cùng lúc để tránh bị rối logic.
