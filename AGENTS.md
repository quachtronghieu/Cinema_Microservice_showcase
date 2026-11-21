# Cinema Microservices - Quy ước kiến trúc và làm việc chung

Phiên bản: 1.4 - 01/11/2026. Vị trí: gốc repository của team.

Đây là chuẩn thiết kế để team đưa vào repo. Phương án 4 service ở mục 3 đã được người dùng lựa chọn và yêu cầu cập nhật trong phiên bản 1.2. Các quy tắc dưới đây áp dụng khi team tiếp nhận file này; chúng mô tả đích cần xây dựng, không khẳng định hệ thống đã triển khai. Người và AI cùng tuân thủ. Thay đổi kiến trúc phải qua quy trình ở mục 14.

## 1. Mục tiêu và cách đọc

Chuyển Cinema monolith thành hệ thống Spring Boot microservices phục vụ MSS301, giữ nghiệp vụ customer và quản trị theo mục 1.1, bảo đảm không bán trùng ghế và có demo chạy lặp lại được.

- **BẮT BUỘC**: mọi thành viên phải tuân thủ; lệch quy tắc cần quyết định kiến trúc được duyệt.
- **ĐÍCH THIẾT KẾ**: cấu trúc cần xây; chưa tồn tại đầy đủ trong source gốc.
- **HIỆN TRẠNG**: đã tìm thấy trong source/SQL; chưa đồng nghĩa đã chạy và kiểm thử.
- **CỔNG NGHIỆM THU**: điều kiện phải đạt trước khi chuyển mốc; không bỏ qua bằng ghi chú TODO.

Ưu tiên: bảo toàn dữ liệu và nghiệp vụ → demo chạy được → ranh giới service và bảo mật → khả năng quan sát → bonus. Không tự thêm tính năng, broker hoặc service ngoài phạm vi đã chốt.

Lộ trình đọc cho Agent: mục 3–7 là ranh giới/nghiệp vụ; mục 16 là công nghệ và setup; mục 17–18 là API nguồn và payload đã xác minh; mục 19–20 là schema và tương thích; mục 21 là cách bắt đầu task. Mục 22–26 chốt bốn tiêu chuẩn triển khai, nguồn và cổng kiểm chứng. Đọc cùng docs/ và reference/ trong bộ 1.3 để lấy hợp đồng máy đọc được và DDL đầy đủ; chỉ nhận riêng AGENTS không có nghĩa đã có toàn bộ file triển khai.

## 1.1. Phạm vi được chốt: Flutter customer và hai role

- Client chính là Flutter dành cho khách hàng, role `USER`. Tái sử dụng backend monolith và chuyển sang bốn service theo domain; không chia service theo role. Source Flutter chưa được cung cấp trong ZIP backend đã đọc: phải kiểm tra `pubspec.yaml`, lockfile, API client và màn hình thực tế trước khi pin Flutter/Dart hoặc kết luận tương thích.
- Backend đích chỉ cấp hai role người dùng: `USER` và `ADMIN`. ADMIN quản lý phim, concession, phòng, lịch chiếu, voucher và thực hiện check-in vé. Customer không được tự check-in, tạo dữ liệu quản trị hoặc nâng quyền.
- Không triển khai bán vé/thu tiền tại quầy, quản lý nhân viên nghiệp vụ hoặc phân ca trong phạm vi này. Không xây giao diện Flutter cho admin/nhân viên. React/Vite scaffold và Thymeleaf là nguồn/legacy, không phải yêu cầu xây frontend mới.
- ADMIN được demo bằng collection Postman hoặc Swagger có xác thực qua Gateway; phải có API thật và kiểm thử quyền, không thay bằng thao tác SQL trực tiếp. Chưa bắt buộc UI admin. Đối chiếu yêu cầu demo riêng của giảng viên trước nghiệm thu; tài liệu không bảo đảm điểm số.
- Giữ 4 service, 4 DB, CRUD quản trị cần thiết, JWT, discovery, gateway, config, circuit breaker, Docker và các invariant ghế/payment. Số role không làm thay đổi ranh giới service.
- `Staff` được giữ để chứa tài khoản ADMIN theo mô hình Principal hiện có. `Shift_Schedule` chỉ giữ dữ liệu/schema tương thích nếu cần; không có API ca làm mới. Dữ liệu staff/cashier/đơn quầy lịch sử phải sao lưu, đối soát và bảo tồn; không tự đổi nhân viên thành ADMIN, không tự xóa bảng/lịch sử. Chỉ tài khoản admin được xác minh mới nhận ADMIN; tài khoản nhân viên ngoài phạm vi bị vô hiệu hóa ở hệ đích.
- Các mục inventory controller/DDL nguồn mô tả monolith để truy vết, không tự động là danh sách API phải triển khai. Route cashier, ca làm và staff cũ phải được đánh dấu RETIRED trong bảng mapping, không publish qua Gateway và không tiếp tục ghi vào dữ liệu đã chuyển.
- Bộ docs/reference/OpenAPI/DDL 1.3 là tài liệu nền, chưa tự động được sửa theo bản này. Trước code/merge task bị ảnh hưởng phải đồng bộ role enum, security, route mapping, acceptance và ownership trong bộ kit. Không chạy DDL/seed cũ rồi mặc định đã phù hợp; migration đã áp dụng phải thêm phiên bản mới. Quyết định phạm vi này cần được ghi vào ADR trong repo.

## 2. Nguồn căn cứ và hiện trạng

Nguồn phân tích: Cinema_Management.zip, SQL.sql và DDL Watchlist người dùng bổ sung. ZIP chưa gắn với commit Git đã xác minh. Repo nguồn được chỉ định: TuongVy-dev/CinemaBookingApp; repo đích dự kiến: TuongVy-dev/CinemaManagementMicroService. Xác minh URL/branch thực tế khi thiết lập repo.

- Backend trong ZIP: Cinema_Management/, Maven, Java 21, Spring Boot 3.5.4, MVC/JPA/Security, SQL Server, REST và Thymeleaf; cổng hiện tại 8081.
- fe/ trong ZIP chứa React/Vite scaffold; chưa phải bằng chứng có UI Cinema hoàn chỉnh. Không giả định có frontend/ hoặc Flutter source trong ZIP.
- SQL.sql tạo Cinema_Management1 với 16 bảng, 18 foreign key. Watchlist có DDL bổ sung; chưa xác minh đã chạy thành công trên DB. Sau khi bổ sung thành công có 17 bảng trong phạm vi đã xét.
- Đã có hold 5 phút, tối đa 8 ghế/lượt; sau tạo booking có cửa sổ thanh toán mới 5 phút. Hai thời hạn độc lập.
- Hold và cache Idempotency-Key hiện ở RAM; có nhiều đường ghi booking/payment, gồm route MVC cũ. Chưa được kết luận an toàn với nhiều instance.
- Giữ TODO hiện có. Chưa có bằng chứng build, kiểm thử đồng thời, JWT, Docker Compose hoặc deployment trong đợt đánh giá tài liệu.

Dấu nhận diện nguồn: ZIP SHA-256 f9d6cceaf3f6f9be3d8ad243abe221147f510ad8e81a2e6d11a2641bd6af32c0; SQL SHA-256 00007c6d83649627be43f5de5d2300beb7c5590ed177ddeb9438f1d3a6bfc7a6.

## 3. Kiến trúc đích và quyền sở hữu

Đích giai đoạn đầu: **4 service nghiệp vụ + 3 ứng dụng hạ tầng**. Tách dần; mỗi mốc phải ghi phần đang ở legacy. Không tính gateway/discovery/config thay cho domain service.

| Service | DB và dữ liệu sở hữu duy nhất | Trách nhiệm |
|---|---|---|
| identity-service | identity_db: Customer, Staff, Shift_Schedule; account/role/token cần thiết | Đăng nhập, thông tin customer/admin, mật khẩu và role; Staff chứa tài khoản admin. Shift_Schedule chỉ phục vụ bảo tồn dữ liệu legacy, ngoài phạm vi API mới. |
| catalog-service | catalog_db: Movie, Movie_Rating, Watchlist, Concession | Phim, đánh giá, watchlist; module concession quản lý danh mục đồ ăn/uống và giá hiện hành. userId/customerId chỉ là ID ngoài domain. Chưa thêm tồn kho khi chưa có yêu cầu. |
| cinema-service | cinema_db: Room, Template, Template_Seat, Showtime | Phòng, sơ đồ ghế vật lý, lịch chiếu, kiểm tra trùng lịch phòng. |
| booking-service | booking_db: Booking, Booking_Detail, Showtime_Seat, Payment, Ticket, Voucher; bảng hold/idempotency/snapshot mới | Giữ và bán ghế theo suất, tính tiền, voucher, thanh toán demo, phát vé, check-in, hủy/hết hạn. |

Hạ tầng: api-gateway (8080), discovery-server (8761), config-server (8888).
Cổng local đích: identity 8081, catalog 8082, cinema 8083, booking 8084. Legacy giữ 8090 khi cùng chạy. Cổng là quy ước phát triển; routing dùng tên service, không hardcode cổng trong Java/client.

BẮT BUỘC:
- Chỉ service sở hữu được ghi dữ liệu của domain đó. Không đọc bảng, dùng repository/entity, join hoặc foreign key xuyên service.
- Có thể dùng một SQL Server instance cho demo với đúng 4 database: identity_db, catalog_db, cinema_db, booking_db và quyền riêng từng service. Không dùng tài khoản sa cho runtime service.
- Ghế vật lý thuộc cinema; khả dụng/hold/sold theo suất thuộc booking. Cinema không tự đổi trạng thái ghế đang bán.
- Payment, Ticket, Voucher ở cùng booking trong phiên bản này để giữ transaction cục bộ. Tách payment hoặc promotion cần ADR và workflow lỗi/đối soát đã thiết kế.
- Không dùng shared JPA entity hoặc module chứa business service của nhiều domain. Mỗi service triển khai API độc lập.

### 3.1. Quyết định kiến trúc 4 service — ACCEPTED, 29/10/2026

Người dùng đã lựa chọn phương án này và yêu cầu cập nhật tài liệu. Quyết định thay thế phạm vi 6 service ở bản 1.1 nhằm giảm khối lượng cấu hình, migration và giao tiếp HTTP trong đồ án. Bốn service đích là identity, catalog, cinema, booking; không dùng cách chia identity/catalog/booking/payment trong backlog cũ.

- Workforce là module legacy ngoài phạm vi triển khai 1.4, dữ liệu vẫn thuộc Identity; Concession là module nội bộ Catalog. Mỗi module có application logic, DTO, repository và kiểm thử rõ ràng, nhưng dùng deployment, cấu hình và DB của service sở hữu. Không tạo thêm ứng dụng Spring Boot, service discovery name hoặc cổng riêng cho hai module này.
- Payment, Ticket, Voucher ở Booking; cập nhật trạng thái liên quan trong transaction cục bộ khi phù hợp. Khi dùng nhà cung cấp thanh toán bên ngoài vẫn phải xác minh callback, chống gửi lặp và đối soát; transaction DB không bao trùm giao dịch ngân hàng.
- Cinema tiếp tục sở hữu lịch/phòng/sơ đồ vật lý. Booking sở hữu inventory theo suất; workflow publish/close và snapshot ở mục 4 vẫn bắt buộc.
- Bốn DB độc lập theo ownership; không tạo payment_db, concession_db hoặc workforce_db cho kiến trúc đích này. Không xóa DB cũ nếu đã có dữ liệu: backup, đối soát và chuyển một nguồn ghi theo mục 10 trước khi ngừng sử dụng.
- Tách thêm service trong tương lai cần ADR mới và nhu cầu cụ thể; không tự tách chỉ vì có module riêng. Quyết định được lưu trong docs/adr/0001-four-domain-services.md của bộ 1.3; đưa file đó cùng AGENTS vào repo.
- Backlog 4 service bản 1.2 là nền giao việc; áp dụng thêm cổng contract/schema/security ở bản 1.3 trước task phụ thuộc. Task ca làm của backlog cũ ngoài phạm vi 1.4; dữ liệu vẫn thuộc Identity, danh mục concession thuộc Catalog, phòng/lịch chiếu thuộc Cinema, payment thuộc Booking. Bản 1.4 này chưa chỉnh lại Excel; không đánh dấu task implementation hoàn tất chỉ vì có tài liệu/DDL.

