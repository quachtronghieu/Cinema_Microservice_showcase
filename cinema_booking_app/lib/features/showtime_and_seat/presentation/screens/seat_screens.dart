// import 'dart:async';

// import 'package:flutter/material.dart';
// import 'package:go_router/go_router.dart';
// import 'package:provider/provider.dart';

// class ShowtimeSelectionScreen extends StatefulWidget {
//   const ShowtimeSelectionScreen({super.key});
//   @override
//   State<ShowtimeSelectionScreen> createState() =>
//       _ShowtimeSelectionScreenState();
// }

// class _ShowtimeSelectionScreenState extends State<ShowtimeSelectionScreen> {
//   static const _dates = [
//     _ShowtimeDate(
//         weekday: 'T4', day: '30', month: 'th9', fullDate: '30/09/2026'),
//     _ShowtimeDate(
//         weekday: 'T5', day: '01', month: 'th10', fullDate: '01/10/2026'),
//     _ShowtimeDate(
//         weekday: 'T6', day: '02', month: 'th10', fullDate: '02/10/2026'),
//   ];

//   int _selectedDate = 0;

//   @override
//   void initState() {
//     super.initState();
//     WidgetsBinding.instance.addPostFrameCallback((_) {
//       if (mounted) {
//         final movie = context.read<CustomerFlow>().movie;
//         if (movie != null) context.read<SeatViewModel>().load(movie.id);
//       }
//     });
//   }

//   @override
//   Widget build(BuildContext context) {
//     final movie = context.watch<CustomerFlow>().movie;
//     final vm = context.watch<SeatViewModel>();
//     return Scaffold(
//       appBar: AppBar(
//         title: const Text(
//           'CHỌN SUẤT CHIẾU',
//           style: TextStyle(fontSize: 17, fontWeight: FontWeight.w900),
//         ),
//         bottom: const PreferredSize(
//           preferredSize: Size.fromHeight(1),
//           child: Divider(height: 1, color: Color(0xFF332929)),
//         ),
//       ),
//       body: movie == null
//           ? const AsyncStateView(message: 'Hãy chọn một bộ phim trước.')
//           : Center(
//               child: ConstrainedBox(
//                 constraints: const BoxConstraints(maxWidth: 390),
//                 child: Column(
//                   children: [
//                     _ShowtimeMovieHeader(movie: movie),
//                     Expanded(
//                       child: vm.busy
//                           ? const Center(child: CircularProgressIndicator())
//                           : vm.error != null
//                               ? AsyncStateView(
//                                   message: vm.error!,
//                                   retry: () => vm.load(movie.id),
//                                 )
//                               : vm.showtimes.isEmpty
//                                   ? const AsyncStateView(
//                                       message: 'Chưa có suất chiếu.',
//                                     )
//                                   : ListView(
//                                       padding: const EdgeInsets.fromLTRB(
//                                         10,
//                                         14,
//                                         10,
//                                         24,
//                                       ),
//                                       children: [
//                                         Row(
//                                           children: List.generate(
//                                             _dates.length,
//                                             (index) => Padding(
//                                               padding: const EdgeInsets.only(
//                                                 right: 8,
//                                               ),
//                                               child: _ShowtimeDateCard(
//                                                 date: _dates[index],
//                                                 selected:
//                                                     _selectedDate == index,
//                                                 onTap: () => setState(
//                                                   () => _selectedDate = index,
//                                                 ),
//                                               ),
//                                             ),
//                                           ),
//                                         ),
//                                         const SizedBox(height: 12),
//                                         Row(
//                                           children: [
//                                             const Icon(
//                                               Icons.calendar_month_outlined,
//                                               size: 15,
//                                               color: CinemaColors.muted,
//                                             ),
//                                             const SizedBox(width: 5),
//                                             Text(
//                                               _dates[_selectedDate].fullDate,
//                                               style: const TextStyle(
//                                                 fontWeight: FontWeight.w700,
//                                               ),
//                                             ),
//                                           ],
//                                         ),
//                                         const SizedBox(height: 12),
//                                         Row(
//                                           children: List.generate(
//                                             vm.showtimes.length,
//                                             (index) => Expanded(
//                                               child: Padding(
//                                                 padding: EdgeInsets.only(
//                                                   right: index ==
//                                                           vm.showtimes.length -
//                                                               1
//                                                       ? 0
//                                                       : 8,
//                                                 ),
//                                                 child: _ShowtimeTimeCard(
//                                                   showtime: vm.showtimes[index],
//                                                   availableSeats: [
//                                                     27,
//                                                     30,
//                                                     31
//                                                   ][index % 3],
//                                                   onTap: () {
//                                                     context
//                                                         .read<CustomerFlow>()
//                                                         .chooseShowtime(
//                                                           vm.showtimes[index],
//                                                         );
//                                                     vm.reset();
//                                                     context.push('/seats');
//                                                   },
//                                                 ),
//                                               ),
//                                             ),
//                                           ),
//                                         ),
//                                         const SizedBox(height: 13),
//                                         const Text(
//                                           'Số ghế trống tính từ trạng thái ghế của từng suất chiếu.',
//                                           style: TextStyle(
//                                             color: CinemaColors.muted,
//                                             fontSize: 10,
//                                           ),
//                                         ),
//                                       ],
//                                     ),
//                     ),
//                   ],
//                 ),
//               ),
//             ),
//     );
//   }
// }

