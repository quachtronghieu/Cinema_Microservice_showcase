# Phạm vi công việc của Nguyễn Đào Thu Ngân

Phân công ghi nhận ngày 2026-11-05, thay phần việc của Ngân tại AGENTS.md mục 27. File này chỉ quy định phạm vi chức năng; kiến trúc, ownership, bảo mật, dữ liệu, Flyway và invariant vẫn theo AGENTS.md và docs/. Khi yêu cầu chức năng ở đây khác AGENTS.md (ví dụ phòng chỉ đọc và đổi trạng thái thay cho CRUD phòng), áp dụng file này. Lệnh, số liệu và bằng chứng test nằm ở [verification.md](verification.md).

## Task và tiến độ

| # | Task | Trạng thái | Còn lại |
|---|---|---|---|
| 1 | Manage Screening Rooms & Seat Layout | Đã nghiệm thu trên DB local 2026-11-06 | Thọ review phần sửa `ShowtimeService` |
| 2 | View Seat Map | Đã nghiệm thu trên app 2026-11-06 | S-04; xem phòng 3 khi có suất mở bán |
| 3 | Select / Deselect Seat | Đã nghiệm thu trên app 2026-11-06 | Không |
| 4 | Load Real Seat Availability | Đã nghiệm thu trên app 2026-11-06 | Không |
| 5 | Hold / Release Selected Seats | Đã nghiệm thu API và app 2026-11-06 | H-05; hai JVM thật (tùy chọn) |
| 6 | Show Hold Expiry & Conflict | Đã nghiệm thu trên app 2026-11-06 | Không |
| 7 | Finalize Demo Dataset, Backup / Restore 4 DBs | Đã nghiệm thu 2026-11-08; dataset 3 phòng (D-06) | Không |
| 8 | Centralize logs with ELK | Đã nghiệm thu 2026-11-09 trên DB local, bằng luồng thật qua Gateway | Không |

## Ràng buộc

- 3–4 phòng cố định, mapping Room–Template theo dữ liệu gốc. Không có API tạo, xóa, sửa phòng, đổi template hay sửa từng ghế; không xây trình thiết kế ghế.
- Dữ liệu gốc không mô tả lối đi; vị trí lối đi trong Flutter là quy ước hiển thị, chưa phải thiết kế đã duyệt (S-04).
- Mọi thay đổi trong bảng "Cần người dùng đồng ý" chỉ làm sau khi người dùng đồng ý. Tác động lên DB local phải báo trước; agent không tự chạy script ghi lên DB local.

## Quyết định đã chốt

| Mã | Quyết định | Ngày |
|---|---|---|
| D-01 | Luật cùng hàng, liền nhau, không để ghế lẻ chỉ kiểm tra ở Flutter (`ValidateSeatSelection`). Backend cũ không có và backend mới không thêm. Gọi thẳng API có thể giữ ghế rời nhau; chấp nhận vì không gây bán trùng ghế. | 2026-11-05 |
| D-02 | Backend luôn kiểm tra 1–8 ghế, ghế thuộc đúng suất, ghế còn trống, chủ hold, hạn hold, và giữ ghế nguyên tử. | 2026-11-05 |
| D-03 | Guest xem được availability; giữ ghế cần USER. Response công khai không lộ chủ hold. | 2026-11-05 |
| D-04 | Suất chiếu và ghế theo suất mới dùng UUID; ID cũ (SW..., HS...) giữ nguyên; tạo mới luôn là insert, trùng ID thì báo lỗi. | 2026-11-06 |
| D-06 | Dataset demo dùng 3 phòng (1–3, 235 ghế); không nhập phòng 4 (TM000004) và không thêm layout cho nó trong Flutter. | 2026-11-07 |
| D-05 | Đổi trạng thái phòng theo cách của Thọ (commit `878f705`): chỉ kiểm tra chiều ACTIVE → INACTIVE và chỉ xét suất đang chiếu hoặc bắt đầu trong 1 tháng tới. Thay quy tắc "toàn bộ lịch tương lai" chốt ngày 2026-11-05. Code và test đã có trên main (merge vào NganNDT 2026-11-08). | 2026-11-07 |