## 4. Phụ thuộc và đồng bộ giữa domain

Luồng gọi mặc định: client → gateway → service sở hữu. Service chỉ gọi API đã công bố. Gateway xác thực/routing, không chứa nghiệp vụ giữ ghế hoặc tính tiền.

Phụ thuộc nghiệp vụ trực tiếp được phép:
- cinema → catalog: lấy metadata phim cần cho lập lịch.
- booking → cinema: lấy snapshot suất/sơ đồ trước transaction giữ ghế; booking → catalog: lấy quote danh mục/giá concession trước transaction tạo đơn.
- catalog → identity chỉ nếu nghiệp vụ admin cần xác minh account, không gọi trên mọi request đã có JWT hợp lệ.
- Workforce chỉ còn dữ liệu legacy ngoài phạm vi API mới; Identity không gọi HTTP về chính service. Module concession nằm trong catalog-service; client booking gọi API catalog để lấy giá, không truy cập catalog_db.
- booking không gọi identity trên mỗi thao tác mua; danh tính lấy từ token đã xác minh.

Cinema và booking phối hợp bằng workflow được điều phối từ lớp ứng dụng gateway-facing của cinema, gọi API nội bộ của booking. Endpoint booking nhận snapshot không được gọi ngược cinema trong cùng workflow. Các truy vấn snapshot từ booking đến cinema không được kích hoạt callback ngược booking. Không tạo chuỗi gọi vòng.

Quy trình suất chiếu:
1. Cinema tạo DRAFT; chưa mở bán, snapshot có version và định danh ghế ổn định.
2. Cinema gửi thao tác khởi tạo inventory idempotent tới booking. Booking lưu snapshot + inventory + trạng thái sales trong transaction cục bộ.
3. Sau xác nhận, cinema hiển thị suất đã publish. Booking là nơi quyết định có được nhận hold hay không; nếu mất phản hồi, retry cùng version/key để hội tụ.
4. Đóng bán: booking đóng sales trước và serialize với hold/payment. Sau xác nhận, cinema mới sửa trạng thái hiển thị. Nếu bước sau lỗi, giữ trạng thái an toàn và retry/đối soát.
5. Khi đã mở bán, không đổi phim/phòng/sơ đồ/giờ tùy ý. Thay đổi có booking cần workflow riêng được duyệt. Hủy suất có payment cần kế hoạch xử lý khách/refund trước khi triển khai.

Không giữ khóa SQL khi chờ HTTP. Giao tiếp ngoài domain không có distributed ACID tự động. Snapshot cần dữ liệu đủ để hiển thị booking/ticket mà không phải gọi nhiều service trong lúc check-in.

## 5. Luồng mua vé và invariant bắt buộc

### 5.1. Hold và tạo booking

- Mọi kênh customer/cashier/MVC/admin dùng cùng invariant của booking-service. Chuyển hoặc ngừng các route cũ trước khi mở traffic tới domain mới.
- Inventory có unique(showtimeId, templateSeatId). Constraint này ngăn tạo inventory lặp; khóa/CAS kiểm tra trạng thái mới ngăn giữ/bán trùng.
- Hold có ID, customerId, showtimeId, danh sách ghế, expiresAt và trạng thái, lưu trong booking_db. Idempotency cũng lưu bền vững ở booking_db.
- Giữ 1–8 ghế trong 5 phút; một khách có tối đa một hold đang hoạt động cho cùng suất như hành vi gốc. Đổi hold phải nguyên tử.
- Khóa các ghế theo thứ tự ổn định hoặc conditional update có kiểm tra số dòng. Thành công toàn bộ hoặc rollback toàn bộ.
- Tạo booking kiểm tra owner, đúng suất, đúng tập ghế, hold còn hạn; consume hold và tạo booking trong cùng transaction.
- Hết hold thì chỉ nhả ghế thuộc đúng hold hiện tại. Hết booking thì chỉ nhả ghế thuộc booking đó và còn chờ thanh toán. Không nhả ghế đã sold hoặc chuyển chủ sở hữu.
- Job hết hạn đọc DB dùng chung và cập nhật có điều kiện. Không dùng quét PENDING lúc startup để suy ra “mồ côi” từ RAM rỗng.
- Không coi synchronized, cache RAM hoặc sticky session là cơ chế bảo đảm nhất quán nhiều instance.

### 5.2. Trạng thái và thời hạn

Đích chuẩn nội bộ/API: inventory AVAILABLE → HELD → SOLD; hold ACTIVE → CONSUMED/RELEASED/EXPIRED; booking PENDING_PAYMENT → PAID/CANCELLED/EXPIRED; payment PENDING → SUCCEEDED/FAILED. Terminal state không được chuyển lùi tùy ý.

Đây là tên trạng thái đích, cần adapter và migration từ BOOKED/PENDING/UNAVAILABLE/paid/success trong nguồn. Không đổi enum rồi giả định DB/client tự hiểu. Tạo bảng ánh xạ status được review trước khi chuyển dữ liệu.

Cửa sổ thanh toán customer là 5 phút kể từ tạo booking, tách khỏi thời hạn hold. Mặc định áp dụng cùng thời hạn cho booking cashier chưa trả tiền, ghi channel CUSTOMER/CASHIER và expiresAt tường minh; chỉ là chính sách tham chiếu cho dữ liệu legacy; không xây UI/API quầy mới trong bản 1.4. Thay policy qua ADR, không tự đổi scheduler riêng lẻ.

Payment, hủy và job expiry phải khóa/conditional update cùng booking. Hai thao tác cạnh tranh chỉ có một kết quả cuối hợp lệ. Đồng hồ và dữ liệu thời gian mới lưu UTC, API ISO-8601 có offset. Dữ liệu datetime cũ không có zone: xác minh timezone nguồn và lưu quy tắc chuyển đổi trước migration.

### 5.3. Giá, voucher, payment, ticket

- Giá do backend tính bằng BigDecimal/DECIMAL; currency VND trong phiên bản này. Snapshot tên/giá món, phim, phòng, ghế và suất tại lúc booking. Giữ scale 2, HALF_UP ở biên tính tổng/giảm giá trong phiên bản này và ghi vào contract quote; không dùng double để cộng tiền. Đổi quy tắc cần ADR.
- Voucher dùng quota theo booking, reserve/consume hoặc hoàn lượt trong transaction có khóa/CAS; không đọc used_count rồi cộng/trừ thiếu đồng bộ. Hủy lặp không hoàn nhiều lần.
- Mặc định một payment record logic và một ticket chứa nhóm ghế cho mỗi booking, phù hợp cách API hiện tìm theo booking. Nếu thêm payment attempts, lưu bảng riêng với provider transaction ID unique.
- SQL hiện có hai bookingId gắn nhiều ticket: phải đối soát, xác định bản ghi hiệu lực và lưu lịch sử; không xóa tự động để ép unique. Chặn mốc migration Ticket đến khi có phương án dữ liệu được review.
- Customer trả tiền thành công thì phát ticket chưa check-in. Hành vi quầy cũ tự check-in chỉ được ghi nhận khi đối soát lịch sử; không triển khai adapter bán quầy mới. Vé online chỉ được ADMIN check-in bằng command xác thực, chống lặp và có audit; customer chỉ xem vé của mình.
- Check-in thực hiện bằng command cập nhật nguyên tử; lần đầu thành công, lần lặp không phát sinh check-in mới và trả 409 TICKET_ALREADY_CHECKED_IN.
- Simulator nhận SUCCESS/FAILED chỉ bật profile demo, mặc định tắt ngoài demo. Trang GET/redirect không được thay đổi payment thành công. Thanh toán thật cần xác minh provider, amount, currency, merchant/order và chống callback lặp.
- Nếu tiền thật đến sau expiry, đưa vào RECONCILIATION_REQUIRED ở workflow đối soát; không lấy lại ghế của người khác. Chưa có provider thật thì ghi rõ demo, không tuyên bố tích hợp ngân hàng.

### 5.4. Idempotency bền vững

BẮT BUỘC cho create booking và command payment có thể retry. Key gắn với (actorId, operation, key), request hash chuẩn hóa, trạng thái xử lý và kết quả/ID tài nguyên. Có unique constraint; ghi kết quả cùng transaction tạo tài nguyên.

Cùng key/cùng payload trả lại kết quả đã ghi; cùng key/khác payload trả 409 IDEMPOTENCY_KEY_REUSED. Yêu cầu đang xử lý trả trạng thái theo contract, không chạy song song lại. Retention replay tối thiểu 24 giờ trong phiên bản này, ghi trong OpenAPI; key đã hết hạn không được hứa replay vô thời hạn. Callback provider dedup bằng transaction/event ID và giữ liên kết payment lâu dài.

## 6. Quy ước API và client

- Flutter → Gateway → domain API; không chứa credential DB/service hoặc gọi trực tiếp cổng service. Đăng nhập lấy JWT USER, gửi Bearer token; không dùng session cookie làm cơ chế xác thực API đích. Chỉ sửa lớp client/repository theo contract sau khi đã đọc source Flutter; không giả định chỉ đổi base URL là đủ.
- Phải có mapping method/path/request/response/lỗi/status từ API customer cũ sang mới. Giữ tương thích qua adapter khi cần, loại bỏ adapter sau khi client đã chuyển và test thành công.
- Token lưu theo cơ chế an toàn phù hợp nền tảng Flutter đã chọn; không log token. 401: xóa phiên local, yêu cầu đăng nhập; 403: báo thiếu quyền, không tự retry/nâng quyền. Logout xóa token và dữ liệu riêng của phiên; JWT hết hạn sau 15 phút, chưa có refresh tự động.
- Hold hết hạn/ghế xung đột phải tải lại availability; retry command dùng cùng Idempotency-Key cho cùng thao tác và payload. Không tự gửi lại payment bằng key mới khi timeout; tra trạng thái đơn trước. Giá/role/owner từ client không là nguồn tin cậy.


