import '../../domain/entities/cinema_models.dart';
import '../dtos/concession_dto.dart';

CinemaConcession mapConcessionDto(
  ConcessionDto dto, {
  required Uri backendBaseUrl,
}) {
  if ((backendBaseUrl.scheme != 'http' && backendBaseUrl.scheme != 'https') ||
      backendBaseUrl.host.isEmpty) {
    throw ArgumentError('Base URL backend không hợp lệ.');
  }

  if (!dto.price.isFinite || dto.price < 0) {
    throw const FormatException('Giá bắp/nước không hợp lệ.');
  }

  final price = dto.price.toInt();

  if (dto.price != price) {
    throw const FormatException(
      'Model hiện tại chỉ hỗ trợ giá nguyên theo VND.',
    );
  }

  final origin = backendBaseUrl.resolve('/');
  final imagePath = dto.img.trim();

  return CinemaConcession(
    id: dto.concessionId,
    type: dto.type,
    name: dto.name,
    description: dto.description,
    price: price,
    image: imagePath.isEmpty ? '' : origin.resolve(imagePath).toString(),
  );
}
