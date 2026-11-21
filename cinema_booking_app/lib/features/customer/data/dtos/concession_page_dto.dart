import 'concession_dto.dart';

class ConcessionPageDto {
  const ConcessionPageDto({
    required this.items,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  final List<ConcessionDto> items;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  factory ConcessionPageDto.fromJson(Map<String, dynamic> json) {
    final items = json['items'];
    final page = json['page'];
    final size = json['size'];
    final totalElements = json['totalElements'];
    final totalPages = json['totalPages'];

    if (items is! List ||
        page is! int ||
        page < 0 ||
        size is! int ||
        size < 1 ||
        size > 100 ||
        totalElements is! int ||
        totalElements < 0 ||
        totalPages is! int ||
        totalPages < 0 ||
        totalPages != (totalElements + size - 1) ~/ size ||
        items.length > size) {
      throw const FormatException(
        'Thông tin phân trang concession không hợp lệ.',
      );
    }

    return ConcessionPageDto(
      items: items
          .map((item) {
            if (item is! Map<String, dynamic>) {
              throw const FormatException('Dữ liệu concession không hợp lệ.');
            }
            return ConcessionDto.fromJson(item);
          })
          .toList(growable: false),
      page: page,
      size: size,
      totalElements: totalElements,
      totalPages: totalPages,
    );
  }
}