- Đích mới dùng /api/v1/; OpenAPI là hợp đồng cho path, payload, role, status và ví dụ lỗi. Tạo contract trước khi producer/consumer code song song.
- Giữ adapter các API customer hiện có trong lúc chuyển: /api/customer/seat-holds, /api/customer/bookings/preview, /api/customer/bookings, /api/customer/bookings/{id}/demo-payment, tickets và watchlist theo controller gốc. Không đổi client đồng loạt khi chưa có bảng route mapping và test tương thích.
- API trả DTO, không trả entity/lazy proxy. Input có validation; list dùng page/size và sort được định nghĩa, size tối đa 100.
- ID domain hiện có giữ nguyên để migration; ID mới dùng UUID lowercase, lưu VARCHAR(36) cho khóa chuỗi; giữ Room INT IDENTITY và Watchlist BIGINT IDENTITY như schema đích. Cấm max(id)+1. ID ngoài API là chuỗi; Room Java/DB cần thống nhất Integer cho khóa vật lý INT IDENTITY và chuyển thành chuỗi ở adapter API nếu cần giữ tương thích.
- HTTP: 200 đọc/cập nhật/replay; 201 tạo mới; 204 xóa/nhả thành công; 400 input; 401 thiếu/sai token; 403 thiếu quyền/không sở hữu; 404 không tồn tại; 409 xung đột trạng thái; 503 phụ thuộc không sẵn sàng.
- Error JSON thống nhất: code, message, traceId, fieldErrors (object, rỗng nếu không có). Mỗi API service có global exception handler; không trả stacktrace/SQL/secret. Security errors dùng cùng hợp đồng.
- Client lấy API base URL từ cấu hình gateway. Không trực tiếp gọi domain service hoặc hardcode localhost ở production.
- Watchlist PUT/DELETE idempotent, unique(customerId, movieId). Khi tách catalog_db, bỏ FK Customer xuyên domain, giữ movie FK cùng catalog nếu phù hợp.

## 7. Security dùng chung

Đích phiên bản này: identity-service cấp JWT ký bất đối xứng; gateway và API service là Resource Server xác minh bằng public key/JWKS. Private key chỉ tại identity, đưa vào môi trường/secret store. Không chia sẻ signing secret cho tất cả service.

- Token có sub ổn định, issuer, audience và exp; kiểm tra signature, algorithm, thời hạn, issuer và audience. Không phân loại account theo chữ hoa username.
- Claim roles chỉ dùng USER, ADMIN; converter ánh xạ sang ROLE_ tương ứng. Không thêm CUSTOMER làm tên đồng nghĩa tự phát.
- Thống nhất định danh sub giữa Customer và Staff; migration kiểm tra username trùng hai bảng. Giữ ID cũ bằng mapping nếu cần.
- Caller ID và staffId nghiệp vụ lấy từ principal đã xác minh; service kiểm tra cả role và ownership. ADMIN override phải có endpoint/quyền và log rõ.
- API nội bộ xác thực bằng service credential/token có audience/scope riêng. Khi chuyển tiếp danh tính user, vẫn kiểm tra quyền command tại service nhận.
- BCrypt cho password mới; xử lý ba bản ghi Customer không có định dạng BCrypt và bỏ default plaintext trong SQL. Không tự hash lại chuỗi đã hash.
- Route admin cụ thể đứng trước route wildcard tổng quát. Legacy session/Thymeleaf giữ CSRF phù hợp; stateless bearer API cấu hình security chain riêng.
- Không commit datasource/mail password, key, token hoặc .env. CORS theo danh sách origin. Actuator nhạy cảm, Eureka, Config và DB chỉ trong mạng nội bộ/quyền admin.

Ma trận quyền tối thiểu (ngoài role còn phải kiểm tra ownership và phạm vi dữ liệu):

| Vai trò | Quyền mặc định |
|---|---|
| Guest | Đọc phim/suất công khai qua DTO; không sửa dữ liệu hoặc tạo booking. |
| USER | Hồ sơ của mình; hold/booking/payment/ticket/watchlist của mình; đọc concession; rating hợp lệ theo contract. |
| ADMIN | CRUD quản trị domain theo API được công bố, quản lý tài khoản admin có kiểm soát và check-in vé; không trực tiếp ép ghế sold hoặc payment thành công bỏ qua workflow. |

Catalog, concession, room/template, lịch chiếu, voucher và hồ sơ phải có CRUD nơi nghiệp vụ yêu cầu. Quản lý ca làm và nhân viên nghiệp vụ ngoài phạm vi. Booking/payment/ticket dùng command chuyển trạng thái và giữ lịch sử, không mở DELETE tùy ý chỉ để đủ CRUD.

## 8. Spring Cloud và resilience

BẮT BUỘC: Eureka Discovery, Gateway + LoadBalancer, Config Server external config và Resilience4j ở lời gọi remote phù hợp. Dùng @FeignClient theo tên logic; không cấu hình URL cố định làm mất discovery ngoài profile test.

- Baseline Java 21/MVC/JPA/Maven; không chuyển toàn bộ sang reactive. Gateway là ứng dụng riêng, chọn đúng starter theo BOM.
- Bộ 1.3 pin parent tham chiếu Boot 3.5.4 + Cloud BOM 2025.0.0, Java 21, JDBC 12.10.1.jre11 tại reference/build/pom.xml. Bootstrap tích hợp thành parent thật và kiểm chứng build/dependency tree chung trước code phụ thuộc. Đây là baseline chuyển đổi, không khẳng định patch còn được hỗ trợ/bảo mật production; dependency review và nâng đồng bộ trước public deployment. Nâng major cần ADR.
- Config Server khởi động được từ Docker DNS/env cố định; tránh vòng bootstrap Config cần Eureka trong khi Eureka cần Config để tìm nhau. Secret không lưu Git config.
- Mặc định HTTP connect timeout 2s, read timeout 3s cho đọc nội bộ; override cần contract. Ban đầu không tự retry ghi; chỉ bật sau khi kiểm chứng idempotency. Circuit breaker không thay timeout.
- Đọc catalog có thể dùng snapshot/cache đã đánh dấu stale. Không dùng giá 0, ghế AVAILABLE giả hoặc payment thành công giả làm fallback. Ghi không chắc kết quả phải đối soát/replay bằng key.
- Mốc đầu dùng REST + transaction cục bộ. Chỉ thêm async khi có ADR: event schema/version, outbox cùng transaction, consumer dedup, retry/DLQ, ordering và reconciliation. Không hứa exactly-once toàn hệ thống.

## 9. Cấu trúc repository đích và chuẩn code

Các đường dẫn dưới đây là đích cần tạo dần, chưa khẳng định có sẵn:

- AGENTS.md ở root; pom.xml là parent/aggregator, chỉ chia sẻ dependency/build convention.
- services/{identity,catalog,cinema,booking}-service/.
- infrastructure/{api-gateway,discovery-server,config-server}/.
- config-repo/ chứa config an toàn hoặc tham chiếu Git config riêng được ghi trong README.
- docs/adr/, docs/contracts/, docs/migration.md, docs/demo.md, docs/ownership.md.
- compose.yaml, .env.example ở root; legacy/ cho source monolith khi đến mốc di chuyển.
- Mỗi service: src/main/java/vn/edu/fpt/cinema/<domain>/ và các package controller, dto, application/service, domain/entity, repository, client, config, exception; không tạo nhiều tầng interface rỗng chỉ để đủ pattern.
- Migration thuộc src/main/resources/db/migration/ của service sở hữu; seed demo tách profile. SQLServer-specific migration phải kiểm chứng trên SQL Server.

Trong identity-service, không triển khai package workforce mới trong phạm vi 1.4; trong catalog-service, dùng package concession cho đồ ăn/uống, tách khỏi movie/rating/watchlist. Mỗi package có thể chứa controller, dto, service, repository theo nhu cầu. Các package gọi application service của nhau khi cần, không mở HTTP loopback hoặc tạo shared entity xuyên service.

Constructor injection; controller mỏng; business logic trong application/domain; repository chỉ thuộc service. DTO request/response riêng, tiền BigDecimal, log có traceId và ID nghiệp vụ, không log password/token/thông tin nhạy cảm.

Không tạo thư viện chung chứa entity/domain logic/DTO buộc deploy đồng thời. Nếu chia sẻ error/security utility, version độc lập và giữ contract wire tương thích.

## 10. Migration dữ liệu và điểm phải xử lý

Dùng Flyway versioned migrations; ddl-auto=validate ngoài test. Không sửa migration đã áp dụng; thêm version mới. Không để Hibernate tự update schema khi demo/deploy.

Cổng baseline:
- Đưa Watchlist DDL đã bổ sung vào migration; xác minh tạo bảng/unique/FK thành công. Khi tách DB chỉ giữ FK trong domain.
- Sửa Room INT/String và chốt Room–Template nhiều-một làm mặc định vì nhiều phòng có thể dùng cùng mẫu; giữ template cố định/snapshot cho suất đã mở bán.
- Đối soát ticket lặp; chuẩn hóa status, password, Payment DEFAULT SUCCESS thành PENDING; kiểm tra unique inventory/seat layout và toàn vẹn dữ liệu.
- Giữ computed total_price hoặc tính tương đương từ quantity/unit_price; dữ liệu thương mại lịch sử không phụ thuộc giá hiện hành.
- Bỏ đường dẫn D:\SQLData trong script tạo DB cho Docker/Linux; tách DDL, migration, seed đã làm sạch.

Mỗi domain có kế hoạch: backup → dừng ghi domain → export/import → so số dòng/ID/tổng tiền/trạng thái và tham chiếu → chuyển một nguồn ghi → smoke test. Không dual-write tự phát. Sau có dữ liệu mới, rollback phải đối soát/sync ngược trước đổi route. Không xóa source/DB legacy đến khi qua nghiệm thu domain.

Migrations chỉ FK nội bộ. Dữ liệu ngoài domain dùng ID và snapshot; API/read model thay join cũ. Hủy/soft-delete dữ liệu đã tham gia giao dịch; không cascade xóa lịch sử đặt vé/thanh toán/vé.

## 11. Kế hoạch triển khai theo cổng

| Mốc | Phạm vi | Điều kiện qua mốc |
|---|---|---|
| M0 | Baseline, parent POM, inventory route, owner và migration | Build gốc; DB từ script chuẩn; kiểm tra source Flutter và pin môi trường client thực tế; xử lý blockers mục 10. |
| M1 | Chuẩn hóa core legacy | Mọi route dùng invariant chung; hold/idempotency bền vững; ID an toàn; test cạnh tranh và payment/expiry. |
| M2 | Config, Discovery, Gateway và Compose | Legacy qua gateway; health/readiness; không vòng bootstrap; token flow có kế hoạch tương thích session. |
| M3 | Catalog (gồm concession), identity (customer/admin) | Hai DB identity/catalog riêng; contract, hai role và danh mục chạy được; adapter client đã kiểm thử; identity mapping hoàn tất. |
| M4 | Cinema và booking | Publish/close workflow, snapshot, mua vé, voucher, payment, ticket, check-in qua test. |
| M5 | Tích hợp đầy đủ 4 service và client thật | Bao phủ nghiệp vụ customer/admin theo mục 1.1; không còn route ghi legacy chưa kiểm soát; client qua gateway. |
| M6 | Deploy/demo và bonus theo ưu tiên | Chạy từ môi trường sạch; dữ liệu giữ sau restart; URL demo, giới hạn và bằng chứng nghiệm thu. |

Trong một mốc có thể chia việc song song sau khi contract/owner được duyệt. Không mở nhiệm vụ phụ thuộc vào schema/API chưa chốt. Nếu một domain tạm ở legacy, ghi trong migration.md và route mapping; không gọi đó là hoàn tất toàn bộ microservices.

## 12. Kiểm thử và Definition of Done

Với mỗi thay đổi code, chạy unit test liên quan, build service và integration/contract test bị ảnh hưởng. Với invariant ghế/payment phải có test DB thật tương thích SQL Server; mock không chứng minh locking. Frontend chỉ chạy lint/build tại thư mục client thật đã xác minh.

