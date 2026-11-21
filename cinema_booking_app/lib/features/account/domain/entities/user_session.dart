class UserSession {
  const UserSession({
    this.userId,
    required this.username,
    this.role,
    required this.isAuthenticated,
  });

  final String? userId;
  final String username;
  final String? role;
  final bool isAuthenticated;
}
