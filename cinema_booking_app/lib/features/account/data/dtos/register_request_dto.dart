class RegisterRequestDto {
  const RegisterRequestDto({
    required this.username,
    required this.password,
    required this.confirmPassword,
    required this.email,
    required this.phone,
    required this.dob,
    required this.sex,
  });

  final String username;
  final String password;
  final String confirmPassword;
  final String email;
  final String phone;
  final String dob;
  final bool sex;

  Map<String, dynamic> toJson() => {
    'username': username,
    'password': password,
    'confirmPassword': confirmPassword,
    'email': email,
    'phone': phone,
    'dob': dob,
    'sex': sex,
  };
}