Các ca chặn nghiệm thu:
- Nhiều customer qua hai instance tranh cùng ghế (route cashier/MVC cũ phải bị chặn sau cutover): đúng một người giữ/bán; request thua trả 409, không tạo đơn bán trùng.
- Nhiều đơn ghế khác nhau: không va chạm ID; nhiều ghế trong một yêu cầu không giữ một phần.
- Hai instance: hold ở A, booking ở B; restart B không nhả hold của A; retry key sau restart không tạo tài nguyên lặp trong retention window.
- Payment/hủy/expiry cạnh tranh; callback giả/lặp/sai tiền; job nhả ghế trễ; quota voucher gần hết được dùng đồng thời.
- Ticket trùng và check-in đồng thời; sửa/hủy suất đã bán; thiếu snapshot inventory thì không mở bán.
- Token giả/hết hạn, role sai, đúng role nhưng sai owner; gọi thẳng service vẫn bị chặn.
- Dependency lỗi: timeout/circuit breaker/fallback đúng hợp đồng; Config/Discovery outage và restart không làm hỏng dữ liệu.
- Migration DB sạch, seed demo, restore/recovery và route tương thích được kiểm tra.

Mỗi PR phải có: lý do và phạm vi, service/schema/API bị ảnh hưởng, migration và rollback nếu có, lệnh kiểm chứng + kết quả, giới hạn chưa chạy, reviewer. Lỗi có sẵn ghi riêng, không gán nhãn “tests pass” khi chưa chạy.

Hoàn thành hệ thống khi: 4 domain service đúng ownership; không đọc DB chéo; bảo mật và invariant qua test; Spring Cloud hoạt động; mỗi ứng dụng có Dockerfile; Compose dựng DB/migration/seed và hệ thống; client thật qua gateway; deployment reachable và demo.md tái hiện được. Tất cả ô nghiệm thu phải có bằng chứng, không đánh dấu từ dependency hoặc annotation.

## 13. Docker, deployment và phạm vi môn học

Mỗi ứng dụng runnable có Dockerfile, health check, log và shutdown phù hợp. Compose dùng network/DNS service name; không dùng localhost cho DB ở container khác. Volume cho SQL Server, migration/seed idempotent theo môi trường; readiness phải chờ DB và migration, không chỉ process started.

Chỉ expose gateway/client cho luồng người dùng; cổng debug/admin chỉ mở ở profile dev. Không container nào phụ thuộc đường dẫn file cá nhân. Có .env.example, lệnh build/start/stop trong docs/demo.md và hướng dẫn Windows.

Bắt buộc demo E2E bằng Docker chạy được. Theo rubric chính thức, production deployment là bonus; nếu public thì dùng HTTPS và URL demo, ghi giới hạn cold start/RAM/persistence/số instance. Chỉ chốt nhà cung cấp sau khi kiểm tra tài nguyên/chi phí; không cam kết toàn bộ hệ thống miễn phí hoặc production-ready.

Rubric chính thức MSS301_assignment_grading_criteria.xlsx, sheet Criteria FA26: kiến trúc/nghiệp vụ 40% (domain 10%, DB 10%, business API 20%); Spring Cloud 35% (Eureka 10%, Gateway 10%, Config 10%, CircuitBreaker 5%); Docker 15%; Security 10%. Bonus mỗi mục 10%: Compose, production deployment, tracing/Actuator, CI/CD, Prometheus/Grafana, ELK. Không tự suy ra tổng/trần điểm bonus. Team chọn Compose là yêu cầu nội bộ. Chi tiết bằng chứng và mức trừ điểm ở docs/acceptance.md; mỗi thành viên phải hiểu và giải thích được code mình dùng.

Ưu tiên bonus: CI/health/tracing cơ bản → metrics → logging tập trung; không đưa Kubernetes/service mesh/ELK vào critical path khi core chưa đạt.

## 14. Phối hợp team và kiểm soát thay đổi kiến trúc

Trước khi chia việc, ghi trong docs/ownership.md: một owner và một reviewer cho từng domain; một người chịu trách nhiệm bootstrap/hạ tầng và một người điều phối tích hợp. Điền theo tên thành viên thực tế, không đoán người phụ trách. Dùng CODEOWNERS nếu quy trình GitHub hỗ trợ, nhưng vẫn cần reviewer thực sự đọc thay đổi.

- Branch: feature/<service>-<task>, fix/<service>-<task>; PR nhỏ, mô tả được và build được. Không commit dependency/build output/IDE files/secret.
- Mỗi task nêu invariant, service và contract liên quan, điều kiện hoàn tất, phần phụ thuộc. Phân việc theo domain, không chia người làm controller còn người khác sở hữu DB của cùng service mà thiếu phối hợp.
- Thay API phải được reviewer producer và consumer duyệt. Chọn mở rộng tương thích trước, đổi client sau, xóa route cũ cuối cùng. Migration schema có version và plan triển khai tương thích.
- Thay ownership bảng, tách/gộp service, token/role, state machine, timeout/payment policy, ID/currency/timezone, framework major hoặc thêm broker đều cần ADR trong docs/adr/.
- ADR có: vấn đề, lựa chọn, quyết định, hệ quả, service/client/dữ liệu ảnh hưởng, migration/rollback, người review và trạng thái PROPOSED/ACCEPTED/SUPERSEDED. Chỉ ACCEPTED mới thay chuẩn.
- Khi ADR được chấp nhận, cập nhật AGENTS + OpenAPI + migration plan + task liên quan trong cùng PR. Không để quyết định chỉ nằm trong chat.
- File AGENTS con được phép hướng dẫn local, phải dẫn về chuẩn root. Nếu mâu thuẫn ownership/API/invariant, ghi nhận và giải quyết bằng ADR trước khi merge; không tự chọn cách hiểu riêng.
- Code đang chạy là bằng chứng hiện trạng; AGENTS là đích chung, OpenAPI là hợp đồng wire, migration là lịch sử DB. Nếu chúng lệch nhau, mở task khắc phục và ghi mốc; không tuyên bố code tự đúng vì tài liệu ghi như vậy.

Ba cổng phối hợp phải hoàn tất trước khi team tách domain song song: (1) parent POM/BOM pin chính xác; (2) owner + contract được duyệt; (3) phương án dữ liệu Ticket trùng và account/ID mapping. Có thể tiếp tục các công việc độc lập trong khi một cổng chưa xong.

## 15. Hướng dẫn cho AI và thành viên bắt đầu task

1. Đọc AGENTS root, hướng dẫn local, ADR ACCEPTED và contract của domain; xác minh cấu trúc file hiện tại.
2. Xác định owner, dữ liệu được phép ghi và route cũ có thể bị ảnh hưởng. Nếu task yêu cầu vượt ranh giới, nêu tác động và đi qua ADR.
3. Thực hiện phạm vi task, cập nhật migration/DTO/OpenAPI theo thay đổi; không tự “tiện thể” nâng framework hay tách thêm service.
4. Kiểm chứng theo mục 12 và đọc kết quả thực tế. Thiếu môi trường thì báo lệnh chưa chạy và rủi ro còn lại.
5. Trước PR, tự đối chiếu invariant ghế/giá/payment/security và cung cấp bằng chứng cho reviewer. Không nói “hoàn tất” nếu chỉ tạo skeleton.

File này là chuẩn chung; docs/contracts và ADR được tạo qua từng mốc để bổ sung chi tiết. Các đường dẫn tài liệu đích chưa tồn tại phải được tạo trước task phụ thuộc, không coi tên file trong hướng dẫn là bằng chứng chúng đã có.


## 16. Công nghệ đã xác minh và setup thống nhất

### 16.1. Baseline từ project được cung cấp

Các đường dẫn nguồn dưới đây tính tương đối từ thư mục Cinema_Management trong ZIP. Khi chuyển vào legacy/, thêm tiền tố legacy/ tương ứng. Nếu repo team khác bản ZIP, đọc lại file thực tế và ghi commit; không áp phiên bản ghi ở tài liệu cho source khác mà không kiểm tra.

| Thành phần | Giá trị trong source | Nguồn / cách sử dụng |
|---|---|---|
| Java | 21 | pom.xml: java.version; JDK dùng build và runtime phải thống nhất |
| Spring Boot | 3.5.4 | spring-boot-starter-parent; baseline kế thừa, không tự nâng riêng từng service |
| Build | Maven | pom.xml; bản ZIP không có Maven Wrapper, chưa chốt Maven patch |
| SQL Server JDBC | 12.10.1.jre11 | mssql-jdbc trong pom.xml; hậu tố jre11 không yêu cầu hạ JDK 21 xuống 11 |
| Backend | MVC/Web, Validation, JPA, Security, Thymeleaf, Mail | Dependency hiện có; chọn theo trách nhiệm service, không sao chép tất cả sang mọi service |
| Thymeleaf Security extras | 3.1.2.RELEASE | thymeleaf-extras-springsecurity6 |
| QR | ZXing core và javase 3.5.3 | Giữ ở domain thực sự tạo QR |
| DB | SQL Server, database xuất Cinema_Management1 | SQL.sql; chưa xác minh chính xác engine version/edition trên máy team |
| Backend local cũ | 8081 | src/main/resources/application.properties; khi cùng chạy hệ mới dùng legacy 8090 |
| React / React DOM | ^19.2.6 | fe/package.json; đây là khoảng phiên bản khai báo, không phải phiên bản đã cài được xác nhận |
| Vite | ^8.0.12 | fe/package.json; kiểm tra lockfile và engines trước khi chọn Node |
| UI thư viện | Bootstrap ^5.3.8, React Bootstrap ^2.10.10, React Router DOM ^7.17.0 | fe/package.json; fe hiện là scaffold |
| Spring Cloud / JWT / Flyway / Compose | Đích cần bổ sung | Chưa có trong pom.xml/cấu trúc nguồn đã xét; không báo đã triển khai |

Các dependency không ghi version trong pom.xml lấy version theo parent/BOM. Không suy ra máy đang cài Maven, Node hoặc SQL Server phiên bản nào từ tài liệu môn học hay từ tên driver. Bộ 1.3 đã pin baseline trong reference/build/pom.xml và docs/setup.md; bootstrap phải ghi phiên bản JDK/image digest thực tế và kết quả effective-pom/build. Có pin phiên bản không đồng nghĩa đã chạy thành công.

### 16.2. Chạy baseline nguồn trên Windows / IntelliJ

Đây là quy trình cần thực thi và ghi kết quả, chưa phải log chạy thành công:

1. Chọn JDK 21 trong Project SDK, Maven runner và JAVA_HOME; chạy `java -version`, `mvn -v`. Nếu khác JDK thì sửa môi trường trước.
2. Mở thư mục chứa pom.xml bằng IntelliJ và import Maven. Không dùng target/ làm thư mục source; không đưa target/, node_modules/ vào repo mới.
3. Dựng SQL Server và restore/import bản sao SQL.sql đã chuẩn hóa đường dẫn file. Script gốc UTF-16 và có đường dẫn Windows cá nhân; không chạy nguyên trạng vào môi trường Docker/Linux. Bổ sung Watchlist bằng migration ở mục 19, chỉ sau kiểm tra bảng đã tồn tại hay chưa.
4. Đưa cấu hình DB/mail ra biến môi trường hoặc cấu hình local bị Git ignore. Bản nguồn có giá trị nhạy cảm; không sao chép các giá trị đó sang AGENTS, seed hay config-repo. Biến môi trường Spring Boot có thể ghi đè properties hiện tại.
5. Build và chạy từ đúng thư mục pom.xml. Ghi kết quả thật, không dựa vào JAR có sẵn trong target/.

Ví dụ PowerShell, thay các placeholder bằng thông tin local của thành viên, không commit giá trị thật:

```powershell
java -version
mvn -v
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://localhost:1433;databaseName=Cinema_Management1;encrypt=true;trustServerCertificate=true'
$env:SPRING_DATASOURCE_USERNAME = '<local-db-user>'
$env:SPRING_DATASOURCE_PASSWORD = '<local-db-password>'
$env:SERVER_PORT = '8090'
mvn clean package
mvn spring-boot:run
```

