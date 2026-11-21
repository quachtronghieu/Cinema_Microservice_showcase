# Seed dữ liệu local dùng chung

Dữ liệu thử cho máy dev, lấy từ `data.sql` gốc: 12 phim, phòng 1–3 (TM000001–TM000003, 235 ghế) và 2 suất demo. Phòng 4 không nhập: dataset demo dùng 3 phòng (D-06). Chỉ dùng cho local; không chạy trên DB legacy hoặc production. Các script chỉ thêm dòng còn thiếu, không sửa hay xóa dữ liệu có sẵn.

## Thứ tự chạy

Bốn database phải cùng một SQL Server instance. Mỗi service phải chạy một lần trước để Flyway tạo schema: Catalog V4 trở lên, Cinema V1–V2, Booking V1. Script không tạo database hay tài khoản. Chạy từng file trong SSMS, dừng lại nếu có lỗi.

| File | DB | Nội dung |
|---|---|---|
| `01_catalog.sql` | catalog_db | 12 phim nguồn và phim demo `MV000016` (NGAN LOCAL DEMO), phát hành hôm qua tính tại lần chạy đầu |
| `02_cinema.sql` | cinema_db | 3 template, phòng 1–3, 235 ghế, suất `SW00023` (phòng 1) và `SW00024` (phòng 2) vào ngày mai lúc 10:00–12:15, OPEN |
| `03_booking.sql` | booking_db | Sales_Snapshot và 155 ghế theo suất (`HS2xxxxx`) cho hai suất demo; giá STANDARD 80000, VIP 100000 VND |
| `05_cinema_copy_to_flyway_db.sql` | cinema_db | Chỉ dùng một lần, khi máy đã có `cinema_db` cũ do Hibernate tạo (xem mục bên dưới) |
| `06_cinema_test_room.sql` | cinema_db | Phòng thử 901 (8 ghế) để test tạo suất, sửa suất và mở bán mà không chiếm lịch phòng 1–3 |

`03_booking.sql` tạo sẵn ghế bên Booking cho hai suất demo mà không cần gọi Open Sales. Suất mới tạo qua API có ghế bên Booking khi Open Sales thành công; bước này cần `CINEMA_SERVICE_CLIENT_SECRET`, thiếu thì trả 503. Script đọc chéo sang cinema_db và catalog_db; việc đọc chéo này chỉ dùng cho seed, không dùng trong service.

Kết quả mong đợi: phòng 1, 2, 3 có 75/80/80 ghế; `GET /api/v1/showtimes/SW00023/seat-availability` trả 75 ghế, SW00024 trả 80 ghế; `GET /api/v1/movies?status=now-showing` có phim NGAN LOCAL DEMO.

Chạy lại script không đổi ngày suất và không đặt lại trạng thái ghế. Khi suất demo đã qua giờ chiếu, tạo suất mới qua API (ID UUID) thay vì sửa ngày của suất đã có hold hoặc booking.

## Lưu ý

- Để test giữ ghế, dùng `showtimeSeatId` lấy từ Booking (`HS...`), không dùng ID ghế vật lý `TS...`.
- Không có dữ liệu Identity trong seed; tài khoản lấy từ Identity V2 hoặc đăng ký qua API (mục "Tài khoản demo" bên dưới).
- Nếu 03 báo thiếu `dbo.Sales_Snapshot`: booking-service chưa chạy Flyway trên đúng `booking_db`. Kiểm tra `SELECT name FROM booking_db.sys.tables` có `Sales_Snapshot` và `flyway_schema_history`. Không tự chép CREATE TABLE từ migration và không bật baseline.
- Phòng 1 thiếu 8 ghế A1–A7, B1 (availability ra 67): DB từng chạy `cinema_local_seed.sql` cũ, có ID ghế `TS000001`–`TS000008` trùng. Kiểm tra các ID đó đã thuộc `TM000001` trong cinema_db rồi chạy lại 03 để thêm phần còn thiếu.
- Lỗi SSMS cũ còn transaction mở thì rollback trong cửa sổ đó trước khi chạy lại.

## Tài khoản demo (B-04)

Mật khẩu không ghi trong repo; mỗi người tự giữ.

| Vai trò | Tài khoản | Nguồn | Claim trong token |
|---|---|---|---|
| ADMIN | `admin_demo` | Identity Flyway V2 (`Staff.ST000001`) | `roles=[ADMIN]`, `staffId=ST000001` |
| USER | Tài khoản demo tự đăng ký, ví dụ `demo_user` | `POST /api/v1/auth/register` qua Gateway | `roles=[USER]`, `customerId` dạng UUID |

