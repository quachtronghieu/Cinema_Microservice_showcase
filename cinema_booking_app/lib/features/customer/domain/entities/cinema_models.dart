class CinemaConcession {
  const CinemaConcession({
    required this.id,
    required this.type,
    required this.name,
    required this.description,
    required this.price,
    required this.image,
  });

  final String id;
  final String type;
  final String name;
  final String description;
  final int price;
  final String image;
}