`trustServerCertificate=true` chỉ là lựa chọn dev với chứng chỉ local; môi trường triển khai phải xác minh chứng chỉ phù hợp. Mail dùng SMTP cổng 587 và STARTTLS theo source, lấy thông tin qua SPRING_MAIL_HOST/USERNAME/PASSWORD; luồng reset password cần mail thật hoặc mock được cấu hình rõ. Lệnh build thành công chưa chứng minh các invariant đặt ghế đúng; kiểm tra DB và luồng tại mục 12 riêng.

fe/ có scripts dev/build/lint/preview. Nếu cần kiểm tra scaffold: dùng Node tương thích engines của dependency và lockfile; `npm ci` khi có package-lock.json phù hợp, sau đó `npm run build`. Không coi scaffold là client nghiệp vụ hoàn chỉnh hoặc bắt team tái tạo chức năng dựa vào giao diện chưa có.

Trong Docker, DB cùng network dùng DNS tên service; DB trên Windows host có thể dùng host.docker.internal trong môi trường hỗ trợ. Không lấy URL localhost của host gắn vào container khác. .env.example chỉ chứa tên biến và giá trị mẫu; mỗi domain có user và DB riêng theo mục 3.

## 17. Inventory API nguồn và service tiếp nhận

Bảng sau là mapping từ annotation đang hoạt động trong các controller Java được gửi, phục vụ migration. Không tính annotation đã comment; annotation chưa chứng minh endpoint đã chạy hay security đúng. Base package nguồn: src/main/java/vn/edu/fpt/cinemamanagement/. Path nguồn được giữ qua adapter trong giai đoạn chuyển; path /api/v1 mới phải có contract riêng trước khi client đổi. Không tự đổi tất cả URL theo tên service.

| Controller nguồn | Service tiếp nhận / xử lý | Method và path nguồn |
|---|---|---|
| controller/AuthApiController.java | identity | `POST /api/auth/forgot-password`<br>`POST /api/auth/reset-password`<br>`POST /api/auth/login`<br>`GET /api/auth/me`<br>`POST /api/auth/logout`<br>`POST /api/auth/register` |
| controller/CashierBookingController.java | booking | `GET /api/cashier/showtimes/{showtimeId}/seats`<br>`POST /api/cashier/bookings`<br>`POST /api/cashier/bookings/{bookingId}/cancel` |
| controller/ConcessionController.java | catalog (module concession) | `GET /api/concessions`<br>`GET /api/concessions/{id}`<br>`POST /api/concessions`<br>`PUT /api/concessions/{id}`<br>`DELETE /api/concessions/{id}`<br>`GET /api/concessions/images` |
| controller/PaymentController.java | booking; thay các GET ghi trạng thái bằng command an toàn | `GET /payments/ebanking/{bookingId}/{staffId}`<br>`GET /payments/paymentsuccess`<br>`GET /payments/ebanking/{bookingId}`<br>`GET /payments/receive-cash/{bookingId}/{staffId}`<br>`POST /payments/receive-cash/{bookingId}/{staffId}` |
| controller/QRController.java | booking; kiểm tra ý nghĩa QR, không suy ra provider thật | `GET /qrpayment/qrpayment` |
| controller/ShiftScheduleController.java | identity (module workforce) | `GET /shift-schedules`<br>`GET /shift-schedules/edit/{id}`<br>`POST /shift-schedules/update`<br>`GET /shift-schedules/create`<br>`POST /shift-schedules/create`<br>`POST /shift-schedules/checkin/{id}`<br>`POST /shift-schedules/delete`<br>`GET /shift-schedules/schedule` |
| controller/ShowtimeController.java | cinema; endpoint seats đọc inventory qua booking | `GET /api/showtimes/admin`<br>`GET /api/showtimes/{id}`<br>`GET /api/showtimes/movie/{movieId}`<br>`GET /api/showtimes/{showtimeId}/seats`<br>`DELETE /api/showtimes/{id}` |
| controller/StaffHomeController.java | adapter legacy; booking cho order/check-in | `GET /staffs/staff_home`<br>`GET /staffs/cashier/booking/{movieId}`<br>`GET /staffs/cashier/concessions`<br>`POST /staffs/cashier/order`<br>`GET /staffs/checkIn_Ticket`<br>`GET /staffs/checkIn_Ticket/{ticketId}` |
| controller/VoucherController.java | booking | `GET /api/vouchers/admin`<br>`GET /api/vouchers/admin/{id}`<br>`POST /api/vouchers/admin`<br>`PUT /api/vouchers/admin/{id}`<br>`DELETE /api/vouchers/admin/{id}` |
| controller/api/CashierShowtimeRestController.java | cinema | `GET /api/cashier/showtimes` |
| controller/api/CustomerBookingRestController.java | booking | `POST /api/customer/bookings/{bookingId}/demo-payment`<br>`POST /api/customer/bookings/preview`<br>`POST /api/customer/bookings` |
| controller/api/CustomerSeatHoldRestController.java | booking | `POST /api/customer/seat-holds`<br>`DELETE /api/customer/seat-holds/{holdId}` |
| controller/api/CustomerTicketRestController.java | booking | `GET /api/customer/tickets`<br>`GET /api/customer/tickets/{ticketId}` |
| controller/api/CustomerWatchlistRestController.java | catalog | `GET /api/customer/watchlist`<br>`PUT /api/customer/watchlist/{movieId}`<br>`DELETE /api/customer/watchlist/{movieId}` |
| controller/api/MovieRestController.java | catalog | `GET /api/movies`<br>`GET /api/movies/{id}`<br>`GET /api/movies/next-id`<br>`POST /api/movies`<br>`PUT /api/movies/{id}`<br>`DELETE /api/movies/{id}`<br>`GET /api/movies/coming-soon` |
| controller/api/PaymentRestController.java | booking | `POST /api/cashier/payments/cash` |
| controller/api/PublicMovieRestController.java | catalog | `GET /api/public/movies/top`<br>`GET /api/public/movies`<br>`GET /api/public/movies/{id}` |
| controller/api/RoomRestController.java | cinema | `GET /api/rooms`<br>`GET /api/rooms/{id}`<br>`GET /api/rooms/{id}/template` |
| controller/api/ShiftScheduleRestController.java | identity (module workforce) | `GET /api/staff-schedules`<br>`GET /api/staff-schedules/cashier`<br>`GET /api/staff-schedules/{id}`<br>`POST /api/staff-schedules`<br>`PUT /api/staff-schedules/{id}`<br>`DELETE /api/staff-schedules/{id}`<br>`POST /api/staff-schedules/{id}/check-in` |
| controller/api/ShowtimeRestController.java | cinema | `GET /api/admin/showtimes`<br>`POST /api/admin/showtimes/validate`<br>`POST /api/admin/showtimes`<br>`PUT /api/admin/showtimes/{id}`<br>`GET /api/admin/showtimes/{id}`<br>`GET /api/admin/showtimes/all`<br>`DELETE /api/admin/showtimes/{id}` |
| controller/api/StaffRestController.java | identity | `GET /api/admin/staffs`<br>`GET /api/admin/staffs/{id}`<br>`GET /api/admin/staffs/next-id`<br>`POST /api/admin/staffs`<br>`PUT /api/admin/staffs/{id}`<br>`DELETE /api/admin/staffs/{id}`<br>`GET /api/admin/staffs/me`<br>`PATCH /api/admin/staffs/me` |
| controller/api/SupportingRestController.java | adapter; tách theo từng endpoint (xem dưới) | `GET /api/public/movies/now-showing`<br>`GET /api/admin/rooms`<br>`GET /api/cashier/bookings/{id}` |
| controller/api/VoucherRestController.java | booking | `POST /api/vouchers/apply` |

SupportingRestController phải tách: GET /api/public/movies/now-showing → catalog/read aggregation có phối hợp cinema nếu cần; GET /api/admin/rooms → cinema; GET /api/cashier/bookings/{id} thuộc booking trong nguồn nhưng RETIRED ở đích 1.4. Không tạo shared service truy cập cả ba DB để giữ controller cũ.

BankingPaymentApiController không có method mapping đang hoạt động trong bản source đã xét; PublicShowtimeRestController không cung cấp endpoint hoạt động. Không tự công bố endpoint dựa vào tên class. Endpoint /next-id là hiện trạng cần thay bằng ID an toàn do server cấp khi tạo, không dùng làm cơ chế reserve ID. POST/PUT Movie có multipart: bảo toàn upload ảnh và vị trí lưu/URL ảnh khi migration, không tự đổi sang JSON thuần.

Không giữ nguyên hành vi thiếu an toàn chỉ để tương thích: các GET payment/check-in có ghi dữ liệu phải chuyển sang command xác thực, cập nhật client tương ứng và chặn đường ghi cũ. UI redirect chỉ đọc kết quả đã xác minh. Các route Thymeleaf còn cần trong giai đoạn chuyển dùng adapter; route ngoài phạm vi phải bị ngừng, không tự yêu cầu xây UI thay thế.

## 18. Payload đã xác minh và yêu cầu contract

### 18.1. Luồng customer đang có

| API nguồn | Input / output theo DTO/controller hiện tại |
|---|---|
| POST /api/customer/seat-holds | Body: showtimeId, showtimeSeatIds (list String). 201: holdId, showtimeId, seatIds, expiresAt, serverTime. expiresAt/serverTime là OffsetDateTime. |
| DELETE /api/customer/seat-holds/{holdId} | Principal sở hữu hold; thành công 204. Lỗi hold có code, message và seatIds nếu có. |
| POST /api/customer/bookings/preview | Body CustomerBookingRequestDTO; 200 CustomerBookingQuoteResponseDTO. |
| POST /api/customer/bookings | Body CustomerBookingRequestDTO; Idempotency-Key bắt buộc, không rỗng, tối đa 128 ký tự. Tạo mới 201; replay hiện tại 200; khác fingerprint 409. Output: bookingId, status, totalAmount. |
| POST /api/customer/bookings/{bookingId}/demo-payment | Body: outcome (SUCCESS/FAILED theo service demo). 200: bookingId, paymentId, paymentStatus, amount, ticketId, message. Phải bổ sung profile demo và idempotency bền vững theo mục 5. |
| GET /api/customer/tickets và /{ticketId} | Lấy principal và kiểm tra ownership qua CustomerTicketService; giữ DTO/source làm căn cứ chi tiết. |
| GET /api/customer/watchlist | 200 list MovieResponseDTO. |
| PUT hoặc DELETE /api/customer/watchlist/{movieId} | 204, idempotent; PUT phim không tồn tại trả 404. |

CustomerBookingRequestDTO gồm:

```json
{
  "holdId": "<hold-id>",
  "showtimeId": "<showtime-id>",
  "showtimeSeatIds": ["<showtime-seat-id>"],
  "concessions": [{"concessionId": "<concession-id>", "quantity": 1}],
  "voucherCode": null
}
```

Không đổi tên showtimeSeatIds đầu vào thành seatIds chỉ vì response hold dùng tên seatIds. Không nhận totalAmount/giá từ client làm nguồn tiền chính thức. Preview không phải thao tác consume hold; tạo booking kiểm tra lại trạng thái/giá/quota trong workflow đã chốt.

