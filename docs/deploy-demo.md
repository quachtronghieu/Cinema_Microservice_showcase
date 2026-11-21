# Triển khai demo công khai qua HTTPS

Tài liệu này dành cho người dựng demo, cần có một server Linux chạy Docker. Stack gồm Discovery, Identity, Catalog, Gateway, SQL Server, Zipkin và Caddy. Caddy là cổng công khai duy nhất ngoài SSH: nó tự xin và gia hạn chứng chỉ HTTPS, rồi chuyển tiếp tới Gateway.

## Việc phải làm trước khi mở public

1. **Thu hồi mật khẩu SMTP đang nằm trong Git.** `services/identity-service/src/main/resources/application.yml` có sẵn username và mật khẩu Gmail làm giá trị mặc định. Compose này đã ghi đè để demo không dùng chúng, nhưng chúng vẫn nằm trong lịch sử Git, nên phải thu hồi app password ở tài khoản Google rồi xóa giá trị mặc định.
2. **Xử lý tài khoản seed.** Migration V2 của Identity tạo tài khoản ADMIN và customer với email cá nhân và hash mật khẩu cũ của thành viên nhóm. Mọi ai biết mật khẩu cũ đều đăng nhập được vào demo. Hãy đổi mật khẩu các tài khoản này ngay sau lần chạy đầu, hoặc thay V2 bằng dữ liệu demo trước khi có database thật (migration đã áp dụng thì không được sửa, mục 10 AGENTS).
3. **Biết rõ phạm vi.** Compose này chưa có Cinema và Booking nên suất chiếu, ghế, đặt vé và thanh toán đều trả 503. Hai service đó đã có mã nguồn nhưng chưa có Dockerfile. Demo hiện chỉ chứng minh được Identity và Catalog.

## Chọn server

Stack cần tối thiểu khoảng 6 GB RAM: riêng SQL Server cần 2 GB, bốn ứng dụng Java dùng thêm khoảng 2 đến 3 GB. Con số này là ước tính theo giới hạn heap trong Compose, chưa đo trên server thật, và sẽ tăng khi thêm Cinema và Booking. Các gói free tier thường không đủ RAM cho SQL Server, nên cần kiểm tra giá và tài nguyên trước khi chọn nhà cung cấp.

Chứng chỉ Let's Encrypt cần tên miền trỏ về server và cổng 80, 443 mở từ Internet. Nếu chưa có tên miền, dùng được dạng `203-0-113-10.sslip.io`, thay bằng IP thật của server.

## Các bước

```bash
# Trên server, từ thư mục gốc repository
cp deploy/demo.env.example deploy/demo.env
# Điền toàn bộ giá trị theo hướng dẫn trong file, dùng mật khẩu ngẫu nhiên riêng cho từng biến
docker compose -f deploy/compose.demo.yaml --env-file deploy/demo.env up -d --build
docker compose -f deploy/compose.demo.yaml --env-file deploy/demo.env ps
```

Lần đầu mất vài phút vì phải build image và chạy migration. Đợi các service báo `healthy`.

## Kiểm tra

```bash
curl -i https://$DEMO_DOMAIN/actuator/health          # 200
curl -i https://$DEMO_DOMAIN/api/v1/movies            # 200, có header X-Trace-Id
curl -i https://$DEMO_DOMAIN/api/v1/admin/movies      # 401
curl -i http://$DEMO_DOMAIN/actuator/health           # chuyển hướng sang HTTPS
```

Database mới chưa có phim. Dùng ADMIN tạo phim qua [bộ Postman](postman/cinema-gateway.postman_collection.json), hoặc chạy [script import](migration/catalog_movie_import.sql) nếu có sẵn dữ liệu monolith trên cùng SQL Server.

Build Flutter trỏ vào demo:

```bash
flutter build apk --dart-define=API_BASE_URL=https://$DEMO_DOMAIN
```

## Xem trace

Zipkin chỉ mở trên `127.0.0.1` của server vì trace lộ đường dẫn request. Từ máy cá nhân:

```bash
ssh -L 9411:127.0.0.1:9411 <user>@<server>
# Mở http://localhost:9411/zipkin/traces/<giá trị X-Trace-Id>
```

## Dừng và dữ liệu

```bash
docker compose -f deploy/compose.demo.yaml --env-file deploy/demo.env down      # Giữ dữ liệu
docker compose -f deploy/compose.demo.yaml --env-file deploy/demo.env down -v   # XÓA toàn bộ dữ liệu và chứng chỉ
```

## Giới hạn

- Một instance mỗi service, SQL Server chạy trong container, chưa có backup tự động. Đây là demo, không phải production.
- Trace nằm trong bộ nhớ của Zipkin nên mất khi restart.
- `depends_on` chỉ đợi khi khởi động. Nếu SQL Server restart giữa chừng thì các service phải tự kết nối lại.
- Config Server chưa có; cấu hình đang lấy từ biến môi trường.
- `compose.yaml` ở gốc repo theo AGENTS mục 9 vẫn chưa có. File này chỉ phục vụ demo và có thể gộp vào khi nhóm dựng Compose chung.
