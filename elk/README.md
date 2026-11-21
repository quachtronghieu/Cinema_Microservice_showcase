# Log tập trung bằng ELK (task 8)

Chỉ dùng cho local và demo. Elasticsearch tắt bảo mật nên hai cổng 9200 và 5601 chỉ mở trên `127.0.0.1`; không đưa stack này ra Internet.

Các service ghi log JSON (chuẩn ECS) ra file, Filebeat đọc thư mục log và gửi vào Elasticsearch, Kibana dùng để tìm kiếm. Không phải sửa `application.yml` hay code của service.

## Yêu cầu

Docker cần khoảng 2 GB RAM trống cho ELK, ngoài phần SQL Server đang dùng. Lần đầu tải khoảng 4,5 GB image.

## Chạy

1. Bật ELK từ thư mục gốc repo:
   ```powershell
   docker compose -f docker-compose.elk.yml up -d
   ```
   Thư mục log mặc định là `../cinema-logs`, nằm cạnh repo để log không lọt vào Git. Muốn đổi thì đặt biến `CINEMA_LOG_DIR`.
2. Trong Run Configuration của từng ứng dụng, thêm 2 biến môi trường. Tên file khác nhau cho mỗi ứng dụng (`discovery-server`, `api-gateway`, `identity-service`, `catalog-service`, `cinema-service`, `booking-service`), ví dụ với Booking:
   ```text
   LOGGING_FILE_NAME=<đường dẫn tuyệt đối>/cinema-logs/booking-service.log
   LOGGING_STRUCTURED_FORMAT_FILE=ecs
   ```
   Console vẫn in log dạng chữ như cũ; chỉ file mới là JSON.
3. Bật lại ứng dụng, mở <http://localhost:5601>, vào **Stack Management > Data Views**, tạo data view với pattern `filebeat-*`, trường thời gian `@timestamp`. Xem log ở **Discover**.

## Tìm log (KQL trong Discover)

| Cần tìm | Câu truy vấn |
|---|---|
| Theo service | `service.name : "booking-service"` |
| Theo traceId | `traceId : "<id>"` |
| Theo lượt giữ ghế | `holdId : "<id>"` |
| Theo suất chiếu | `showtimeId : "<id>"` |
| Theo sự kiện | `event.action : "SEAT_HOLD_REJECTED"` |

Sự kiện giữ ghế: `SEAT_HOLD_CREATED`, `SEAT_HOLD_REPLACED`, `SEAT_HOLD_RELEASED`, `SEAT_HOLD_EXPIRED`, `SEAT_HOLD_REJECTED` (kèm `reason`, ví dụ `SEAT_UNAVAILABLE`). Log chỉ chứa ID, không có thông tin cá nhân.

## Giới hạn

- Filebeat loại bỏ dòng log chứa link đặt lại mật khẩu, nhưng file log trên máy vẫn có dòng đó cho tới khi Identity thôi ghi. Không chia sẻ thư mục log.

## Dừng

```powershell
docker compose -f docker-compose.elk.yml down      # giữ dữ liệu log đã nạp
docker compose -f docker-compose.elk.yml down -v   # xóa luôn dữ liệu log đã nạp
```