Quote hiện có ticketItems, concessionItems, ticketSubtotal, concessionSubtotal, subtotal, voucher, discountAmount, total. Mỗi Item: itemType, itemId, name, quantity, unitPrice, lineTotal; Voucher: voucherId, code, discountType, discountValue. DTO quote hiện khai báo discountValue kiểu int nhưng SQL Voucher là DECIMAL(10,2): kiểm tra có giá trị lẻ trước migration; contract đích dùng BigDecimal tương thích quy tắc tiền, không âm thầm cắt phần lẻ.

Controller hiện tìm Customer qua principal.getName() và repository username; có nhánh tài khoản không hợp lệ trả 403. Đó là hiện trạng. Đích JWT lấy sub ổn định và dùng 401/403 theo mục 6–7. Không đổi đồng thời status/error/payload ở adapter cũ nếu chưa có kiểm thử client; lỗi cũ có khi chỉ message, có khi code/message/seatIds. API v1 dùng hợp đồng lỗi chung, giữ chi tiết seatIds qua contract mở rộng được thống nhất.

### 18.2. Công việc Agent phải thực hiện khi viết OpenAPI

Inventory ở mục 17 và DTO ở mục này là căn cứ sẵn có, không yêu cầu team đoán lại API. Với domain được giao, Agent phải đọc controller → request/response DTO → service → security trước khi viết contract. Mỗi operation ghi method/path, role/ownership, validation, request/response và ví dụ, lỗi, paging, timeout, idempotency, tương thích route cũ. Trường/chức năng chưa có trong source phải gắn ĐÍCH THIẾT KẾ, không ghi là hiện có.

Contract HTTP giữa service cần tạo: cinema đọc metadata catalog; booking đọc snapshot cinema và quote concession do catalog cung cấp; cinema khởi tạo/đóng sales inventory booking. Không triển khai API phân ca; không tạo FeignClient workforce/identity. Endpoint, DTO và version mới được chốt ở docs/contracts/core.yaml bản 1.0.0; đây là thiết kế đích, chưa có trong monolith. Implement và test producer/consumer theo contract, không dùng bảng/entity thay DTO. API CRUD ngoài phạm vi core phải bổ sung contract trước task tương ứng.

## 19. Schema nguồn và kế hoạch chuyển database

### 19.1. Cấu trúc bảng đã đọc từ SQL.sql

Bảng dưới chỉ liệt kê cột và kiểu của schema nguồn, không chép dữ liệu cá nhân hoặc mật khẩu mẫu. Dấu NOT NULL/NULL giữ theo DDL; chi tiết default/FK và index phải được chuyển bằng migration và kiểm tra trên DB. Tên SQL giữ nguyên trong adapter/migration; không tự đổi tên toàn bộ sang camelCase trong DB.

| Bảng nguồn | Cột / kiểu đã khai báo |
|---|---|
| Booking | `booking_id varchar(8) NOT NULL`; `user_id varchar(8) NOT NULL`; `voucher_id varchar(8) NULL`; `total_amount decimal(10, 2) NULL`; `status varchar(20) NULL`; `payment_id varchar(8) NULL`; `created_at datetime NULL` |
| Booking_Detail | `booking_detail_id varchar(8) NOT NULL`; `booking_id varchar(8) NOT NULL`; `item_type varchar(20) NULL`; `showtime_seat_id varchar(8) NULL`; `concession_id varchar(8) NULL`; `quantity int NULL`; `unit_price decimal(10, 2) NOT NULL`; `total_price  AS (quantity*unit_price) PERSISTED` |
| Concession | `concession_id varchar(8) NOT NULL`; `name varchar(100) NOT NULL`; `price decimal(10, 2) NOT NULL`; `description varchar(255) NOT NULL`; `img varchar(255) NOT NULL` |
| Customer | `user_id varchar(8) NOT NULL`; `username varchar(100) NOT NULL`; `password varchar(255) NOT NULL`; `dob date NOT NULL`; `sex bit NOT NULL`; `email varchar(100) NOT NULL`; `phone varchar(10) NOT NULL`; `verify varchar(255) NULL`; `reset_requested_at datetime NULL` |
| Movie | `movie_id varchar(8) NOT NULL`; `title varchar(200) NOT NULL`; `duration int NOT NULL`; `genre varchar(100) NOT NULL`; `release_date date NOT NULL`; `summary varchar(max) NULL`; `age_rating varchar(10) NULL`; `img varchar(255) NOT NULL`; `trailer varchar(255) NOT NULL` |
| Movie_Rating | `rating_id varchar(8) NOT NULL`; `movie_id varchar(8) NOT NULL`; `user_id varchar(8) NOT NULL`; `rating_score int NOT NULL` |
| Payment | `payment_id varchar(8) NOT NULL`; `booking_id varchar(8) NOT NULL`; `payment_method varchar(20) NOT NULL`; `payment_time datetime NULL`; `amount decimal(10, 2) NOT NULL`; `payment_status varchar(20) NULL`; `staff_id varchar(8) NULL` |
| Room | `room_id int IDENTITY(1,1) NOT NULL`; `template_id varchar(8) NOT NULL`; `status varchar(255) NOT NULL` |
| Shift_Schedule | `shift_schedule_id varchar(8) NOT NULL`; `staff_id varchar(8) NOT NULL`; `shift_date date NOT NULL`; `status varchar(20) NULL`; `shift_start time(7) NOT NULL`; `late_minute int NULL` |
| Showtime | `showtime_id varchar(8) NOT NULL`; `movie_id varchar(8) NOT NULL`; `room_id int NOT NULL`; `show_date date NOT NULL`; `start_time time(7) NOT NULL`; `end_time time(7) NOT NULL` |
| Showtime_Seat | `showtime_seat_id varchar(8) NOT NULL`; `showtime_id varchar(8) NOT NULL`; `template_seat_id varchar(8) NOT NULL`; `status varchar(20) NULL` |
| Staff | `staff_id varchar(8) NOT NULL`; `full_name varchar(100) NOT NULL`; `position varchar(50) NOT NULL`; `username varchar(50) NOT NULL`; `password varchar(255) NOT NULL`; `phone varchar(10) NOT NULL`; `email varchar(100) NOT NULL` |
| Template | `template_id varchar(8) NOT NULL`; `name varchar(50) NOT NULL`; `description varchar(max) NULL` |
| Template_Seat | `template_seat_id varchar(8) NOT NULL`; `template_id varchar(8) NOT NULL`; `row_label varchar(255) NOT NULL`; `seat_number int NOT NULL`; `seat_type varchar(255) NOT NULL` |
| Ticket | `ticket_id varchar(8) NOT NULL`; `booking_id varchar(8) NOT NULL`; `price decimal(10, 2) NOT NULL`; `checked_in bit NULL`; `checked_in_time datetime NULL`; `checked_in_staff_id varchar(8) NULL` |
| Voucher | `voucher_id varchar(8) NOT NULL`; `code varchar(10) NOT NULL`; `discount_type varchar(50) NOT NULL`; `discount_value decimal(10, 2) NOT NULL`; `usage_limit int NOT NULL`; `used_count int NOT NULL`; `voucher_name varchar(255) NOT NULL`; `status bit NOT NULL` |

Watchlist do người dùng bổ sung: watchlist_id BIGINT IDENTITY PK; customer_id VARCHAR(8) NOT NULL; movie_id VARCHAR(8) NOT NULL; added_at DATETIME2 NOT NULL DEFAULT SYSDATETIME(); unique(customer_id,movie_id). DDL nguồn có FK Customer(user_id) và Movie(movie_id). Đây là DDL được cung cấp, chưa xác nhận đã thực thi trên DB.

### 19.2. FK giữ trong domain và tham chiếu chuyển thành ID

| Domain đích | Quan hệ FK nội bộ được giữ | Tham chiếu ngoài domain phải bỏ FK/JPA association |
|---|---|---|
| identity_db | Shift_Schedule → Staff; quan hệ account/role mới nếu có | Không truy cập booking/catalog để đăng nhập |
| catalog_db | Movie_Rating → Movie; Watchlist → Movie; Concession không có FK trong schema nguồn | Movie_Rating.user_id, Watchlist.customer_id → identity qua ID; không đọc Booking_Detail qua DB để trả danh mục |
| cinema_db | Room → Template; Template_Seat → Template; Showtime → Room | Showtime.movie_id → catalog qua ID + snapshot phù hợp |
| booking_db | Booking → Voucher; Booking_Detail → Booking và Showtime_Seat; Payment → Booking; Ticket → Booking | Booking.user_id; Booking_Detail.concession_id; Payment.staff_id; Ticket.checked_in_staff_id; Showtime_Seat.showtime_id và template_seat_id |

Shift_Schedule.staff_id tiếp tục có FK tới Staff.staff_id trong identity_db. Không bỏ FK này theo phương án workforce tách riêng của bản cũ. Concession chuyển vào catalog_db; Booking_Detail.concession_id vẫn là tham chiếu ngoài booking nên phải bỏ FK xuyên domain và lưu snapshot giá/tên món.

Showtime_Seat trong booking giữ showtimeId và templateSeatId để đối chiếu snapshot, không giữ @ManyToOne đến entity thuộc cinema. Nếu tạo bảng snapshot trong booking_db thì có thể dùng FK tới bảng snapshot cùng DB. Không giữ FK trỏ thẳng cinema_db dù SQL Server instance dùng chung. Booking_Detail.concession_id là ID ngoài domain, giá/tên tại lúc mua lưu snapshot.

### 19.3. Phần schema đích cần tạo thêm

Đây là thiết kế bổ sung, chưa có trong SQL gốc. Bộ 1.3 cung cấp DDL đích ở reference/database/booking/V1__initial_schema.sql; tích hợp và kiểm chứng bằng Flyway trước code phụ thuộc, không giả định Hibernate sẽ tự tạo:

- SeatHold: holdId, customerId, showtimeId, status, createdAt, expiresAt; liên kết ghế và owner hiện tại lưu bền vững. Có index trạng thái/thời hạn để job hết hạn truy vấn được.
- ActiveHold: một hàng hiện hành cho (customerId, showtimeId), unique trên cặp này; FK tới hold hiện hành. Lưu lịch sử hold riêng; serialize tạo/thay hold, không chỉ query “chưa có” rồi insert thiếu constraint.
- Inventory bổ sung holdId/bookingId hiện hành hoặc quan hệ tương đương, status và concurrency version nếu dùng optimistic/CAS. unique(showtimeId,templateSeatId); điều kiện cập nhật phải khớp owner + trạng thái.
- IdempotencyRecord: actorId, operation, key, requestHash, processingStatus, resourceId, responseStatus/responseBody hoặc dữ liệu đủ tái hiện, createdAt, expiresAt; unique(actorId,operation,key). Quy định độ dài/collation của key để tránh SQL Server coi hai key khác nhau là một ngoài ý muốn. Ghi kết quả cùng transaction; không để dữ liệu nhạy cảm ngoài phạm vi replay.
- Booking thêm channel và expiresAt; snapshot phim/phòng/giờ/ghế, món/giá và salesVersion đủ cho ticket và đối soát. Cập nhật kích thước cột ID nếu dùng UUID; không nhét UUID vào VARCHAR(8).
- Sales snapshot theo suất: showtimeId, snapshotVersion, trạng thái mở/đóng bán; ghế ổn định, metadata và lịch sử chuyển trạng thái cần thiết. Khởi tạo snapshot/inventory nguyên tử và idempotent.
- Payment.booking_id và Ticket.booking_id unique theo chính sách mục 5, chỉ sau đối soát dữ liệu. Booking.payment_id trùng hướng liên kết với Payment.booking_id: chọn Payment.booking_id làm nguồn liên kết chính trong đích, đối soát giá trị cũ rồi loại bỏ cột dư bằng migration tương thích.
- Template_Seat unique(template_id,row_label,seat_number); check quantity dương, tiền không âm và voucher quota hợp lệ khi nghiệp vụ áp dụng. Dữ liệu cũ NULL/khác chuẩn phải được phân loại trước khi thêm NOT NULL/CHECK.