// class _ShowtimeDate {
//   const _ShowtimeDate({
//     required this.weekday,
//     required this.day,
//     required this.month,
//     required this.fullDate,
//   });

//   final String weekday;
//   final String day;
//   final String month;
//   final String fullDate;
// }

// class _ShowtimeMovieHeader extends StatelessWidget {
//   const _ShowtimeMovieHeader({required this.movie});

//   final CinemaMovie movie;

//   @override
//   Widget build(BuildContext context) => DecoratedBox(
//         decoration: const BoxDecoration(
//           border: Border(bottom: BorderSide(color: Color(0xFF332929))),
//         ),
//         child: Padding(
//           padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 12),
//           child: Row(
//             children: [
//               CinemaPoster(movie.poster, width: 32, height: 48),
//               const SizedBox(width: 10),
//               Expanded(
//                 child: Column(
//                   crossAxisAlignment: CrossAxisAlignment.start,
//                   children: [
//                     Text(
//                       movie.title,
//                       style: const TextStyle(
//                         fontSize: 14,
//                         fontWeight: FontWeight.w800,
//                       ),
//                     ),
//                     Text(
//                       '${movie.duration} phút · ${movie.rating}',
//                       style: const TextStyle(
//                         color: CinemaColors.muted,
//                         fontSize: 11,
//                       ),
//                     ),
//                   ],
//                 ),
//               ),
//               Container(
//                 padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
//                 decoration: BoxDecoration(
//                   border: Border.all(color: CinemaColors.warning),
//                   borderRadius: BorderRadius.circular(20),
//                 ),
//                 child: const Row(
//                   mainAxisSize: MainAxisSize.min,
//                   children: [
//                     Icon(
//                       Icons.person_outline,
//                       size: 10,
//                       color: CinemaColors.warning,
//                     ),
//                     SizedBox(width: 3),
//                     Text(
//                       'DEMO',
//                       style: TextStyle(
//                         color: CinemaColors.warning,
//                         fontSize: 9,
//                         fontWeight: FontWeight.w800,
//                       ),
//                     ),
//                   ],
//                 ),
//               ),
//             ],
//           ),
//         ),
//       );
// }

// class _ShowtimeDateCard extends StatelessWidget {
//   const _ShowtimeDateCard({
//     required this.date,
//     required this.selected,
//     required this.onTap,
//   });

//   final _ShowtimeDate date;
//   final bool selected;
//   final VoidCallback onTap;

//   @override
//   Widget build(BuildContext context) => Material(
//         color: selected ? CinemaColors.red : CinemaColors.surface,
//         borderRadius: BorderRadius.circular(13),
//         child: InkWell(
//           onTap: onTap,
//           borderRadius: BorderRadius.circular(13),
//           child: Container(
//             width: 46,
//             padding: const EdgeInsets.symmetric(vertical: 7),
//             decoration: BoxDecoration(
//               borderRadius: BorderRadius.circular(13),
//               border: Border.all(
//                 color: selected ? CinemaColors.red : const Color(0xFF4A3D3D),
//               ),
//             ),
//             child: Column(
//               children: [
//                 Text(date.weekday, style: const TextStyle(fontSize: 10)),
//                 Text(
//                   date.day,
//                   style: const TextStyle(
//                     fontSize: 18,
//                     fontWeight: FontWeight.w900,
//                   ),
//                 ),
//                 Text(date.month, style: const TextStyle(fontSize: 9)),
//               ],
//             ),
//           ),
//         ),
//       );
// }

// class _ShowtimeTimeCard extends StatelessWidget {
//   const _ShowtimeTimeCard({
//     required this.showtime,
//     required this.availableSeats,
//     required this.onTap,
//   });

//   final CinemaShowtime showtime;
//   final int availableSeats;
//   final VoidCallback onTap;

//   @override
//   Widget build(BuildContext context) => Material(
//         color: CinemaColors.surface,
//         borderRadius: BorderRadius.circular(13),
//         child: InkWell(
//           onTap: onTap,
//           borderRadius: BorderRadius.circular(13),
//           child: Container(
//             padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 11),
//             decoration: BoxDecoration(
//               borderRadius: BorderRadius.circular(13),
//               border: Border.all(color: const Color(0xFF4A3D3D)),
//             ),
//             child: Column(
//               children: [
//                 Text(
//                   showtime.time,
//                   style: const TextStyle(
//                     fontSize: 15,
//                     fontWeight: FontWeight.w900,
//                   ),
//                 ),
//                 Text(
//                   showtime.room,
//                   style: const TextStyle(
//                     color: CinemaColors.muted,
//                     fontSize: 9,
//                   ),
//                 ),
//                 const SizedBox(height: 2),
//                 Text(
//                   '$availableSeats ghế trống',
//                   style: const TextStyle(
//                     color: CinemaColors.success,
//                     fontSize: 9,
//                   ),
//                 ),
//               ],
//             ),
//           ),
//         ),
//       );
// }

