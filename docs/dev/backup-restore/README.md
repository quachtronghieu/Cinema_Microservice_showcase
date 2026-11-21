# Backup, restore và đối soát 4 database

Script cho task 7 (B-05, B-06 trong [ngan-task-scope.md](../../ngan-task-scope.md)): backup `identity_db`, `catalog_db`, `cinema_db`, `booking_db`, restore thành bản sao, rồi đối soát. Chỉ dùng cho local/demo.

Các script không sửa, không xóa dòng nào trong 4 DB gốc. Script 02 chỉ tạo DB mới và dừng ngay nếu DB đích đã tồn tại; không có `REPLACE`, nên không thể ghi đè DB gốc.

## Script

| File | Việc | Ghi gì |
|---|---|---|
| `00_provision_4db.sql` | Chỉ dùng khi dựng lại từ đầu trên SQL Server trống: tạo 4 DB và login migrator/app, mật khẩu truyền lúc chạy. Cần SQLCMD mode | 4 DB trống và 8 login |
| `01_backup_4db.sql` | Backup `COPY_ONLY` kèm `CHECKSUM`, rồi `RESTORE VERIFYONLY` cho 4 DB; in ra mã backup (`backup_tag`) | 4 file `.bak` và lịch sử backup trong msdb |
| `02_restore_as_copy.sql` | Restore 4 file thành `identity_db_restore`, `catalog_db_restore`, `cinema_db_restore`, `booking_db_restore`; file dữ liệu và log mang tên mới | 4 DB mới |
| `03_compare_copy.sql` | So từng bảng giữa DB gốc và bản sao: số dòng và checksum | Không ghi gì |
| `04_reconcile.sql` | Báo cáo đối soát: số dòng, kiểm tra tham chiếu và trạng thái, đếm trạng thái, tổng tiền. Cần SQLCMD mode | Không ghi gì |

Chạy trong SSMS, hoặc bằng `sqlcmd -I -i <file>`. Cờ `-I` bật `QUOTED_IDENTIFIER`, giống mặc định của SSMS; thiếu cờ này thì lệnh ghi vào bảng có cột tính toán bị từ chối.

Cần một login quản trị có quyền backup và tạo database (sysadmin, hoặc dbcreator kèm db_backupoperator). Không dùng login runtime như `cinema_app` hay login migrator: chúng không có quyền này, và AGENTS mục 3 không cho dùng chúng vào việc khác.

## Backup và restore trên bản sao

1. Dừng 4 service, hoặc ít nhất ngừng mọi thao tác ghi. Như vậy 4 file cùng mô tả một thời điểm, và tham chiếu giữa các DB (booking → cinema → catalog → identity) vẫn khớp.
2. Chạy `01_backup_4db.sql` và ghi lại `backup_tag`. Mặc định file nằm trong thư mục backup của SQL Server. Với SQL Server chạy trong Docker, đó là thư mục trong container (thường là `/var/opt/mssql/data`). Để giữ file ra ngoài máy:
   ```powershell
   docker cp <container>:/var/opt/mssql/data/booking_db_<tag>.bak D:\backup\
   ```
   Đặt file `.bak` ngoài thư mục repo. File của `identity_db` chứa hash mật khẩu và thông tin khách, không được commit.
3. Mở `02_restore_as_copy.sql`, đặt `@tag` bằng `backup_tag` vừa ghi lại, rồi chạy. Kết quả có 4 DB `*_restore` ở trạng thái `ONLINE`.
4. Chạy `03_compare_copy.sql`. Mọi dòng phải có `match = 1`, và `mismatches = 0`. Checksum chỉ là dấu vân tay nhanh, không phải bằng chứng tuyệt đối.
5. Bật SQLCMD mode trong SSMS (Query > SQLCMD Mode) rồi chạy `04_reconcile.sql` hai lần: một lần với `:setvar SUFFIX ""`, một lần với `:setvar SUFFIX "_restore"`. Hai lần phải ra cùng số dòng, cùng số đếm trạng thái và cùng tổng tiền.

Restore lên một máy khác cũng làm như vậy: chép file `.bak` vào thư mục backup của instance đó, chạy 02 với `@suffix = N''` nếu máy đó chưa có 4 DB. Script vẫn dừng nếu DB đã tồn tại. Login runtime và migrator (`cinema_app`, `cinema_migrator`...) phải được tạo trên instance đó (xem chú thích trong [cinema-local.properties.example](../../../cinema-local.properties.example)). User trong DB gắn với login theo SID, nên sau khi restore phải nối lại trong từng DB, ví dụ `USE cinema_db; ALTER USER cinema_app WITH LOGIN = cinema_app;`. Restore trên cùng instance thì không cần bước này.

## Đọc báo cáo đối soát

Kết quả thứ hai của `04_reconcile.sql` liệt kê từng mục kiểm tra, mỗi mục kèm tối đa 5 ID mẫu:

