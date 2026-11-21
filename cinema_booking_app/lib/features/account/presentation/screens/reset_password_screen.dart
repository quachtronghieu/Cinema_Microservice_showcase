import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../../../core/ui/cinema_widgets.dart';
import '../../../../core/ui/validators.dart';
import '../view_models/account_view_model.dart';

class ResetPasswordScreen extends StatefulWidget {
  const ResetPasswordScreen({
    super.key,
    required this.id,
    required this.resetTimestamp,
    required this.resetCode,
  });

  final String id;
  final int resetTimestamp;
  final String resetCode;

  @override
  State<ResetPasswordScreen> createState() => _ResetPasswordScreenState();
}

class _ResetPasswordScreenState extends State<ResetPasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _newPassword = TextEditingController();
  final _confirmPassword = TextEditingController();

  bool _submitting = false;
  bool _obscureNew = true;
  bool _obscureConfirm = true;
  String? _errorMessage;

  @override
  void dispose() {
    _newPassword.dispose();
    _confirmPassword.dispose();
    super.dispose();
  }

  bool get _isLinkMalformed =>
      widget.id.trim().isEmpty ||
      widget.resetTimestamp <= 0 ||
      widget.resetCode.trim().isEmpty;

  bool get _isLinkExpired {
    if (_isLinkMalformed) return false;
    // Backend expiration window is 10 minutes (600,000 ms)
    final now = DateTime.now().millisecondsSinceEpoch;
    final diff = now - widget.resetTimestamp;
    return diff > 10 * 60 * 1000;
  }

  void _navigateToLogin(BuildContext context) {
    if (GoRouter.maybeOf(context) != null) {
      context.go('/login');
    } else {
      Navigator.of(context).maybePop();
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() {
      _submitting = true;
      _errorMessage = null;
    });

    final vm = context.read<AccountViewModel>();
    final success = await vm.resetPassword(
      id: widget.id,
      resetTimestamp: widget.resetTimestamp,
      resetCode: widget.resetCode,
      newPassword: _newPassword.text,
      confirmPassword: _confirmPassword.text,
    );

    if (!mounted) return;
    setState(() => _submitting = false);

    if (success) {
      showCinemaMessage(
        context,
        'Đặt lại mật khẩu thành công! Vui lòng đăng nhập với mật khẩu mới.',
      );
      if (mounted) {
        _navigateToLogin(context);
      }
    } else {
      setState(() {
        _errorMessage = vm.error ?? 'Mã xác thực không còn hiệu lực hoặc đã được sử dụng. Vui lòng yêu cầu lại.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLinkMalformed || _isLinkExpired) {
      return Scaffold(
        appBar: AppBar(title: const Text('Đặt lại mật khẩu')),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(
                  Icons.timer_off_outlined,
                  color: CinemaColors.red,
                  size: 64,
                ),
                const SizedBox(height: 20),
                Text(
                  _isLinkMalformed
                      ? 'Liên kết không hợp lệ'
                      : 'Liên kết đã hết hạn',
                  style: Theme.of(context).textTheme.titleLarge
                      ?.copyWith(fontWeight: FontWeight.bold),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 12),
                Text(
                  _isLinkMalformed
                      ? 'Thông tin trong liên kết xác thực bị thiếu hoặc sai định dạng.'
                      : 'Liên kết xác thực chỉ có hiệu lực trong 10 phút. Vui lòng gửi lại yêu cầu quên mật khẩu.',
                  style: const TextStyle(
                    color: CinemaColors.muted,
                    fontSize: 14,
                    height: 1.4,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 28),
                SizedBox(
                  width: double.infinity,
                  height: 48,
                  child: FilledButton(
                    onPressed: () => _navigateToLogin(context),
                    child: const Text('Quay lại Đăng nhập'),
                  ),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(title: const Text('Đặt lại mật khẩu')),
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Center(
                      child: Container(
                        width: 64,
                        height: 64,
                        decoration: BoxDecoration(
                          color: CinemaColors.red.withValues(alpha: 0.12),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.lock_reset_rounded,
                          color: CinemaColors.red,
                          size: 32,
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'Thiết lập mật khẩu mới',
                      style: Theme.of(context).textTheme.headlineSmall
                          ?.copyWith(fontWeight: FontWeight.bold),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Liên kết từ email đã được xác minh thành công. Hãy nhập mật khẩu mới cho tài khoản.',
                      style: TextStyle(
                        color: CinemaColors.muted,
                        fontSize: 13,
                        height: 1.4,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 16),

                    // Identification chip
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 10,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.green.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: Colors.green.withValues(alpha: 0.35),
                        ),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.verified_outlined,
                            color: Colors.greenAccent,
                            size: 20,
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              'Đã nhận diện tài khoản ID: ${widget.id}',
                              style: const TextStyle(
                                color: Colors.greenAccent,
                                fontSize: 13,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),

                    if (_errorMessage != null) ...[
                      const SizedBox(height: 16),
                      Container(
                        padding: const EdgeInsets.all(12),
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
                              size: 20,
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Text(
                                _errorMessage!,
                                style: const TextStyle(
                                  color: CinemaColors.red,
                                  fontSize: 13,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],

                    const SizedBox(height: 20),
                    TextFormField(
                      controller: _newPassword,
                      obscureText: _obscureNew,
                      decoration: InputDecoration(
                        labelText: 'Mật khẩu mới',
                        prefixIcon: const Icon(Icons.lock_outline_rounded),
                        suffixIcon: IconButton(
                          icon: Icon(
                            _obscureNew
                                ? Icons.visibility_off_outlined
                                : Icons.visibility_outlined,
                          ),
                          onPressed: () =>
                              setState(() => _obscureNew = !_obscureNew),
                        ),
                      ),
                      validator: validatePassword,
                    ),
                    const SizedBox(height: 16),
                    TextFormField(
                      controller: _confirmPassword,
                      obscureText: _obscureConfirm,
                      decoration: InputDecoration(
                        labelText: 'Xác nhận mật khẩu mới',
                        prefixIcon: const Icon(Icons.lock_reset_rounded),
                        suffixIcon: IconButton(
                          icon: Icon(
                            _obscureConfirm
                                ? Icons.visibility_off_outlined
                                : Icons.visibility_outlined,
                          ),
                          onPressed: () => setState(
                            () => _obscureConfirm = !_obscureConfirm,
                          ),
                        ),
                      ),
                      validator: (v) => v != _newPassword.text
                          ? 'Mật khẩu xác nhận không khớp.'
                          : null,
                    ),
                    const SizedBox(height: 28),
                    SizedBox(
                      height: 50,
                      child: FilledButton(
                        onPressed: _submitting ? null : _submit,
                        child: _submitting
                            ? const SizedBox(
                                width: 22,
                                height: 22,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : const Text(
                                'Xác nhận đổi mật khẩu',
                                style: TextStyle(
                                  fontSize: 15,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextButton(
                      onPressed: () => _navigateToLogin(context),
                      child: const Text('Hủy và quay lại Đăng nhập'),
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
