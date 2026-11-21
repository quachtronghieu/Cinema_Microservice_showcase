class ResetPasswordRequestDto {
  const ResetPasswordRequestDto({
    required this.id,
    required this.resetTimestamp,
    required this.resetCode,
    required this.newPassword,
    required this.confirmPassword,
  });

  final String id;
  final int resetTimestamp;
  final String resetCode;
  final String newPassword;
  final String confirmPassword;

  Map<String, dynamic> toJson() => {
    'id': id,
    'resetTimestamp': resetTimestamp,
    'resetCode': resetCode,
    'newPassword': newPassword,
    'confirmPassword': confirmPassword,
  };
}