Đăng ký USER qua Gateway. Username gồm 2–20 ký tự chữ, số hoặc `_`; mật khẩu ít nhất 6 ký tự; số điện thoại 9–11 chữ số:
```powershell
$body = @{ username='demo_user'; password='<mật khẩu>'; confirmPassword='<mật khẩu>'; email='demo_user@example.com'; phone='0900000001'; dob='2000-01-01'; sex=$true } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/register -ContentType 'application/json' -Body $body
```
Đăng nhập (`POST /api/v1/auth/login`), rồi gọi `GET /api/v1/auth/me` với token vừa nhận. Với tài khoản USER, trường `userId` trong kết quả chính là claim `customerId` của token, tức chủ của mọi hold và booking mà tài khoản này tạo. Mục B14 trong [04_reconcile.sql](../backup-restore/04_reconcile.sql) kiểm tra chủ hold và booking có trong `Customer`; mục I04, I05 kiểm tra còn ít nhất một ADMIN và một USER active.

Các khách cũ `CS000001`–`CS000008` cũng đăng nhập được nếu biết mật khẩu. Không dùng `staff1` (CASHIER_STAFF): role nhân viên nằm ngoài phạm vi 1.4.

## Chuyển cinema_db cũ sang Flyway

Từ 2026-11-06, cinema-service tạo schema bằng Flyway và chỉ kiểm tra schema (`ddl-auto: validate`). `cinema_db` cũ do Hibernate tạo (room_id dạng chữ, không có lịch sử Flyway) không chạy được V1, và không dùng baseline để bỏ qua (AGENTS mục 10). Cách chuyển: giữ DB cũ dưới tên khác, để Flyway tạo DB mới, rồi chép dữ liệu bằng 05. Script 05 chỉ đọc DB cũ, giữ nguyên mọi ID, chỉ chép phòng có ID số (bỏ R1) và liệt kê những dòng bị bỏ qua.

1. Dừng cinema-service. Backup: `BACKUP DATABASE cinema_db TO DISK = N'cinema_db_before_flyway.bak' WITH COPY_ONLY, INIT;`
2. Đổi tên DB cũ: `ALTER DATABASE cinema_db SET SINGLE_USER WITH ROLLBACK IMMEDIATE; ALTER DATABASE cinema_db MODIFY NAME = cinema_db_before_flyway; ALTER DATABASE cinema_db_before_flyway SET MULTI_USER;`
3. Tạo DB mới với tên file khác, vì DB cũ vẫn giữ file `cinema_db.mdf` (đường dẫn dưới đây là SQL Server trên Docker/Linux; trên Windows dùng thư mục DATA của instance):
   ```sql
   CREATE DATABASE cinema_db
   ON (NAME = cinema_db_flyway, FILENAME = '/var/opt/mssql/data/cinema_db_flyway.mdf')
   LOG ON (NAME = cinema_db_flyway_log, FILENAME = '/var/opt/mssql/data/cinema_db_flyway_log.ldf');
   ```
4. Tạo login và user `cinema_migrator`, `cinema_app` theo chú thích trong [cinema-local.properties.example](../../../cinema-local.properties.example). Chạy `SELECT DB_NAME()` để chắc đang ở `cinema_db` trước khi tạo user.
5. Trong file cấu hình mà cinema-service thực sự nạp (IntelliJ dùng `.idea/cinema-local.properties`), đặt `spring.datasource.username/password` của `cinema_app` và thêm `spring.flyway.url`, `spring.flyway.user`, `spring.flyway.password` của `cinema_migrator`. File `.example` chỉ là mẫu, không được nạp và không chứa mật khẩu thật.
6. Chạy cinema-service một lần; log phải có Flyway áp dụng V1 và V2.
7. Chạy `05_cinema_copy_to_flyway_db.sql`. Kết quả mong đợi: phòng có ID số được chép (1–3 với 75/80/80 ghế), danh sách bỏ qua gồm R1 và các suất của R1. Script từ chối chạy nếu cinema_db mới đã có dữ liệu.

Không chạy `02_cinema.sql` lên DB mới thay cho 05: ngày suất demo sẽ bị tính lại và lệch với `Sales_Snapshot` trong booking_db. Giữ `cinema_db_before_flyway` và file backup đến khi nghiệm thu xong. Muốn quay lại: dừng cinema-service, đổi tên `cinema_db` mới đi, đổi tên `cinema_db_before_flyway` về `cinema_db`, chạy code trước đợt chuyển Flyway.

Máy chưa từng có cinema_db thì bỏ qua mục này: tạo DB trống, chạy cinema-service, rồi chạy 02 (và 06 nếu cần phòng thử).