Sơ đồ vật lý do Cinema trả (`GET /api/v1/showtimes/{id}/seats`), trạng thái ghế do Booking trả (`GET /api/v1/showtimes/{id}/seat-availability`); Flutter ghép theo `templateSeatId`. Contract: [seat-holds.yaml](contracts/seat-holds.yaml), [rooms.yaml](contracts/rooms.yaml).

## Điều kiện đổi trạng thái phòng (chốt 2026-11-05, sửa theo D-05 ngày 2026-11-07)

- ACTIVE cho phép lập lịch và mở bán. INACTIVE không cho tạo suất, chuyển suất vào phòng hay mở bán (409 `ROOM_INACTIVE`). Trạng thái phòng không thay trạng thái bán vé.
- ACTIVE → INACTIVE bị chặn khi còn suất có `effectiveEndAt` sau thời điểm hiện tại và giờ bắt đầu trước thời điểm hiện tại + 1 tháng. `effectiveEndAt` = ngày chiếu + giờ kết thúc (đã gồm thời gian dọn phòng), cộng một ngày nếu suất qua nửa đêm. INACTIVE → ACTIVE luôn được phép.
- DRAFT, OPEN và CLOSED đều chặn; CLOSED chỉ là đóng bán, không phải hủy suất. Suất bắt đầu sau 1 tháng không chặn và vẫn nằm trong phòng INACTIVE; suất đó chưa mở bán thì không mở bán được (409 `ROOM_INACTIVE`), đã OPEN thì vẫn bán được vì Booking không kiểm tra trạng thái phòng. Đổi trạng thái không hủy suất, booking hay nhả ghế.
- Đổi trạng thái và tạo, sửa, chuyển phòng, mở bán khóa cùng dòng phòng trong transaction; nhiều phòng thì khóa theo thứ tự ổn định; không giữ khóa qua HTTP.
- Gửi lại đúng trạng thái hiện tại: 200, không ghi audit. Còn suất chặn: 409 `ROOM_HAS_UPCOMING_SHOWTIMES`. Body sai hoặc thừa field: 400. Không token 401, USER 403, phòng không tồn tại 404.
- Mỗi lần đổi thành công ghi audit (R-06): người đổi, phòng, trạng thái cũ và mới, giờ UTC, traceId. Chỉ ADMIN, kiểm tra ở cả Gateway và Cinema.

## Việc còn mở

