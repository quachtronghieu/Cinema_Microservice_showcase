# Cinema Flutter — quy ước làm việc trong repository

Tài liệu này áp dụng cho toàn bộ repository. Trước khi sửa code, đọc `README.md`, tài liệu liên quan trong `docs/mobile/` (nếu đã có), và những file hiện có của feature. Ưu tiên mã nguồn cùng API contract thực tế; khi tài liệu và code mâu thuẫn, nêu rõ chỗ lệch và cập nhật tài liệu sau khi nhóm chốt quyết định. Không tự khẳng định một endpoint, bảng hoặc feature đã tồn tại khi chưa kiểm tra.

## 1. Mục tiêu và phạm vi

- Xây ứng dụng **Cinema** cho khách hàng trên **Android bằng Flutter**, tận dụng backend **Spring Boot + SQL Server** của dự án Cinema Management.
- Luồng chính: tìm phim → chọn suất → chọn ghế → đăng nhập nếu cần → giữ ghế → chọn bắp/nước hoặc bỏ qua → áp voucher → tạo đơn → thanh toán demo → nhận vé QR.
- Giai đoạn đầu chỉ làm thanh toán demo do backend xác nhận. Không triển khai thanh toán tiền thật, chuyển toàn bộ màn hình quản trị lên mobile hoặc thêm tính năng ngoài phạm vi nếu chưa được giao.
- Dữ liệu và nghiệp vụ chính thức nằm ở server: quyền sở hữu, trạng thái ghế/hold, giá, giảm giá, trạng thái thanh toán và việc phát hành vé.

## 2. Stack đã thống nhất

| Thành phần | Lựa chọn |
| --- | --- |
| Giao diện mobile | Flutter Material / Dart |
| Tổ chức code | Feature first; MVVM; UI, domain, data tách trách nhiệm |
| State và dependency injection | Provider + ChangeNotifier; UI state rõ ràng |
| Điều hướng | go_router |
| HTTP | Dio qua một ApiClient dùng chung; session cookie/cookie manager theo cách backend hiện có hỗ trợ |
| Backend | Spring Boot; controller → service → repository |
| Database chính | SQL Server, được backend truy cập; app không kết nối SQL Server trực tiếp |

Không tự thay Provider bằng Bloc/Riverpod, Dio bằng `http`, go_router bằng một router khác, hoặc chuyển cơ chế session sang JWT. Nếu phát hiện stack thực tế khác bảng này, báo rõ bằng chứng trong repo và thống nhất với nhóm trước khi đổi. Dùng phiên bản package phù hợp SDK đã chốt trong `pubspec.yaml`; commit `pubspec.lock` cho app.

## 3. Vị trí và cấu trúc code

Giữ Flutter trong `mobile/`, đặt cạnh cấu trúc web/backend hiện có. Không dời `src/`, `pom.xml` hoặc `fe/` chỉ để phù hợp với Flutter. Nếu checkout đang có đường dẫn khác, kiểm tra trước khi tạo thư mục mới.

mobile/
├── lib/
│   ├── main.dart
│   ├── app/
│   │   ├── app.dart
│   │   ├── app_router.dart
│   │   ├── app_theme.dart
│   │   └── app_providers.dart
│   ├── core/
│   │   ├── network/
│   │   ├── errors/
│   │   ├── config/
│   │   └── ui/
│   └── features/
│       └── <feature>/
│           ├── presentation/
│           │   ├── screens/
│           │   ├── view_models/
│           │   ├── states/
│           │   └── widgets/
│           ├── domain/
│           │   ├── entities/
│           │   ├── repositories/
│           │   └── use_cases/
│           └── data/
│               ├── dtos/
│               ├── mappers/
│               ├── services/
│               └── repositories/
├── test/
│   └── features/
│       └── <feature>/
│           ├── domain/
│           │   └── use_cases/
│           │       └── <use_case>_test.dart
│           ├── data/
│           │   └── repositories/
│           │       └── <repository>_test.dart
│           └── presentation/
│               ├── view_models/
│               │   └── <view_model>_test.dart
│               ├── widgets/
│               │   └── <widget>_test.dart
│               └── screens/
│                   └── <screen>_test.dart
└── integration_test/
    └── <feature>/
        └── <feature>_flow_test.dart

