class ConcessionDto {
  const ConcessionDto({
    required this.concessionId,
    required this.type,
    required this.name,
    required this.price,
    required this.description,
    required this.img,
  });

  final String concessionId;
  final String type;
  final String name;
  final num price;
  final String description;
  final String img;

  factory ConcessionDto.fromJson(Map<String, dynamic> json) {
    final id = json['concessionId'];
    final type = json['type'];
    final name = json['name'];
    final price = json['price'];

    if (id is! String ||
        id.trim().isEmpty ||
        type is! String ||
        (type != 'PC' && type != 'DR') ||
        name is! String ||
        name.trim().isEmpty ||
        price is! num ||
        !price.isFinite ||
        price <= 0) {
      throw const FormatException('Dữ liệu bắp/nước không hợp lệ.');
    }

    return ConcessionDto(
      concessionId: id,
      type: type,
      name: name,
      price: price,
      description: json['description'] as String? ?? '',
      img: json['img'] as String? ?? '',
    );
  }
}