// class SeatSelectionScreen extends StatefulWidget {
//   const SeatSelectionScreen({super.key});
//   @override
//   State<SeatSelectionScreen> createState() => _SeatSelectionScreenState();
// }

// class _SeatSelectionScreenState extends State<SeatSelectionScreen> {
//   Timer? _timer;
//   @override
//   void initState() {
//     super.initState();
//     _timer = Timer.periodic(const Duration(seconds: 1), (_) {
//       if (mounted) setState(() {});
//     });
//   }

//   @override
//   void dispose() {
//     _timer?.cancel();
//     super.dispose();
//   }

//   @override
//   Widget build(BuildContext context) {
//     final flow = context.watch<CustomerFlow>();
//     final vm = context.watch<SeatViewModel>();
//     final remaining = vm.holdUntil?.difference(DateTime.now()) ?? Duration.zero;
//     final holdExpired = vm.holdUntil != null && remaining <= Duration.zero;
//     final selected = vm.chosen.toList()..sort();
//     final price = selected.fold<int>(
//         0,
//         (sum, seat) =>
//             sum +
//             (seat.startsWith('H')
//                 ? 180000
//                 : 'DEF'.contains(seat[0])
//                     ? 95000
//                     : 75000));

//     return Scaffold(
//       appBar: AppBar(title: const Text('Chọn ghế')),
//       body: flow.showtime == null
//           ? const AsyncStateView(message: 'Hãy chọn suất chiếu trước.')
//           : ListView(padding: const EdgeInsets.all(20), children: [
//               Text('${flow.showtime!.cinema} · ${flow.showtime!.time}',
//                   style: Theme.of(context).textTheme.titleMedium),
//               if (vm.holdUntil != null)
//                 Padding(
//                     padding: const EdgeInsets.symmetric(vertical: 12),
//                     child: Text(
//                         holdExpired
//                             ? 'Giữ ghế demo đã hết hạn.'
//                             : 'Giữ ghế demo: ${remaining.inMinutes}:${(remaining.inSeconds % 60).toString().padLeft(2, '0')}',
//                         style: const TextStyle(color: CinemaColors.warning))),
//               const SizedBox(height: 24),
//               Container(
//                   padding: const EdgeInsets.all(12),
//                   color: CinemaColors.surface,
//                   child: const Center(child: Text('MÀN HÌNH'))),
//               const SizedBox(height: 20),
//               ...'ABCDEFGH'.split('').map((row) => Padding(
//                   padding: const EdgeInsets.only(bottom: 7),
//                   child: Row(children: [
//                     SizedBox(width: 20, child: Text(row)),
//                     Expanded(
//                         child: Row(
//                             children: List.generate(10, (index) {
//                       final id = '$row${index + 1}';
//                       final taken = {'A3', 'A4', 'C5', 'C6', 'E7', 'F4', 'F5'}
//                           .contains(id);
//                       return Expanded(
//                           child: Padding(
//                               padding:
//                                   const EdgeInsets.symmetric(horizontal: 2),
//                               child: InkWell(
//                                 onTap: taken ? null : () => vm.toggle(id),
//                                 child: AspectRatio(
//                                     aspectRatio: 1,
//                                     child: DecoratedBox(
//                                         decoration: BoxDecoration(
//                                           color: taken
//                                               ? CinemaColors.muted
//                                                   .withValues(alpha: 0.25)
//                                               : vm.chosen.contains(id)
//                                                   ? CinemaColors.red
//                                                   : CinemaColors.surface,
//                                           borderRadius:
//                                               BorderRadius.circular(5),
//                                         ),
//                                         child: Center(
//                                             child: Text('${index + 1}',
//                                                 style: const TextStyle(
//                                                     fontSize: 9))))),
//                               )));
//                     }))),
//                   ]))),
//               const SizedBox(height: 12),
//               const Text(
//                   'Ghế thường 75.000 ₫ · VIP 95.000 ₫ · Hàng H 180.000 ₫',
//                   style: TextStyle(color: CinemaColors.muted, fontSize: 12)),
//             ]),
//       bottomNavigationBar: SafeArea(
//           child: Padding(
//               padding: const EdgeInsets.all(16),
//               child: Column(mainAxisSize: MainAxisSize.min, children: [
//                 PriceLine('Ghế ${selected.join(', ')}', price, total: true),
//                 const SizedBox(height: 10),
//                 ElevatedButton(
//                     onPressed:
//                         selected.isEmpty || holdExpired || flow.showtime == null
//                             ? null
//                             : () {
//                                 vm.holdDemo();
//                                 flow.chooseSeats(selected);
//                                 context
//                                     .read<ConcessionsViewModel>()
//                                     .setTickets(price);
//                                 context.push('/concessions');
//                               },
//                     child: const Text('Tiếp tục chọn bắp nước')),
//                 const Text(
//                     'Dữ liệu ghế và thời gian giữ ghế chỉ để trình diễn.',
//                     style: TextStyle(fontSize: 11, color: CinemaColors.muted)),
//               ]))),
//     );
//   }
// }
