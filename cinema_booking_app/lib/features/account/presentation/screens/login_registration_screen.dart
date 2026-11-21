import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:go_router/go_router.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../../../core/ui/cinema_widgets.dart';
import '../../../../core/ui/validators.dart';
import '../view_models/account_view_model.dart';

class LoginRegistrationScreen extends StatefulWidget {
  const LoginRegistrationScreen({super.key});
  @override
  State<LoginRegistrationScreen> createState() =>
      _LoginRegistrationScreenState();
}

class _LoginRegistrationScreenState extends State<LoginRegistrationScreen> {
  final _formKey = GlobalKey<FormState>();
  final _username = TextEditingController();
  final _password = TextEditingController();
  final _confirmPassword = TextEditingController();
  final _email = TextEditingController();
  final _phone = TextEditingController();
  final _birthDate = TextEditingController();
  String _gender = 'Nam';
  bool _isRegister = false;

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    _confirmPassword.dispose();
    _email.dispose();
    _phone.dispose();
    _birthDate.dispose();
    super.dispose();
  }

  Future<void> _pickBirthDate() async {
    final now = DateTime.now();
    final date = await showDatePicker(
      context: context,
      initialDate: DateTime(now.year - 18, now.month, now.day),
      firstDate: DateTime(1900),
      lastDate: now,
    );
    if (date != null) _birthDate.text = formatDateOfBirth(date);
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final vm = context.read<AccountViewModel>();
    if (_isRegister) {
      final success = await vm.register(
        username: _username.text.trim(),
        password: _password.text,
        confirmPassword: _confirmPassword.text,
        email: _email.text.trim(),
        phone: _phone.text.trim(),
        dob: _birthDate.text,
        sex: _gender == 'Nam',
      );
      if (!mounted) return;
      if (success) {
        showCinemaMessage(context, 'Đăng ký thành công! Vui lòng đăng nhập.');
        setState(() => _isRegister = false);
      }
    } else {
      final success = await vm.login(_username.text.trim(), _password.text);
      if (mounted && success) context.go('/account');
    }
  }

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<AccountViewModel>();
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 460),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Center(
                      child: Column(
                        children: [
                          RichText(
                            text: TextSpan(
                              style: Theme.of(context).textTheme.displaySmall
                                  ?.copyWith(fontWeight: FontWeight.w900),
                              children: const [
                                TextSpan(text: 'CINEMA'),
                                TextSpan(
                                  text: '.',
                                  style: TextStyle(color: CinemaColors.red),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 8),
                          const Text('Đặt vé xem phim nhanh trên điện thoại.'),
                        ],
                      ),
                    ),
                    const SizedBox(height: 32),
                    SegmentedButton<bool>(
                      segments: const [
                        ButtonSegment(value: false, label: Text('Đăng nhập')),
                        ButtonSegment(value: true, label: Text('Đăng ký')),
                      ],
                      selected: {_isRegister},
                      onSelectionChanged: (value) =>
                          setState(() => _isRegister = value.first),
                    ),
                    const SizedBox(height: 24),
                    TextFormField(
                      controller: _username,
                      decoration: const InputDecoration(labelText: 'Username'),
                      validator: validateUsername,
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _password,
                      obscureText: true,
                      decoration: const InputDecoration(labelText: 'Mật khẩu'),
                      validator: validatePassword,
                    ),
                    if (!_isRegister)
                      Align(
                        alignment: Alignment.centerRight,
                        child: TextButton(
                          onPressed: () => showModalBottomSheet<void>(
                            context: context,
                            isScrollControlled: true,
                            backgroundColor: CinemaColors.surface,
                            shape: const RoundedRectangleBorder(
                              borderRadius: BorderRadius.vertical(
                                top: Radius.circular(24),
                              ),
                            ),
                            builder: (_) => const _ForgotPasswordSheet(),
                          ),
                          child: const Text('Quên mật khẩu?'),
                        ),
                      ),
                    if (_isRegister) ...[
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _confirmPassword,
                        obscureText: true,
                        decoration: const InputDecoration(
                          labelText: 'Xác nhận mật khẩu',
                        ),
                        validator: (value) => value != _password.text
                            ? 'Mật khẩu xác nhận không khớp.'
                            : null,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _email,
                        keyboardType: TextInputType.emailAddress,
                        decoration: const InputDecoration(labelText: 'Email'),
                        validator: validateEmail,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _phone,
                        keyboardType: TextInputType.phone,
                        decoration: const InputDecoration(
                          labelText: 'Số điện thoại',
                        ),
                        validator: validatePhone,
                      ),
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _birthDate,
                        readOnly: true,
                        onTap: _pickBirthDate,
                        decoration: const InputDecoration(
                          labelText: 'Ngày sinh',
                        ),
                        validator: (value) => value == null || value.isEmpty
                            ? 'Ngày sinh là bắt buộc.'
                            : null,
                      ),
                      const SizedBox(height: 12),
                      DropdownButtonFormField<String>(
                        initialValue: _gender,
                        decoration: const InputDecoration(
                          labelText: 'Giới tính',
                        ),
                        items: const [
                          DropdownMenuItem(value: 'Nam', child: Text('Nam')),
                          DropdownMenuItem(value: 'Nữ', child: Text('Nữ')),
                        ],
                        onChanged: (value) {
                          if (value != null) setState(() => _gender = value);
                        },
                      ),
                    ],
                    if (vm.error != null) ...[
                      const SizedBox(height: 16),
                      Text(
                        vm.error!,
                        style: const TextStyle(color: CinemaColors.red),
                        textAlign: TextAlign.center,
                      ),
                    ],
                    const SizedBox(height: 24),
                    FilledButton(
                      onPressed: vm.busy ? null : _submit,
                      child: vm.busy
                          ? const SizedBox(
                              width: 22,
                              height: 22,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Text(_isRegister ? 'Đăng ký' : 'Đăng nhập'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _ForgotPasswordSheet extends StatefulWidget {
  const _ForgotPasswordSheet();

  @override
  State<_ForgotPasswordSheet> createState() => _ForgotPasswordSheetState();
}

class _ForgotPasswordSheetState extends State<_ForgotPasswordSheet> {
  final _emailFormKey = GlobalKey<FormState>();
  final _resetFormKey = GlobalKey<FormState>();

  final _email = TextEditingController();
  final _linkOrToken = TextEditingController();
  final _userId = TextEditingController();
  final _resetTimestamp = TextEditingController();
  final _newPassword = TextEditingController();
  final _confirmNewPassword = TextEditingController();

  bool _isResetStep = false;
  bool _submitting = false;
  bool _obscureNewPassword = true;
  bool _obscureConfirmPassword = true;
  String? _errorMessage;

  @override
  void dispose() {
    _email.dispose();
    _linkOrToken.dispose();
    _userId.dispose();
    _resetTimestamp.dispose();
    _newPassword.dispose();
    _confirmNewPassword.dispose();
    super.dispose();
  }

  void _onLinkChanged(String value) {
    final parsed = _parseLink(value);
    if (parsed != null) {
      setState(() {
        _userId.text = parsed.$1;
        _resetTimestamp.text = parsed.$2.toString();
        _linkOrToken.text = parsed.$3;
      });
    }
  }

  static (String, int, String)? _parseLink(String input) {
    final trimmed = input.trim();
    final match = RegExp(r'reset-password/([^/\s]+)/(\d+)/([^/\s?#]+)')
        .firstMatch(trimmed);
    if (match != null) {
      final id = match.group(1);
      final ts = int.tryParse(match.group(2) ?? '');
      final code = match.group(3);
      if (id != null && ts != null && code != null) {
        return (id, ts, code);
      }
    }
    return null;
  }

  Future<void> _sendEmail() async {
    if (!_emailFormKey.currentState!.validate()) return;
    setState(() {
      _submitting = true;
      _errorMessage = null;
    });

    final vm = context.read<AccountViewModel>();
    final success = await vm.forgotPassword(_email.text.trim());
    if (!mounted) return;
    setState(() => _submitting = false);

    if (success) {
      setState(() => _isResetStep = true);
      showCinemaMessage(context, 'Mã xác thực đã được gửi về email của bạn!');
    } else {
      String err =
          vm.error ??
          'Không thể gửi email. Vui lòng kiểm tra lại địa chỉ email.';
      if (err.toLowerCase().contains('no account found')) {
        err = 'Không tìm thấy tài khoản với email này trên hệ thống.';
      }
      setState(() {
        _errorMessage = err;
      });
    }
  }

  Future<void> _resetPassword() async {
    if (!_resetFormKey.currentState!.validate()) return;

    final parsed = _parseLink(_linkOrToken.text);
    final id = parsed?.$1 ?? _userId.text.trim();
    final ts = parsed?.$2 ?? int.tryParse(_resetTimestamp.text.trim()) ?? 0;
    final code = parsed?.$3 ?? _linkOrToken.text.trim();

    if (id.isEmpty || ts == 0 || code.isEmpty) {
      setState(() {
        _errorMessage = 'Vui lòng dán liên kết đầy đủ từ email hoặc nhập đầy đủ ID và Mã token.';
      });
      return;
    }

    setState(() {
      _submitting = true;
      _errorMessage = null;
    });

    final vm = context.read<AccountViewModel>();
    final success = await vm.resetPassword(
      id: id,
      resetTimestamp: ts,
      resetCode: code,
      newPassword: _newPassword.text,
      confirmPassword: _confirmNewPassword.text,
    );

    if (!mounted) return;
    setState(() => _submitting = false);

    if (success) {
      Navigator.of(context).pop();
      showCinemaMessage(
        context,
        'Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới.',
      );
    } else {
      setState(() {
        _errorMessage = vm.error ?? 'Đặt lại mật khẩu thất bại. Mã có thể đã hết hạn hoặc không hợp lệ.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    return Padding(
      padding: EdgeInsets.fromLTRB(24, 16, 24, bottomInset + 24),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.white24,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Container(
                  width: 42,
                  height: 42,
                  decoration: BoxDecoration(
                    color: CinemaColors.red.withValues(alpha: 0.12),
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    _isResetStep
                        ? Icons.lock_open_rounded
                        : Icons.mark_email_read_outlined,
                    color: CinemaColors.red,
                    size: 22,
                  ),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        _isResetStep ? 'Đặt lại mật khẩu' : 'Quên mật khẩu',
                        style: Theme.of(context).textTheme.titleMedium
                            ?.copyWith(
                              fontWeight: FontWeight.bold,
                              fontSize: 18,
                            ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        _isResetStep
                            ? 'Bước 2: Xác nhận mã và mật khẩu mới'
                            : 'Bước 1: Nhận mã xác thực qua email',
                        style: const TextStyle(
                          color: CinemaColors.muted,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: const Icon(
                    Icons.close,
                    size: 20,
                    color: CinemaColors.muted,
                  ),
                  onPressed: () => Navigator.of(context).pop(),
                  tooltip: 'Đóng',
                ),
              ],
            ),
            const SizedBox(height: 12),
            const Divider(color: Colors.white10),
            const SizedBox(height: 8),
            Text(
              _isResetStep
                  ? 'Dán liên kết nhận được trong email để tự nhận diện mã, sau đó nhập mật khẩu mới.'
                  : 'Nhập email tài khoản của bạn. Hệ thống sẽ gửi thư xác nhận kèm liên kết khôi phục mật khẩu trong 5 phút.',
              style: const TextStyle(
                color: CinemaColors.muted,
                fontSize: 13,
                height: 1.4,
              ),
            ),
            if (_errorMessage != null) ...[
              const SizedBox(height: 14),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 10,
                ),
                decoration: BoxDecoration(
                  color: CinemaColors.red.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(
                    color: CinemaColors.red.withValues(alpha: 0.35),
                  ),
                ),
                child: Row(
                  children: [
                    const Icon(
                      Icons.error_outline,
                      color: CinemaColors.red,
                      size: 18,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        _errorMessage!,
                        style: const TextStyle(
                          color: CinemaColors.red,
                          fontSize: 12.5,
                          height: 1.3,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            const SizedBox(height: 16),
            if (!_isResetStep) _buildEmailStep() else _buildResetStep(),
          ],
        ),
      ),
    );
  }

  Widget _buildEmailStep() {
    return Form(
      key: _emailFormKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextFormField(
            controller: _email,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(
              labelText: 'Địa chỉ Email',
              hintText: 'example@gmail.com',
              prefixIcon: Icon(Icons.email_outlined),
              border: OutlineInputBorder(),
            ),
            validator: validateEmail,
          ),
          const SizedBox(height: 20),
          SizedBox(
            height: 48,
            child: FilledButton(
              style: FilledButton.styleFrom(backgroundColor: CinemaColors.red),
              onPressed: _submitting ? null : _sendEmail,
              child: _submitting
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'Gửi mã xác nhận',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
            ),
          ),
          const SizedBox(height: 12),
          TextButton.icon(
            onPressed: () => setState(() {
              _isResetStep = true;
              _errorMessage = null;
            }),
            icon: const Icon(Icons.vpn_key_outlined, size: 16),
            label: const Text('Đã có mã xác nhận? Đổi mật khẩu ngay'),
          ),
        ],
      ),
    );
  }

  Widget _buildResetStep() {
    final hasParsedId = _userId.text.isNotEmpty;

    return Form(
      key: _resetFormKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextFormField(
            controller: _linkOrToken,
            decoration: InputDecoration(
              labelText: 'Liên kết hoặc Token từ email',
              hintText: 'Dán liên kết từ email nhận được...',
              prefixIcon: const Icon(Icons.link_rounded),
              border: const OutlineInputBorder(),
              suffixIcon: _linkOrToken.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear, size: 18),
                      onPressed: () {
                        setState(() {
                          _linkOrToken.clear();
                          _userId.clear();
                          _resetTimestamp.clear();
                        });
                      },
                    )
                  : null,
            ),
            onChanged: _onLinkChanged,
            validator: (v) => v == null || v.trim().isEmpty
                ? 'Vui lòng nhập liên kết hoặc mã token.'
                : null,
          ),
          if (hasParsedId) ...[
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
              decoration: BoxDecoration(
                color: Colors.green.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.green.withValues(alpha: 0.35)),
              ),
              child: Row(
                children: [
                  const Icon(
                    Icons.check_circle_outline,
                    color: Colors.greenAccent,
                    size: 16,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Đã nhận diện: ID ${_userId.text}',
                      style: const TextStyle(
                        fontSize: 12,
                        color: Colors.greenAccent,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
          const SizedBox(height: 12),
          TextFormField(
            controller: _newPassword,
            obscureText: _obscureNewPassword,
            decoration: InputDecoration(
              labelText: 'Mật khẩu mới',
              prefixIcon: const Icon(Icons.lock_outline_rounded),
              border: const OutlineInputBorder(),
              suffixIcon: IconButton(
                icon: Icon(
                  _obscureNewPassword
                      ? Icons.visibility_off_outlined
                      : Icons.visibility_outlined,
                  size: 20,
                ),
                onPressed: () =>
                    setState(() => _obscureNewPassword = !_obscureNewPassword),
              ),
            ),
            validator: validatePassword,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _confirmNewPassword,
            obscureText: _obscureConfirmPassword,
            decoration: InputDecoration(
              labelText: 'Xác nhận mật khẩu mới',
              prefixIcon: const Icon(Icons.lock_reset_rounded),
              border: const OutlineInputBorder(),
              suffixIcon: IconButton(
                icon: Icon(
                  _obscureConfirmPassword
                      ? Icons.visibility_off_outlined
                      : Icons.visibility_outlined,
                  size: 20,
                ),
                onPressed: () => setState(
                  () => _obscureConfirmPassword = !_obscureConfirmPassword,
                ),
              ),
            ),
            validator: (v) =>
                v != _newPassword.text ? 'Mật khẩu xác nhận không khớp.' : null,
          ),
          const SizedBox(height: 20),
          SizedBox(
            height: 48,
            child: FilledButton(
              style: FilledButton.styleFrom(backgroundColor: CinemaColors.red),
              onPressed: _submitting ? null : _resetPassword,
              child: _submitting
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'Xác nhận đổi mật khẩu',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
            ),
          ),
          const SizedBox(height: 12),
          TextButton.icon(
            onPressed: () => setState(() {
              _isResetStep = false;
              _errorMessage = null;
            }),
            icon: const Icon(Icons.arrow_back, size: 16),
            label: const Text('Quay lại gửi email'),
          ),
        ],
      ),
    );
  }
}