- Mục `ERROR` phải có `problems = 0` (cột result là `OK`). Có lỗi là dữ liệu sai invariant, ví dụ: ghế SOLD mà đơn không PAID, hold ACTIVE không khớp ActiveHold, tổng tiền khác tổng các dòng, đơn PAID thiếu payment hoặc vé, chủ hold không có trong identity, số ghế của suất khác template phòng.
- Mục `WARN` cần người xem, không tự sửa:
  - I06: tài khoản role cũ như `staff1` (CASHIER_STAFF) vẫn active. Theo AGENTS mục 1.1, tài khoản này cần bị vô hiệu hóa ở hệ đích; đó là việc của Identity.
  - B09: hold quá hạn, có khi chỉ vì service đang dừng.
  - B13: số voucher đã dùng là dữ liệu cũ.
  - C04: suất OPEN trong phòng INACTIVE, hệ quả của quyết định D-05.

Script đọc chéo 4 DB chỉ để đối soát; service không bao giờ làm vậy (AGENTS mục 3).

## Nghiệm thu task 7

**Restore chạy được luồng ghế → hold.** Trỏ Booking, Cinema, Catalog và Identity sang bản sao chỉ cho lần chạy này, bằng biến môi trường (không sửa file cấu hình). Ví dụ với booking-service:
```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://localhost:1433;databaseName=booking_db_restore;encrypt=true;trustServerCertificate=true'
$env:SPRING_FLYWAY_URL = $env:SPRING_DATASOURCE_URL
```
Flyway chỉ kiểm tra lịch sử đã có, không chạy lại migration. `application.yml` của Identity không khai báo tài khoản Flyway, nên Identity lấy migrator từ `SPRING_FLYWAY_USER` và `SPRING_FLYWAY_PASSWORD` (`deploy/compose.demo.yaml` đang làm vậy); không đặt thì Flyway dùng tài khoản datasource. Bản sao đã có đủ migration nên tài khoản app chỉ cần đọc lịch sử; DB trống thì bắt buộc dùng migrator (db_owner). Identity còn cần thêm tham số dòng lệnh `--spring.jpa.properties.hibernate.default_catalog=identity_db_restore`, vì `application.yml` của nó ghi cứng `default_catalog: identity_db`; thiếu tham số này thì Flyway kiểm tra bản sao nhưng các câu truy vấn vẫn đọc và ghi DB gốc.

Booking từ chối giữ ghế khi suất đã bắt đầu (409 `SHOWTIME_STARTED`), mà suất trong backup có thể đã qua giờ chiếu. Tìm suất còn dùng được:
```sql
SELECT showtime_id, room_id, show_date, start_time, sales_status FROM booking_db_restore.dbo.Sales_Snapshot ORDER BY show_date DESC;
```
Nếu không còn suất nào, chạy `03_compare_copy.sql` trước, rồi dời ngày một suất **chỉ trên bản sao**, ở cả hai DB (không làm trên DB gốc, và không chọn suất đã có booking):
```sql
DECLARE @d DATE = DATEADD(DAY, 2, CAST(GETDATE() AS DATE));
UPDATE cinema_db_restore.dbo.Showtime SET show_date = @d WHERE showtime_id = 'SW00023';
UPDATE booking_db_restore.dbo.Sales_Snapshot SET show_date = @d WHERE showtime_id = 'SW00023';
```
Sau đó:

1. Đăng nhập USER.
2. Gọi `GET /api/v1/showtimes/{id}/seat-availability`.
3. Gọi `POST /api/v1/customer/seat-holds`, rồi `DELETE /api/v1/customer/seat-holds/{holdId}`.

Mở terminal mới (hoặc xóa biến) để quay lại DB gốc. Xong việc, có thể xóa bản sao bằng `DROP DATABASE [<tên>_restore]`; đây là thao tác của người dùng, script không làm.

**Dựng lại từ đầu ra cùng số liệu.**

1. Trên một SQL Server trống, chạy `00_provision_4db.sql` để tạo 4 DB và 8 login (mỗi service một `<service>_migrator` có `db_owner` và một `<service>_app` chỉ đọc/ghi). Script dừng nếu một trong 4 DB đã tồn tại.
2. Chạy từng service một lần để Flyway tạo schema bằng tài khoản migrator. Booking, Catalog, Cinema đã khai báo sẵn tài khoản Flyway. Identity không khai báo trong `application.yml`, nên phải truyền `SPRING_FLYWAY_USER=identity_migrator` và `SPRING_FLYWAY_PASSWORD` (hoặc key `spring.flyway.user`, `spring.flyway.password` trong file local); thiếu thì Flyway dùng tài khoản datasource và không tạo được schema nếu đó là `identity_app`.
3. Chạy seed theo [local-seed/README.md](../local-seed/README.md): 01, 02, 03, và 06 nếu cần phòng thử.
4. Chạy `04_reconcile.sql`. Số liệu mong đợi:

| DB | Bảng | Số dòng |
|---|---|---|
| identity_db | Staff / Customer | 2 / 9 (Identity V2; tài khoản đăng ký thêm cộng vào Customer) |
| catalog_db | Movie | 13 (12 phim nguồn + `MV000016`) |
| cinema_db | Template / Room / Template_Seat | 3 / 3 / 235 (thêm 1 / 1 / 8 nếu chạy 06) |
| cinema_db | Showtime | 2 (`SW00023`, `SW00024`) |
| booking_db | Sales_Snapshot / Showtime_Seat | 2 / 155 |

Mọi mục ERROR phải OK. Không xóa dữ liệu lịch sử để đạt số liệu; số lệch phải giải thích được (ví dụ booking tạo thêm khi test).
