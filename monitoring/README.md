# Cinema Microservices - Monitoring Infrastructure (Prometheus & Grafana)

## 1. Overview
Hệ thống giám sát hiệu năng và trạng thái sức khỏe dành cho kiến trúc Cinema Microservices (MSS301).
- **Prometheus (Port 9090):** Thu thập (scrape) metrics từ Spring Boot Actuator qua endpoint `/actuator/prometheus`.
- **Grafana (Port 3000):** Trực quan hóa dữ liệu hiệu năng thông qua Dashboard dựng sẵn (Real-time TPS, HTTP Status, CPU, RAM).

## 2. Ports & Target Endpoints

| Service | Port | Endpoint Prometheus Metrics |
|---|:---:|---|
| **Discovery Server (Eureka)** | `8761` | `http://localhost:8761/actuator/prometheus` |
| **API Gateway** | `8080` | `http://localhost:8080/actuator/prometheus` |
| **Identity Service** | `8081` | `http://localhost:8081/actuator/prometheus` |
| **Catalog Service** | `8082` | `http://localhost:8082/actuator/prometheus` |
| **Booking Service** | `8083` | `http://localhost:8083/actuator/prometheus` |
| **Cinema Service** | `8084` | `http://localhost:8084/actuator/prometheus` |

## 3. How to Run Monitoring

### Khởi động Prometheus và Grafana bằng Docker Compose:
```powershell
docker-compose -f docker-compose.monitoring.yml up -d
```

### Truy cập giao diện:
1. **Prometheus Web UI:** `http://localhost:9090`
   - Kiểm tra trạng thái kết nối các service: Menu **Status** -> **Targets**.
2. **Grafana Dashboard:** `http://localhost:3000`
   - Đăng nhập mặc định: `admin` / `admin`
   - Mở Dashboard: **Dashboards** -> thư mục **Microservices** -> **Cinema Microservices Overview**.

### Dừng cụm giám sát:
```powershell
docker-compose -f docker-compose.monitoring.yml down
```

## 4. Distributed Tracing (Zipkin)

Gateway, Identity, Catalog, Cinema và Booking dùng Micrometer Tracing (Brave) và truyền trace qua header W3C `traceparent`, kể cả các lời gọi nội bộ (Cinema gọi Catalog, Identity, Booking bằng `RestClient`; Booking gọi Catalog bằng Feign). Header `X-Trace-Id` và trường `traceId` trong error JSON chính là trace ID trên Zipkin, nên từ một lỗi có thể tìm thẳng ra luồng request.

Client gọi service khác phải tạo từ `RestClient.Builder` do Spring Boot inject. `RestClient.builder()` tự tạo không gửi `traceparent`, làm luồng bị cắt thành nhiều trace; `ClientTracingTest` của Cinema kiểm tra điều này.

Mặc định chỉ ghi trace ID vào log, không gửi span đi, để máy không chạy Zipkin không bị log cảnh báo. Muốn xem luồng request:

```powershell
docker-compose -f docker-compose.monitoring.yml up -d zipkin
$env:TRACING_EXPORT_ENABLED = 'true'
# Khởi động lại Gateway, Identity, Catalog, Cinema và Booking trong cùng phiên terminal này
```

Chạy bằng IntelliJ thì thêm `TRACING_EXPORT_ENABLED=true` vào Environment variables của 5 run configuration đó. Chạy toàn bộ bằng Compose:

```powershell
$env:TRACING_EXPORT_ENABLED = 'true'
docker compose --profile monitoring up -d --build
```

Gửi một request qua Gateway, lấy giá trị header `X-Trace-Id`, rồi mở `http://localhost:9411/zipkin/traces/<trace-id>`.

| Biến môi trường | Mặc định | Ý nghĩa |
|---|---|---|
| `TRACING_EXPORT_ENABLED` | `false` | Bật gửi span tới Zipkin |
| `ZIPKIN_ENDPOINT` | `http://localhost:9411/api/v2/spans` | Địa chỉ nhận span |
| `TRACING_SAMPLING_PROBABILITY` | `1.0` | Tỷ lệ request được ghi trace |

Giới hạn: Zipkin chạy bộ nhớ trong nên restart sẽ mất trace. Discovery Server không gắn tracing vì không nằm trên đường đi của request nghiệp vụ.