- [ ] S-04 Đối chiếu layout lối đi trong [seat_layout_template.dart](../cinema_booking_app/lib/features/showtime_and_seat/presentation/widgets/seat_layout_template.dart) với thiết kế cho TM000001–TM000003 (TM000004 không dùng, D-06).
- [ ] Phòng 3 hiển thị đúng 80 ghế trên app. API đã trả đủ 80 ghế và có suất OPEN ở phòng 3 ngày 2026-11-10 lúc 19:00 (tạo khi nghiệm thu task 8); còn xem trên app.
- [ ] H-05 Cùng Vy kiểm tra giữ ghế rồi tạo đơn bằng API thật, thay `docs/dev/create_test_hold.sql` và màn debug. Có thể dùng [cinema-e2e.postman_collection.json](postman/cinema-e2e.postman_collection.json) của Vy; collection cần một suất OPEN trong hôm nay.
- [ ] Hold ở instance A, tạo đơn ở instance B với hai JVM thật (mới đạt hai Spring context trong cùng JVM).
- [x] B-04 Tài khoản USER/ADMIN demo: hướng dẫn trong [local-seed/README.md](dev/local-seed/README.md) (ADMIN `vyltt12`, USER đăng ký qua API); người dùng đã thực hiện 2026-11-08.
- [x] B-05 Script và hướng dẫn backup/restore 4 DB trên bản sao tại [dev/backup-restore/](dev/backup-restore/README.md): đạt trên SQL Server tạm và trên DB local 2026-11-08 ([verification.md](verification.md)).
- [x] B-06 Báo cáo đối soát `04_reconcile.sql` (chỉ đọc, 27 mục): đạt trên DB gốc và bản sao 2026-11-08.
- [x] B-07 Chốt 3 phòng ngày 2026-11-07 (D-06).
- [x] Nghiệm thu task 7 ngày 2026-11-08: dựng lại từ đầu bằng Flyway ra đúng số liệu (SQL Server tạm); bản sao khớp DB gốc và chạy được luồng ghế → hold qua Gateway (DB local); không xóa dữ liệu lịch sử.
- [x] K-01 traceId thống nhất: Thái đã thêm Micrometer tracing cho Cinema và Booking (`e8c4da6`), `TraceIdFilter` ưu tiên traceId phân tán. Đã merge vào NganNDT ngày 2026-11-09, toàn bộ test đạt.
- [x] K-02 ELK và log service (G-05): [docker-compose.elk.yml](../docker-compose.elk.yml), [elk/README.md](../elk/README.md), log sự kiện trong `SeatHoldService`; cả 6 ứng dụng đã gửi log vào ELK ngày 2026-11-09.
- [x] K-03 Rà soát ngày 2026-11-07. Booking, Cinema, Catalog và Gateway không log token, mật khẩu hay header Authorization. Identity có 2 vấn đề, chuyển Thịnh (bảng phụ thuộc). Cinema bật `show-sql: true`: không lộ tham số, nhưng làm nhiễu log khi đưa vào ELK; tắt khi làm K-02 (sửa cấu hình, cần đồng ý).
- [x] Nghiệm thu task 8 ngày 2026-11-09: Kibana tìm được log theo service, traceId, holdId và showtimeId của một lượt giữ và nhả ghế thật qua Gateway; có ảnh chụp Discover. Lần nghiệm thu này chạy trước khi có tracing của Thái, nên traceId khi đó chỉ trong phạm vi từng service.

## Đã hoàn thành

| Nhóm | Nội dung |
|---|---|
| Contract (C-01 đến C-04) | seat-holds.yaml, rooms.yaml, route mapping trong migration.md, sửa ghi chú lỗi thời trong verification.md |
| Phòng (R-01 đến R-08) | DTO có sức chứa, Room INT và ManyToOne, lỗi dạng chung, API template, `PATCH .../status`, audit (Flyway V2), Spring Security ở Cinema, khóa phòng và chặn phòng INACTIVE trong `ShowtimeService` |
| Sơ đồ ghế (S-01 đến S-03, S-05) | Đúng path và dạng response, sơ đồ đọc từ template kèm `templateId`, đọc `message` khi lỗi |
| Chọn ghế (L-01) | Test luật chọn ghế và view model |
| Availability (A-01 đến A-05) | API Booking công khai, Flutter ghép hai nguồn, hiểu HELD/SOLD, không hiện ghế trống giả khi lỗi |
| Giữ ghế (H-01 đến H-04) | API giữ và nhả lưu trong booking_db, thay hold nguyên tử, job nhả hold hết hạn |
| Hết hạn và xung đột (E-01 đến E-03) | Tải lại ghế với mọi 409, sửa ghi chú, test đếm ngược và xung đột |
| Dữ liệu (B-01 đến B-03) | Bổ sung 8 ghế thiếu ở phòng 1; seed dùng chung tại [docs/dev/local-seed/](dev/local-seed/README.md) thay `cinema_local_seed.sql`; ID UUID (D-04) |

## Cần người dùng đồng ý