Chỉ tạo thư mục/file khi có code thực sự cần dùng; không sinh lớp rỗng cho đủ cây thư mục. `core/` chỉ chứa phần nhiều feature cùng dùng. Mỗi feature phải bám theo mẫu này, tránh chia nơi thì theo feature, nơi thì theo toàn bộ layer.

## 4. Luồng phụ thuộc và trách nhiệm

- **Screen/widget:** hiển thị trạng thái, nhận thao tác, gọi ViewModel; không gọi Dio, parse JSON hoặc quyết định giá/thanh toán.
- **ViewModel:** quản lý loading/empty/error/success, validation phía UI và thao tác người dùng; phụ thuộc vào repository interface hoặc use case qua constructor.
- **Domain:** entity thuần Dart, repository interface, use case cho nghiệp vụ phối hợp/phức tạp. Không import Flutter UI, Dio, data hoặc presentation.
- **Data service:** gọi endpoint qua ApiClient; DTO đọc/ghi JSON và mapper chuyển DTO ↔ entity.
- **Repository implementation:** triển khai interface, phối hợp data service, ánh xạ lỗi và nguồn dữ liệu. Không đặt luật bảo mật/transaction thay cho backend.
- **App wiring:** cung cấp dependency bằng Provider tại phạm vi phù hợp. Feature trao đổi bằng ID, model hoặc interface đã thống nhất; không gọi ViewModel của feature khác.
- **Backend:** controller nhận/trả DTO; service xử lý nghiệp vụ và transaction; repository truy cập SQL Server. Phía mobile không được coi dữ liệu client gửi lên là dữ liệu đã được xác thực.

## 5. Phân công feature và hợp đồng tích hợp

| Người | Hai màn hình có nghiệp vụ | CRUD chính | Signature function |
| --- | --- | --- | --- |
| TV1 | `LoginRegistrationScreen`, `AccountScreen` | Tài khoản/hồ sơ khách | Khôi phục mật khẩu qua email có hạn và token dùng một lần |
| TV2 | `MovieDiscoveryScreen`, `MovieDetailScreen` | Watchlist của khách | Gợi ý phim theo thể loại phim đã lưu, có fallback |
| TV3 | `ShowtimeSelectionScreen`, `SeatSelectionScreen` | `BookingDraft` | Giữ ghế 5 phút theo thời gian server và chống tranh ghế |
| TV4 | `ConcessionCatalogScreen`, `ConcessionCartScreen` | `CartItem` | Áp voucher và nhận báo giá đúng từ backend |
| TV5 | `MyBookingsScreen`, `BookingDetailScreen` | Booking theo trạng thái | Thanh toán demo và phát hành vé QR đúng một lần |

Mỗi người phụ trách cả screen, state, data/API, lỗi và kiểm thử của vertical slice mình. Dialog, splash, menu hoặc một màn hình chỉ đổi nhãn không tính là màn hình nghiệp vụ thứ hai. Các feature `auth`, `account`, `movies`, `watchlist`, `showtimes`, `seat_selection`, `concessions`, `cart`, `vouchers`, `bookings`, `payments`, `tickets` chỉ tách thành thư mục riêng theo dữ liệu và phạm vi thực tế của repo; hai màn hình của một người có thể phối hợp nhiều feature.

Trước khi sửa các điểm giao nhau, thống nhất contract: TV2 → TV3 `movieId`; TV3 → TV4/TV5 `holdId`, `showtimeId`, `seatIds`, `expiresAt`, `serverTime`; TV4 → TV5 `cartId`, `cartVersion`, `voucherCode` hoặc `quoteId`; TV1 cung cấp session và tín hiệu hết phiên. TV3 sở hữu luật hold, TV4 sở hữu báo giá/voucher, TV5 sở hữu đơn/payment/vé. TV5 không viết lại thuật toán khóa ghế hoặc tính giảm giá.

