# Cinema Management Microservices Platform (MSS301)

Hệ thống kiến trúc Microservices phân tán dành cho nền tảng quản lý rạp chiếu phim và đặt vé trực tuyến, phát triển theo chuẩn kiến trúc Spring Boot 3 & Spring Cloud tại [AGENTS.md](AGENTS.md).

---

## 1. Bản đồ Dịch vụ & Cổng mạng (Port Mapping)

| Service | Port | Trách nhiệm chính & Database sở hữu |
|---|---|---|
| **api-gateway** | `8080` | Điểm truy cập duy nhất (Ingress Routing, JWT Verification, Rate Limiting) |
| **discovery-server** | `8761` | Eureka Service Registry & Health Registry |
| **identity-service** | `8081` | Đăng ký, Đăng nhập (JWT RS256), Quản lý tài khoản, Phân quyền Admin (`identity_db`) |
| **catalog-service** | `8082` | Phim ảnh, Đồ ăn/uống (Concession), Danh sách yêu thích (`catalog_db`) |
| **booking-service** | `8083` | Giữ ghế, Đặt vé, Thanh toán demo, Quản lý Voucher, Vé (`booking_db`) |
| **cinema-service** | `8084` | Quản lý phòng chiếu, Sơ đồ ghế vật lý, Lịch chiếu (`cinema_db`) |
| **sqlserver** | `1433` | Microsoft SQL Server 2022 (Lưu trữ 4 database độc lập) |
| **prometheus** *(Profile)* | `9090` | Thu thập metrics giám sát toàn hệ thống |
| **grafana** *(Profile)* | `3000` | Dashboard trực quan hóa hiệu năng & lưu lượng |

---

## 2. Hướng dẫn Khởi chạy Hệ thống với 1 Câu lệnh (One Command Run)

### Bước 1: Chuẩn bị Môi trường
Đảm bảo máy đã cài đặt **Docker Desktop** và Docker Engine đang hoạt động.

### Bước 2: Thiết lập Biến môi trường
Copy file mẫu cấu hình `.env.example` thành `.env` (file này đã có sẵn các giá trị mật khẩu và cấu hình an toàn mặc định):
```bash
# Trên Linux/macOS:
cp .env.example .env

# Trên Windows PowerShell:
Copy-Item .env.example .env
```

### Bước 3: Khởi chạy Toàn bộ Hệ thống
Chạy 1 câu lệnh duy nhất từ thư mục gốc repository:
```bash
docker compose up -d --build
# Hoặc:
docker-compose up -d --build
```

> **Ghi chú:** Để bật thêm cả stack giám sát (Prometheus & Grafana), thêm cờ `--profile monitoring`:
> ```bash
> docker compose --profile monitoring up -d
> ```

---

## 3. Thứ tự Khởi động & Cơ chế Tự phục hồi (Dependency Healthchecks)

Hệ thống được điều phối qua `compose.yaml` với cơ chế kiểm tra sức khỏe tự động (`service_healthy`):
1. **`sqlserver`** khởi động trước và vượt qua kiểm tra `SELECT 1` bằng `sqlcmd`.
2. **`db-init`** chạy một lần (`restart: "no"`) để tự động tạo 4 database độc lập: `identity_db`, `catalog_db`, `cinema_db`, `booking_db` cùng các user phân quyền tối thiểu (`_app` và `_migrator`).
3. **`discovery-server`** (Eureka) khởi động trên cổng `8761`.
4. **`identity-service`** khởi động, thực thi Flyway migration cho `identity_db`, kết nối Eureka và công bố Public Key JWKS tại `/.well-known/jwks.json`.
5. **`catalog-service`**, **`cinema-service`**, **`booking-service`** đồng loạt khởi động sau khi `identity-service` đã sẵn sàng, tự động lấy JWKS để giải mã chữ ký token và kết nối database riêng.
6. **`api-gateway`** khởi động cuối cùng sau khi toàn bộ 4 domain services đã ở trạng thái `healthy`, bắt đầu định tuyến traffic người dùng qua cổng `8080`.

---

## 4. Kịch bản Kiểm thử Full Luồng (End-to-End Verification Journey)

Sau khi hệ thống khởi chạy, mọi thao tác có thể thực hiện thông qua **API Gateway** tại `http://localhost:8080`:

### 4.1. Đăng ký tài khoản mới (`identity-service`)
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer_test",
    "password": "ChangeMe_Local_9!",
    "fullName": "Nguyen Van A",
    "email": "customer_test@example.com",
    "phoneNumber": "0912345678"
  }'
```

### 4.2. Đăng nhập để nhận JWT Token RS256 (`identity-service`)
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "customer_test",
    "password": "ChangeMe_Local_9!"
  }'
```
*Lưu giá trị `accessToken` nhận được từ phản hồi để dùng cho các bước tiếp theo.*

### 4.3. Xem danh mục phim công khai (`catalog-service`)
```bash
curl -X GET http://localhost:8080/api/v1/movies
```

### 4.4. Thêm phim vào Watchlist bằng Token của khách hàng (`catalog-service`)
```bash
curl -X POST http://localhost:8080/api/v1/customer/watchlist \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "movieId": "M000001"
  }'
```

### 4.5. Xem danh sách suất chiếu công khai (`cinema-service`)
```bash
curl -X GET http://localhost:8080/api/v1/showtimes
```

### 4.6. Giữ ghế và Đặt vé (`booking-service`)
```bash
# 1. Giữ ghế 5 phút
curl -X POST http://localhost:8080/api/v1/customer/seat-holds \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "showtimeId": "SW1",
    "showtimeSeatIds": ["SS1"]
  }'

# 2. Tạo booking thanh toán
curl -X POST http://localhost:8080/api/v1/customer/bookings \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "holdId": "<HOLD_ID>"
  }'
```

### 4.7. Kiểm tra phân quyền Admin (Chống leo thang đặc quyền)
```bash
# Dùng token Khách cố tình gọi API Admin -> Bị chặn 403 FORBIDDEN
curl -i -X POST http://localhost:8080/api/v1/admin/vouchers \
  -H "Authorization: Bearer <CUSTOMER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"code":"SALE50"}'
```

### 4.8. Đăng xuất và Hủy phiên (`identity-service`)
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 5. Dừng Hệ thống
```bash
docker compose down
# Hoặc xóa cả volume dữ liệu nếu muốn cài đặt lại từ đầu:
docker compose down -v
```
