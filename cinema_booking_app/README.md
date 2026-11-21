# cinema_booking_app

## Kết nối backend

App mặc định gọi Gateway tại `http://10.0.2.2:8080` trên Android Emulator và
`http://localhost:8080` trên Windows/Web. Nếu chạy trên điện thoại Android thật,
hãy dùng địa chỉ IPv4 LAN của máy chạy Gateway:

```powershell
flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8080
```

Thay `192.168.1.10` bằng IPv4 của máy backend. Máy tính và điện thoại cần cùng
mạng, và cổng `8080` phải được phép truy cập từ mạng LAN. API danh sách phim là
`GET /api/public/movies?status=now-showing`.

## Concession customer — Iteration 2

Đăng nhập bằng tài khoản USER của Identity qua Gateway. Token chỉ giữ trong RAM;
khởi động lại app cần đăng nhập lại. API danh sách là `/api/v1/concessions`,
trang bắt đầu từ 0, mỗi trang mặc định 20 món. App tải các trang rồi lọc PC/DR
bằng trường `type`, không suy loại món từ UUID. Giá niêm yết là số nguyên VND;
response `25000.00` vẫn hợp lệ. Backend là nguồn quyết định món active và giá.

Sau khi ngừng bán món qua ADMIN API, nhấn tải lại danh sách: món không còn trong
catalog và lựa chọn giỏ local tương ứng được bỏ. Khi refresh lỗi, app giữ danh
sách đã tải nhưng hiển thị lỗi và nút Retry. Việc này không thay thế quote và
kiểm tra lại giá/trạng thái khi tạo Booking.

Kiểm tra từ thư mục `cinema_booking_app`:

```powershell
flutter analyze
flutter test test/features/customer/catalog_acceptance_test.dart test/features/customer/add_concession_to_cart_test.dart test/core/network/session_expiry_test.dart test/features/account/data/repositories/account_repository_impl_test.dart test/widget_test.dart
```

Smoke test Android trên emulator đã chạy, thay ID thiết bị nếu khác:

```powershell
flutter test integration_test/concession_catalog_smoke_test.dart -d emulator-5554 --dart-define=CHECK_LIVE_GATEWAY=true
```

Smoke test dùng HTTP fixture ngay trên Android cho 21 món/2 trang, bộ lọc,
inactive, ảnh, refresh và retry; không tạo/sửa dữ liệu DB thật. Cờ
`CHECK_LIVE_GATEWAY=true` chạy thêm kiểm tra Gateway thật tại
`http://10.0.2.2:8080/actuator/health`. Nếu bỏ cờ, ca health được đánh dấu skip.
Hai ca này không thay thế đăng nhập Identity và đọc Catalog thật trên Android.

Sau integration test, build lại ứng dụng thường trước khi dùng/chia sẻ APK:

```powershell
flutter build apk --debug
```

APK ứng dụng thường dùng `lib/main.dart`, không dùng điểm khởi chạy fixture.
Không commit token, APK, thư mục build hoặc cấu hình SDK cá nhân. Kết quả và
giới hạn kiểm thử được ghi trong [verification](../docs/verification.md).

Giỏ bắp/nước lưu tạm trong phiên đăng nhập, tối đa 10 phần/món. Có thể chọn món
trước khi giữ ghế; giỏ đi theo hold đầu tiên, chuyển sang hold khác thì xóa lựa
chọn cũ. Cần hold còn hạn để tiếp tục đặt đơn. Giá trong giỏ là tạm tính; Booking
xác nhận lại khi tạo đơn. Xem [quy tắc và checklist giỏ](../docs/contracts/concession-cart-acceptance.md).

A new Flutter project.

## Getting Started

This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Learn Flutter](https://docs.flutter.dev/get-started/learn-flutter)
- [Write your first Flutter app](https://docs.flutter.dev/get-started/codelab)
- [Flutter learning resources](https://docs.flutter.dev/reference/learning-resources)

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev/), which offers tutorials,
samples, guidance on mobile development, and a full API reference.