## 6. Quy tắc nghiệp vụ không được phá vỡ

- Khách chỉ xem/sửa dữ liệu của mình. Phim, suất chiếu và ghế vật lý là dữ liệu quản trị; CRUD của TV2 là watchlist, của TV3 là booking draft.
- Xóa tài khoản nghĩa là vô hiệu hóa có kiểm soát; xóa booking nghĩa là hủy khi trạng thái paid. Giữ lịch sử đơn đã thanh toán và vé đã phát hành.
- Booking draft không đồng nghĩa với ghế đã giữ. Server xác định ghế còn trống, khóa ghế trong transaction và quyết định thời điểm hết hold; countdown mobile chỉ hiển thị theo `expiresAt`/`serverTime`.
- Xóa draft không giải phóng hold đã được gắn vào booking. Hai yêu cầu tranh cùng ghế không được cùng thành công.
- Giỏ bắp/nước có thể trống. Backend kiểm voucher và tính lại tiền khi tạo đơn; không tin giá hoặc tổng tiền do client tự gửi.
- Lặp yêu cầu tạo đơn/thanh toán cùng khóa và payload không tạo đơn, giao dịch hoặc vé mới. QR chỉ hiển thị vé do backend phát hành sau thanh toán demo được xác nhận.
- Chốt endpoint, method, request/response, mã lỗi, quyền truy cập, ID, đơn vị tiền và timestamp trong API contract trước khi code các phần phụ thuộc. Không đoán schema DB hoặc URL API từ tên màn hình.

## 7. Clean code và trải nghiệm

- File Dart dùng `snake_case.dart`; class `UpperCamelCase`; biến/hàm `lowerCamelCase`. Đặt tên theo nghiệp vụ và giữ mỗi hàm/lớp một trách nhiệm rõ.
- Không gọi API trong `build()`. Hủy hoặc bỏ kết quả request cũ khi đổi lựa chọn; không cập nhật ViewModel sau `dispose`. Dọn `Timer`, `StreamSubscription` và controller đúng vòng đời.
- Màn hình dùng dữ liệu async cần trạng thái loading, empty, error/retry và success phù hợp. Form có validation, thông báo lỗi rõ và không bị bàn phím che trường nhập.
- Dùng một quy ước `Result`/`Failure` để truyền lỗi từ data lên UI; không nuốt exception, không để screen tự phân tích mã HTTP ở nhiều chỗ.
- Tiền theo API contract, không tính tiền chính thức bằng `double` trên client; thời điểm hết hạn có múi giờ/offset rõ. Base URL theo môi trường; không commit token, mật khẩu hay thông tin kết nối DB.
- Viết comment khi giải thích luật nghiệp vụ hoặc lý do kỹ thuật; thêm `///` cho API Dart cần giải thích. Không viết comment chỉ lặp lại câu lệnh.

## 8. Quy trình làm task và kiểm chứng

1. Xác định owner, hai màn hình, CRUD, signature, API contract và dữ liệu giao/nhận của feature; đối chiếu code hiện tại trước khi sửa.
2. Triển khai theo chiều dọc: entity/DTO + service/repository → ViewModel/state → screen/widget → kiểm thử; chỉ tạo use case khi có logic phối hợp thực tế.
3. Kiểm tra ca thành công và ít nhất một ca lỗi/biên quan trọng của phần thay đổi. Với ghế, voucher và payment, kiểm thử luật chính ở backend; UI test không thay thế transaction test.
4. Trong `mobile/`, chạy `dart format .`, `flutter analyze` và `flutter test` trước PR. Trước bản nộp chạy kiểm thử tích hợp cần thiết và xác nhận APK/AAB release trên thiết bị.
5. PR mô tả thay đổi, cách chạy thử, tác động API/schema và bằng chứng đã kiểm tra. Nhờ thành viên khác review; giữ commit cá nhân có ý nghĩa và cập nhật tài liệu/AI Assistance Log khi AI hỗ trợ đáng kể.


