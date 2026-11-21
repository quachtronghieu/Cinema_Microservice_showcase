class UserInfoDto {
  const UserInfoDto({this.userId, required this.username, this.role});

  final String? userId;
  final String username;
  final String? role;

  factory UserInfoDto.fromJson(Map<String, dynamic> json) {
    final rawId = json['userId'] ?? json['id'];
    return UserInfoDto(
      userId: rawId?.toString(),
      username: json['username'] as String? ?? '',
      role: json['role'] as String?,
    );
  }
}