Tên bảng/cột/PK/FK/CHECK cụ thể đã ghi trong reference/database/*/V1__initial_schema.sql của bộ 1.3; JPA phải khớp. DDL dành cho DB mới rỗng, chưa chạy trên SQL Server. Provision 4 database và ETL dữ liệu cũ là bước riêng, không chạy V1 vào schema monolith.

## 20. Bảng tương thích và việc sửa từ hệ thống cũ

| Điểm đã xác minh | Quy tắc chuyển đổi / cách kiểm chứng |
|---|---|
| CustomerSeatHoldService có ConcurrentHashMap; ghế PENDING đã ghi DB nhưng owner/expiry hold nằm RAM | Lưu toàn bộ hold cần thiết vào DB. Test giữ ở instance A, tạo booking ở B; restart vẫn giữ đúng owner/expiry. Không mô tả sai là toàn bộ trạng thái ghế chỉ ở RAM. |
| SeatHoldExpiryScheduler có cleanup ở ApplicationReadyEvent | Bỏ suy luận hold mồ côi từ map rỗng; job chỉ xử lý hạn/owner theo DB. Startup instance B không nhả hold A. |
| CustomerBookingRestController có LinkedHashMap tối đa 500 và synchronized | Chuyển sang unique/transaction DB, replay tối thiểu 24 giờ. synchronized hiện chỉ bảo vệ JVM, không dùng làm cam kết nhiều instance. |
| Có CustomerBookingService, CashierBookingService và BookingService cũ | Dùng cùng invariant; kiểm tra StaffHomeController POST /staffs/cashier/order trước khi route traffic. Luồng cũ không được bỏ qua lock/availability. |
| PaymentController có GET /payments/paymentsuccess dựa tham số | Chặn khả năng giả thành công; thay bằng command xác thực. Không giữ lỗ hổng bằng adapter chỉ để UI cũ tiếp tục dùng được. |
| CustomerDemoPaymentService nhận outcome từ client | Chỉ profile demo, bảo vệ quyền/owner, chống retry lặp; không công bố đã xác minh ngân hàng. |
| SecurityConfig dùng formLogin/session; có cấu hình CSRF disable và thứ tự matcher voucher rộng trước admin | Phân tách chain legacy và bearer; sửa thứ tự matcher; test trực tiếp service, role và owner. Không chỉ bảo vệ ở gateway. |
| Room.id Java String, SQL room_id INT IDENTITY; Room.template là OneToOne | Dùng Integer persistence, adapter chuỗi nếu cần; Room–Template nhiều-một theo mục 10. Test giữ nguyên ID và quan hệ khi chuyển. |
| Payment/Ticket entity OneToOne nhưng SQL thiếu unique; có hai booking gắn nhiều ticket | Lập báo cáo bản ghi hiệu lực/lịch sử; không xóa tự động. Constraint mới chỉ sau đối soát và duyệt phương án dữ liệu. |
| BookingDetail.total_price SQL computed PERSISTED, entity có xử lý riêng | Giữ công thức quantity × unit_price, mapping read-only nếu giữ computed; không ghi trực tiếp vào computed column. |
| Status nguồn khác cách viết và khác ý nghĩa theo luồng | Mapping có ngữ cảnh theo bảng bên dưới; không replace toàn DB theo chuỗi đơn thuần. |
| Password Customer có ba bản ghi không giống BCrypt, default SQL không phù hợp | Xác minh/đặt lại mật khẩu có kiểm soát; bỏ default; không suy ra mọi chuỗi lạ là plaintext, không hash lại BCrypt. |
| ID mới còn có max + 1; API next-id tồn tại | Sequence/UUID và migration độ dài; tạo ID tại server lúc ghi. Test đồng thời nhiều đơn khác ghế không va chạm. |
| LocalDateTime/datetime cũ không có offset | Xác minh timezone thực của nguồn, ghi mapping trước UTC; không tự cộng/trừ 7 giờ chỉ dựa vị trí người dùng. |

Mapping trạng thái dùng làm hướng dẫn đối soát:

| Dữ liệu nguồn | Đích / điều kiện |
|---|---|
| Inventory AVAILABLE | AVAILABLE nếu không có booking/hold hiệu lực xung đột |
| Inventory PENDING | HELD; phải tái dựng được owner hold hoặc booking chờ trả tiền và hạn. Không rõ chủ → đối soát, không tự nhả |
| Inventory UNAVAILABLE | Chỉ ánh xạ SOLD khi có chứng cứ booking/payment tương ứng; dữ liệu chặn ghế khác cần phân loại |
| Booking BOOKED | PENDING_PAYMENT kèm channel, expiresAt; booking cũ chưa rõ hạn cần chính sách chuyển dữ liệu |
| Booking paid/PAID | PAID sau đối chiếu payment/ticket/amount; không tạo payment thành công giả để lấp dữ liệu thiếu |
| Booking cancelled/CANCELLED | CANCELLED; bảo toàn lịch sử và kiểm tra ghế được nhả đúng |
| Payment PAID hoặc SUCCESS/success | SUCCEEDED sau đối soát; các luồng dùng tên khác nhau, không đồng nhất theo tên mà bỏ qua chứng cứ |
| Payment FAILED | FAILED, giữ nguyên việc thất bại không bán ghế |
| Payment EXPIRED và trường hợp đến muộn | Giữ kết quả cũ trong lịch sử; booking EXPIRED; provider đến muộn đi workflow đối soát. Không tự quy EXPIRED thành FAILED rồi bỏ lịch sử |

Các giá trị khác/NULL đưa vào danh sách migration cần xử lý; không tự chọn default. Trước chuyển traffic: backup, snapshot số liệu, kiểm tra số dòng/ID/tham chiếu/tổng tiền, test route cũ qua adapter, ghi route nào còn legacy và ai là nguồn ghi. Cấm hai service cùng nhận ghi một domain trong lúc chuyển mà không có protocol được duyệt.

## 21. Chỉ dẫn bắt buộc cho Agent của từng thành viên

Agent nhận task phải đọc AGENTS.md này cùng source hiện tại; không chỉ dựa vào bản tóm tắt trong chat. Khi đọc file này mà không có source, chỉ có thể hỗ trợ thiết kế; không tuyên bố đã kiểm tra/build project.

1. Xác định domain ở mục 3, API nguồn ở mục 17–18, bảng/FK ở mục 19 và điểm chuyển đổi ở mục 20.
2. Đọc baseline mục 22 và reference/build/pom.xml; kiểm tra parent thực tế đã tích hợp/kiểm chứng. Không tự chọn patch riêng cho service.
3. Đọc controller, DTO, service, repository, entity và security của luồng được giao. File và thư mục nào chưa tồn tại thì nói rõ, không dựng kết luận từ tên đường dẫn dự kiến.
4. Trước code phụ thuộc, đọc contract/schema 1.3; bổ sung API ngoài phạm vi core và tích hợp migration đúng domain từ inventory sẵn có. Không yêu cầu người dùng cung cấp lại thông tin đã đọc được từ project. Những lựa chọn nghiệp vụ mới hoặc dữ liệu mơ hồ phải ghi rõ để review.
5. Mỗi task ghi đầu ra thực tế: file sửa, route cũ được giữ/chuyển, DB được phép ghi, contract/schema ảnh hưởng, cách kiểm chứng và giới hạn. Không tự thêm service, broker, JWT claim hoặc đổi payload ngoài phạm vi.
6. Chạy kiểm tra ở mục 12 tương ứng thay đổi; lưu bằng chứng. Task tài liệu chỉ xác minh nội dung/đường dẫn, không báo hệ thống đã chạy.

Bộ 1.3 đã tạo docs/setup.md, docs/security.md, docs/api.md, docs/contracts/core.yaml, docs/database.md, docs/migration.md, docs/ownership.md, docs/acceptance.md, docs/sources.md, docs/verification.md, ADR và reference/build/pom.xml + DDL tham chiếu cho 4 DB. Đưa nguyên bộ vào repo để các thành viên cùng đọc. Source ứng dụng, Compose/Dockerfile, ETL và CI chạy thật chưa được tạo chỉ vì có tài liệu; implementation phải qua các cổng nghiệm thu. Contract core không bao phủ toàn bộ inventory API legacy; task CRUD còn lại phải bổ sung hợp đồng trước code.

Ghi nhận kiểm chứng nguồn ở bản 1.1, được giữ trong bản 1.2: đối chiếu tĩnh pom.xml, package.json, application.properties, annotation controller, DTO/entity/service/scheduler/security và DDL đã cung cấp. Chưa chạy build, DB migration, integration test hay deployment trong lần bổ sung này. Các cổng nghiệm thu mục 11–14 vẫn phải có kết quả thực tế.

Ghi nhận thay đổi bản 1.2: đồng bộ 4 service/4 DB, module workforce và concession, port, cấu trúc repository, API owner, lời gọi nội bộ/HTTP, FK và các mốc nghiệm thu. Chỉ cập nhật tài liệu; chưa thay source, backlog Excel hoặc chạy migration/deployment.


## 22. Tiêu chuẩn 1 — Công nghệ và cấu hình dùng chung

Nguồn chuẩn: [setup](docs/setup.md), [parent tham chiếu](reference/build/pom.xml). Đây là baseline chuyển đổi; ứng dụng vẫn phải được implement và build.

| Thành phần | Quy tắc 1.3 |
|---|---|
| Java / Maven | Java 21; Maven 3.9.9 qua Wrapper; pin vendor, patch JDK và image digest trong PR bootstrap sau khi kiểm chứng |
| Spring Boot / Cloud | 3.5.4 / BOM 2025.0.0 cho toàn repo; không dùng wildcard hoặc nâng patch riêng từng service |
| JDBC / QR | SQL Server JDBC 12.10.1.jre11; ZXing 3.5.3 ở Booking |
| Flyway / Security / JPA / Test | Version theo Boot BOM; Cloud starter theo Cloud BOM; lưu effective-pom để biết patch thực tế |
| Domain / Gateway | Domain dùng MVC/JPA; Gateway dùng spring-cloud-starter-gateway-server-webflux, không thêm starter-web/JPA |
| Database | SQL Server 2022 Developer cho local/demo; 4 DB, 4 runtime login riêng; migration credential riêng |
| Cấu hình | ddl-auto=validate, open-in-view=false; Flyway enabled, baseline-on-migrate=false, clean-disabled=true; UTC |

Bootstrap theo thứ tự: JDK 21 → parent và 7 app → 4 DB và Flyway → Config/Discovery → JWT/gateway/domain → Docker/Compose. Config và Discovery không phụ thuộc lẫn nhau để khởi động; secret lấy từ env hoặc secret store. Runtime không dùng sa/db_owner. Frontend giữ lockfile nguồn; scaffold chưa phải UI hoàn thiện.

Baseline này kế thừa source, không bảo đảm còn được hỗ trợ hoặc đủ bảo mật cho production tại thời điểm triển khai. Trước public deployment phải review support/advisory và nâng đồng bộ qua PR có test. Không tự chuyển sang Boot 4.

## 23. Tiêu chuẩn 2 — API và command

Nguồn chuẩn: [quy ước API](docs/api.md), [OpenAPI core 1.0.0](docs/contracts/core.yaml). Hợp đồng OpenAPI 3.0.3 có 23 operation và 30 schema: JWKS, login/logout/me, hold, preview/create/read/cancel booking, demo payment, ticket/check-in, watchlist, metadata/snapshot/quote và init/close sales nội bộ.

**Phạm vi còn phải mở rộng:** CRUD quản trị, profile, register/reset và upload chưa được mô tả đầy đủ DTO trong core. Bổ sung contract trước code. Cashier nằm ngoài phạm vi 1.4; bỏ operation/role cashier khỏi contract đích nếu có. Không coi core là toàn bộ API cần triển khai.

- API ngoài dùng `/api/v1`; nội bộ dùng `/internal/v1`, không public qua gateway. Client không gọi port service.
- ID API là string; giữ ID cũ, ID mới UUID lowercase 36 ký tự. Room vẫn INT ở persistence. JSON camelCase; tiền number tối đa hai chữ số thập phân, backend BigDecimal/DECIMAL(19,2), currency VND; thời điểm UTC ISO-8601 có Z.
- Error `{code,message,traceId,fieldErrors}`; fieldErrors là map field → array string. Request DTO reject unknown fields. List `{items,page,size,totalElements,totalPages}`, page từ 0, size mặc định 20, tối đa 100.
- Idempotency-Key: ASCII printable không chứa space, dài 1–128; unique(actor,operation,key), phân biệt hoa/thường BIN2. Hash payload đã chuẩn hóa kèm operation version; sort tập ghế, gộp và sort concession. Cùng payload replay 200; khác payload 409; đang xử lý 409 REQUEST_IN_PROGRESS với Retry-After: 1; retention tối thiểu 24 giờ.
- Demo SUCCESS: booking PAID, inventory SOLD và phát vé trong một transaction. FAILED: payment FAILED, booking CANCELLED, nhả ghế và quota đúng owner; không retry FAILED thành SUCCEEDED. Mua lại phải tạo booking mới. Chỉ bật profile demo.
- Close sales chặn hold/create mới; booking chờ thanh toán đã tạo vẫn được trả tiền tới expiresAt. Đây không phải hủy suất hoặc refund. Init retry không mở lại sales đã CLOSED. Cinema lưu Sales_Workflow trước HTTP, retry cùng key/payload sau restart.
- Không giữ khóa SQL khi chờ HTTP. Lấy quote/snapshot trước, kiểm tra version/trạng thái/hạn dưới lock. Reviewer producer và consumer duyệt contract trước merge; adapter legacy có test tương thích.

## 24. Tiêu chuẩn 3 — Schema, migration và dữ liệu cũ

Nguồn chuẩn: [database](docs/database.md), [migration](docs/migration.md), [ownership](reference/database/ownership.json). Có 4 DDL tham chiếu, tổng 30 bảng, dành cho 4 database mới rỗng. Đây không phải ETL tự động hoặc script nâng cấp trực tiếp monolith.

| Domain | Bảng và quyết định chính |
|---|---|
| Identity | Customer, Staff, Shift_Schedule; Principal, Principal_Role, Password_Reset_Token, Service_Client, Service_Client_Grant |
| Catalog | Movie/Concession thêm active và version; Movie_Rating unique(movie,user); Watchlist unique(customer,movie), chỉ giữ FK Movie |
| Cinema | Template, Template_Seat, Room, Showtime; Room–Template ManyToOne; Showtime UTC; Sales_Workflow bền vững cho publish/close |
| Booking | Voucher, Sales_Snapshot, Booking, SeatHold, ActiveHold, Showtime_Seat, SeatHold_Item, Booking_Detail, Payment, Ticket, Voucher_Reservation, IdempotencyRecord, Ticket_Legacy_Archive |

- Mọi ID chuỗi nguồn VARCHAR(8) mở rộng VARCHAR(36), giữ giá trị cũ; Room INT IDENTITY, Watchlist BIGINT IDENTITY. Không FK/JPA link sang DB của service khác.
- Payment.booking_id và Ticket.booking_id unique; bỏ Booking.payment_id dư. Vé trùng nguồn phải xác định bản ghi hiệu lực và archive bằng quyết định có bằng chứng, không xóa tự động.
- Inventory AVAILABLE không owner; HELD có đúng một hold hoặc booking owner; SOLD chỉ có booking owner. Consume hold chuyển owner sang booking nguyên tử. Unique(suất,ghế vật lý) không thay thế lock/CAS chống tranh ghế.
- ActiveHold có PK(customer,showtime), composite FK trỏ đúng hold của owner/suất; khóa key range khi slot chưa tồn tại. SeatHold_Item giữ lịch sử tập ghế. Expiry chỉ cập nhật đúng owner và trạng thái, không suy đoán từ RAM.
- Voucher.used_count là đã consume; reserved_count là đơn chờ. Voucher_Reservation theo booking bảo đảm hoàn một lần. Idempotency result ghi cùng transaction nghiệp vụ; không commit PROCESSING riêng mà thiếu lease/recovery.
- Money DECIMAL(19,2); computed Detail total chỉ đọc; thời điểm DATETIME2(3) UTC. Ngày/giờ ca làm trong dữ liệu lịch sử không phải UTC instant. Timezone dữ liệu cũ phải xác minh trước khi đổi.
- Tích hợp V1 vào migration domain và test trên SQL Server. Sau khi áp dụng không sửa V1, thêm V2. Không dùng ddl-auto=update, clean hoặc baseline để lờ lỗi.

Cutover: backup → preflight → quyết định dữ liệu mơ hồ → ETL trên bản sao → đối soát ID/count/money/reference → chặn ghi nguồn cũ → chuyển route → smoke test. Chỉ một nguồn ghi cho mỗi domain. Khi đã có ghi mới, không rollback route về legacy nếu chưa đối soát delta.

## 25. Tiêu chuẩn 4 — JWT và phân quyền

Nguồn chuẩn: [security](docs/security.md). Identity ký RS256 với RSA tối thiểu 2048 bit; public JWKS tại `/.well-known/jwks.json`; private key chỉ ở Identity và không commit.

| Thành phần | Hợp đồng |
|---|---|
| User token | sub=Principal UUID; iss=JWT_ISSUER; aud=cinema-api; jti UUID; iat/nbf/exp tính bằng giây UTC; TTL 900 giây; clock skew tối đa 30 giây |
| Account | accountType CUSTOMER/STAFF; customerId hoặc staffId đúng loại lấy từ DB, không suy từ username |
| Roles | USER, ADMIN; converter thêm ROLE_; không tự thêm role CUSTOMER |
| Validation | Gateway và mọi service kiểm chữ ký, alg, kid, iss, aud, exp, nbf, claim, role và ownership |
| Logout / refresh | Chưa có refresh; hết 15 phút phải đăng nhập lại. Logout xóa token phía client; JWT cũ còn hợp lệ tới expiry, không hứa thu hồi tức thời |
| Service token | sub=svc:service-name; token_kind=service; aud là service nhận; scope allowlist; TTL 300 giây; security chain riêng cho internal |
| Password | BCrypt cost 12 cho mật khẩu mới; hash nguồn bất thường phải xử lý có kiểm soát; reset token chỉ lưu hash, TTL 15 phút, dùng một lần |

Username trim và lowercase(Locale.ROOT), Principal.normalized_username unique; va chạm Customer/Staff phải đối soát trước migrate. Key rotation giữ public key cũ ít nhất 15 phút + 30 giây sau token cuối ký bằng key đó. JWK endpoint nội bộ và issuer công khai có thể khác host nhưng issuer claim phải khớp chính xác.

Scope nội bộ: Cinema→Catalog `catalog:read`; Cinema→Booking `sales:write`; Booking→Cinema `showtimes:read`; Booking→Catalog `concessions:quote`. Identity cấp service token qua Basic credential riêng và grant phía server; không tin scope client tự chọn. User token không được gọi internal; service token không giả danh USER.

401 cho token không hợp lệ; 403 cho thiếu quyền/ownership/scope. Route admin đứng trước matcher rộng; bearer stateless tách session legacy và CSRF. Đổi role/password/disable chưa thu hồi tức thời JWT cũ trong bản này. Nếu cần thu hồi ngay, phải thêm ADR và revocation dùng chung; denylist RAM không đủ cho nhiều instance.

## 26. Nguồn, thứ tự ưu tiên và kiểm chứng

[docs/sources.md](docs/sources.md) gắn từng nhóm quyết định với source, rubric và tài liệu Spring/OpenAPI/Flyway. Số lượng 4 service, TTL, UUID, logout và policy payment là quyết định dự án; không phải sách bắt buộc như vậy. Không dùng đề xuất cũ trong chat để ghi đè bản đã commit.

Khi có xung đột: rubric chính thức quyết định yêu cầu đánh giá; ADR và AGENTS quyết định kiến trúc; OpenAPI quyết định wire contract; Flyway migration đã áp dụng quyết định schema hiện hành. Phát hiện mâu thuẫn phải sửa đồng bộ trước merge, không tự chọn một file rồi bỏ qua file khác.

Giới hạn bản 1.3: đã tạo tài liệu, parent tham chiếu, OpenAPI core và DDL đích; chưa sửa hoặc đẩy source ứng dụng lên GitHub, chưa có 7 app chạy, chưa thực thi SQL/ETL hoặc test nhiều instance. [verification](docs/verification.md) ghi kiểm tra đã chạy và chưa chạy. Team có thể bắt đầu bootstrap và code core theo hợp đồng, nhưng chỉ nghiệm thu sau các cổng mục 11–14 và docs/acceptance.md.

## 27. Nghiệm thu và đồng bộ backlog cho phạm vi 1.4

- Flutter: đăng ký/đăng nhập, xem phim/lịch/ghế, hold, đặt vé, voucher, thanh toán demo, xem vé, hủy theo policy, hồ sơ/watchlist/rating theo contract. Chỉ đánh dấu hoàn tất khi màn hình thực tế đã tích hợp và chạy qua Gateway.
- ADMIN qua API: CRUD danh mục/phòng/lịch/voucher; publish/close; check-in vé hợp lệ. Chứng minh USER bị chặn quản trị và check-in, không đọc/sửa đơn khách khác; token role nhân viên cũ không được cấp/quyền truy cập ở hệ mới.
- Check-in: chỉ ADMIN; vé phải thuộc booking PAID, còn hiệu lực theo contract; kiểm tra và cập nhật nguyên tử, gọi đồng thời không ghi nhận hai lượt, lưu actor/thời điểm. Không tự check-in ngay sau thanh toán online.
- Sáu thành viên tiếp tục chia theo domain, không theo role. Ngân: hạ tầng/DB/Gateway; Vy: Identity/JWT/customer/admin; Thịnh: Catalog/tích hợp Flutter chung/kiểm thử; Thọ: Cinema/mở đóng bán/migration; Thái: hold/booking/vé/ADMIN check-in; Hiếu: payment/voucher/idempotency/Docker.
- Backlog Excel chưa được sửa trong lần cập nhật Markdown này. Trước giao việc, thay task nhân viên/ca làm của Vy bằng quản lý truy cập hai role và kiểm thử quyền/owner; bỏ thu tiền quầy mới nhưng giữ đối soát payment lịch sử của Hiếu; thay tích hợp UI quầy của Hiếu bằng tích hợp Flutter voucher/payment và kiểm thử timeout/gửi lặp. Thịnh phụ trách tích hợp client chung, Hiếu chỉ luồng voucher/payment để không trùng trách nhiệm.
- Không coi số task hoặc tài liệu là bằng chứng hoàn thành. Mỗi thành viên có đầu ra code, test và phần demo/giải thích riêng; đối chiếu rubric và yêu cầu giảng viên. Bonus không thay các tiêu chí chính còn thiếu.