| Mã | Thay đổi | Trạng thái |
|---|---|---|
| G-01 | Route availability ở Gateway sang Booking, đặt trước route Cinema | Đã đồng ý 2026-11-05, đã làm |
| G-02 | Mở công khai GET availability ở Booking | Đã đồng ý 2026-11-05, đã làm |
| G-03 | Cinema dùng Flyway, `ddl-auto: validate`, Room INT, tài khoản migrator/runtime riêng; `cinema_db` local phải chuyển theo README seed | Đã đồng ý 2026-11-06, đã làm và chuyển DB local |
| G-04 | Spring Security và JWT ở Cinema | Đã đồng ý 2026-11-06, đã làm |
| G-05 | Container và cấu hình log cho ELK | Đã đồng ý 2026-11-09, đã làm; Docker tăng lên 6 GB trong `.wslconfig` |

## Phụ thuộc người khác

| Người | Nội dung |
|---|---|
| Thịnh | Compose và Identity V3 (`fe2641b`): secret dạng rõ nằm trong comment V3, `.env.example` và giá trị mặc định của `compose.yaml` (AGENTS mục 7); đổi secret phải bằng migration mới. Identity còn log `Using generated security password` khi khởi động. |
| Thọ | Review phần sửa `ShowtimeService` (khóa phòng, `ROOM_INACTIVE`, ID UUID). |
| Thọ | `Cinema.Showtime_Seat` và `InternalCinemaSeatController` trái ownership; bỏ bằng migration mới khi Open Sales nối Booking. |
| Thái | Phòng thử `R1` đã được thay bằng phòng 901 (`06_cinema_test_room.sql`). |
| Vy | Kiểm tra chung bước tạo đơn từ hold (H-05). |
| Thịnh | K-03 (Forgot/Reset Password, Login): `PasswordResetService` log đầy đủ link reset kèm token thô ở mức INFO, nên ai đọc được log (sau này là ELK) đều đặt lại được mật khẩu; API forgot-password cũng trả `resetToken`. `identity-service/application.yml` có mật khẩu mặc định cho DB và mail (`SPRING_MAIL_PASSWORD`), giống app password Gmail thật: cần thu hồi và bỏ khỏi file. |
| Thịnh | Identity: `application.yml` không khai báo tài khoản Flyway, chỉ `deploy/compose.demo.yaml` truyền migrator qua `SPRING_FLYWAY_USER`; repo không có `identity-local.properties.example`; datasource mặc định `sa`; `baseline-on-migrate: true` trái AGENTS mục 22. Chạy local cần tự thêm migrator. Không chặn nghiệm thu task 7. |
| Thịnh | Task Docker Compose và Prometheus/Grafana: Dockerfile, compose của Cinema/Booking và cách thu log container thuộc Thịnh; ELK chỉ thêm file compose riêng sau khi thống nhất. |
| Thọ | `BookingClient` của Cinema vẫn dùng URL cố định `services.booking.url` thay cho discovery (AGENTS mục 8). Các vấn đề khác của commit `878f705` đã được Vy sửa ở `67c61ac` (service token cho `/api/v1/internal/**`, không nuốt lỗi, Booking xác nhận trước). |

## Dữ liệu phòng chính thức

Theo `data.sql` gốc. Dataset demo chỉ dùng phòng 1–3 (235 ghế), theo D-06; phòng 4 giữ trong bảng để truy vết dữ liệu gốc.

| Phòng | Template | Tên | Ghế | STANDARD / VIP | Hàng và số ghế |
|---|---|---|---|---|---|
| 1 | TM000001 | 2D | 75 | 21 / 54 | A–C: 1–7; D–I: 1–9 |
| 2 | TM000002 | 3D | 80 | 30 / 50 | A–H: 1–10 |
| 3 | TM000003 | 4D | 80 | 30 / 50 | A–H: 1–10 |
| 4 | TM000004 | 2D | 64 | 32 / 32 | A–H: 1–8 |

Giữ nguyên ID phòng, TM... và TS...; hai template cùng tên 2D vẫn là hai template khác nhau. 20 suất và 1.439 ghế theo suất của dữ liệu gốc đều đã qua ngày chiếu: không đổi ngày để mở bán lại, không tự coi `unavailable` là SOLD khi chưa đối chiếu booking và payment. Hold cũ không đủ thông tin chủ và hạn nên không dựng lại; hold mới tạo qua API.
