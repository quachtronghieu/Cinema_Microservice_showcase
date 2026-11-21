-- Current checkout uses cinema_db. Change USE only if CINEMA_DB_URL points elsewhere.
USE [cinema_db];
-- LOCAL/DEV ONLY. Run against the named database after its schema exists.
-- Insert only: existing rows are not updated or deleted. Stop application writes while seeding.
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;

BEGIN TRAN;
IF NOT EXISTS (SELECT 1 FROM dbo.[Template] WHERE [template_id]='TM000001')
INSERT [dbo].[Template] ([template_id], [name], [description]) VALUES (N'TM000001', N'2D', N'');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template] WHERE [template_id]='TM000002')
INSERT [dbo].[Template] ([template_id], [name], [description]) VALUES (N'TM000002', N'3D', N'');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template] WHERE [template_id]='TM000003')
INSERT [dbo].[Template] ([template_id], [name], [description]) VALUES (N'TM000003', N'4D', N'');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000001')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000001', N'TM000001', N'A', 1, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000002')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000002', N'TM000001', N'A', 2, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000003')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000003', N'TM000001', N'A', 3, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000004')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000004', N'TM000001', N'A', 4, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000005')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000005', N'TM000001', N'A', 5, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000006')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000006', N'TM000001', N'A', 6, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000007')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000007', N'TM000001', N'A', 7, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000008')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000008', N'TM000001', N'B', 1, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000009')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000009', N'TM000001', N'B', 2, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000010')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000010', N'TM000001', N'B', 3, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000011')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000011', N'TM000001', N'B', 4, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000012')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000012', N'TM000001', N'B', 5, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000013')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000013', N'TM000001', N'B', 6, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000014')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000014', N'TM000001', N'B', 7, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000015')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000015', N'TM000001', N'C', 1, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000016')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000016', N'TM000001', N'C', 2, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000017')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000017', N'TM000001', N'C', 3, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000018')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000018', N'TM000001', N'C', 4, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000019')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000019', N'TM000001', N'C', 5, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000020')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000020', N'TM000001', N'C', 6, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000021')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000021', N'TM000001', N'C', 7, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000022')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000022', N'TM000001', N'D', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000023')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000023', N'TM000001', N'D', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000024')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000024', N'TM000001', N'D', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000025')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000025', N'TM000001', N'D', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000026')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000026', N'TM000001', N'D', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000027')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000027', N'TM000001', N'D', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000028')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000028', N'TM000001', N'D', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000029')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000029', N'TM000001', N'D', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000030')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000030', N'TM000001', N'D', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000031')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000031', N'TM000001', N'E', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000032')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000032', N'TM000001', N'E', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000033')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000033', N'TM000001', N'E', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000034')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000034', N'TM000001', N'E', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000035')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000035', N'TM000001', N'E', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000036')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000036', N'TM000001', N'E', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000037')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000037', N'TM000001', N'E', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000038')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000038', N'TM000001', N'E', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000039')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000039', N'TM000001', N'E', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000040')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000040', N'TM000001', N'F', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000041')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000041', N'TM000001', N'F', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000042')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000042', N'TM000001', N'F', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000043')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000043', N'TM000001', N'F', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000044')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000044', N'TM000001', N'F', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000045')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000045', N'TM000001', N'F', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000046')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000046', N'TM000001', N'F', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000047')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000047', N'TM000001', N'F', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000048')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000048', N'TM000001', N'F', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000049')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000049', N'TM000001', N'G', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000050')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000050', N'TM000001', N'G', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000051')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000051', N'TM000001', N'G', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000052')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000052', N'TM000001', N'G', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000053')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000053', N'TM000001', N'G', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000054')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000054', N'TM000001', N'G', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000055')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000055', N'TM000001', N'G', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000056')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000056', N'TM000001', N'G', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000057')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000057', N'TM000001', N'G', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000058')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000058', N'TM000001', N'H', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000059')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000059', N'TM000001', N'H', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000060')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000060', N'TM000001', N'H', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000061')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000061', N'TM000001', N'H', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000062')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000062', N'TM000001', N'H', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000063')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000063', N'TM000001', N'H', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000064')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000064', N'TM000001', N'H', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000065')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000065', N'TM000001', N'H', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000066')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000066', N'TM000001', N'H', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000067')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000067', N'TM000001', N'I', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000068')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000068', N'TM000001', N'I', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000069')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000069', N'TM000001', N'I', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000070')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000070', N'TM000001', N'I', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000071')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000071', N'TM000001', N'I', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000072')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000072', N'TM000001', N'I', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000073')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000073', N'TM000001', N'I', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000074')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000074', N'TM000001', N'I', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000075')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000075', N'TM000001', N'I', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000076')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000076', N'TM000002', N'A', 1, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000077')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000077', N'TM000002', N'A', 2, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000078')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000078', N'TM000002', N'A', 3, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000079')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000079', N'TM000002', N'A', 4, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000080')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000080', N'TM000002', N'A', 5, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000081')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000081', N'TM000002', N'A', 6, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000082')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000082', N'TM000002', N'A', 7, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000083')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000083', N'TM000002', N'A', 8, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000084')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000084', N'TM000002', N'A', 9, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000085')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000085', N'TM000002', N'A', 10, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000086')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000086', N'TM000002', N'B', 1, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000087')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000087', N'TM000002', N'B', 2, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000088')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000088', N'TM000002', N'B', 3, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000089')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000089', N'TM000002', N'B', 4, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000090')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000090', N'TM000002', N'B', 5, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000091')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000091', N'TM000002', N'B', 6, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000092')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000092', N'TM000002', N'B', 7, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000093')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000093', N'TM000002', N'B', 8, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000094')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000094', N'TM000002', N'B', 9, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000095')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000095', N'TM000002', N'B', 10, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000096')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000096', N'TM000002', N'C', 1, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000097')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000097', N'TM000002', N'C', 2, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000098')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000098', N'TM000002', N'C', 3, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000099')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000099', N'TM000002', N'C', 4, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000100')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000100', N'TM000002', N'C', 5, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000101')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000101', N'TM000002', N'C', 6, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000102')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000102', N'TM000002', N'C', 7, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000103')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000103', N'TM000002', N'C', 8, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000104')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000104', N'TM000002', N'C', 9, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000105')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000105', N'TM000002', N'C', 10, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000106')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000106', N'TM000002', N'D', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000107')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000107', N'TM000002', N'D', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000108')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000108', N'TM000002', N'D', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000109')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000109', N'TM000002', N'D', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000110')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000110', N'TM000002', N'D', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000111')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000111', N'TM000002', N'D', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000112')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000112', N'TM000002', N'D', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000113')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000113', N'TM000002', N'D', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000114')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000114', N'TM000002', N'D', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000115')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000115', N'TM000002', N'D', 10, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000116')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000116', N'TM000002', N'E', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000117')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000117', N'TM000002', N'E', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000118')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000118', N'TM000002', N'E', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000119')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000119', N'TM000002', N'E', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000120')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000120', N'TM000002', N'E', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000121')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000121', N'TM000002', N'E', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000122')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000122', N'TM000002', N'E', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000123')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000123', N'TM000002', N'E', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000124')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000124', N'TM000002', N'E', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000125')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000125', N'TM000002', N'E', 10, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000126')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000126', N'TM000002', N'F', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000127')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000127', N'TM000002', N'F', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000128')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000128', N'TM000002', N'F', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000129')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000129', N'TM000002', N'F', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000130')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000130', N'TM000002', N'F', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000131')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000131', N'TM000002', N'F', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000132')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000132', N'TM000002', N'F', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000133')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000133', N'TM000002', N'F', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000134')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000134', N'TM000002', N'F', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000135')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000135', N'TM000002', N'F', 10, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000136')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000136', N'TM000002', N'G', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000137')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000137', N'TM000002', N'G', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000138')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000138', N'TM000002', N'G', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000139')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000139', N'TM000002', N'G', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000140')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000140', N'TM000002', N'G', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000141')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000141', N'TM000002', N'G', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000142')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000142', N'TM000002', N'G', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000143')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000143', N'TM000002', N'G', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000144')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000144', N'TM000002', N'G', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000145')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000145', N'TM000002', N'G', 10, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000146')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000146', N'TM000002', N'H', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000147')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000147', N'TM000002', N'H', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000148')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000148', N'TM000002', N'H', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000149')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000149', N'TM000002', N'H', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000150')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000150', N'TM000002', N'H', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000151')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000151', N'TM000002', N'H', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000152')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000152', N'TM000002', N'H', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000153')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000153', N'TM000002', N'H', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000154')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000154', N'TM000002', N'H', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000155')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000155', N'TM000002', N'H', 10, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000156')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000156', N'TM000003', N'A', 1, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000157')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000157', N'TM000003', N'A', 2, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000158')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000158', N'TM000003', N'A', 3, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000159')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000159', N'TM000003', N'A', 4, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000160')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000160', N'TM000003', N'A', 5, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000161')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000161', N'TM000003', N'A', 6, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000162')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000162', N'TM000003', N'A', 7, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000163')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000163', N'TM000003', N'A', 8, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000164')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000164', N'TM000003', N'A', 9, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000165')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000165', N'TM000003', N'A', 10, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000166')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000166', N'TM000003', N'B', 1, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000167')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000167', N'TM000003', N'B', 2, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000168')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000168', N'TM000003', N'B', 3, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000169')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000169', N'TM000003', N'B', 4, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000170')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000170', N'TM000003', N'B', 5, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000171')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000171', N'TM000003', N'B', 6, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000172')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000172', N'TM000003', N'B', 7, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000173')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000173', N'TM000003', N'B', 8, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000174')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000174', N'TM000003', N'B', 9, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000175')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000175', N'TM000003', N'B', 10, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000176')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000176', N'TM000003', N'C', 1, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000177')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000177', N'TM000003', N'C', 2, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000178')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000178', N'TM000003', N'C', 3, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000179')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000179', N'TM000003', N'C', 4, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000180')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000180', N'TM000003', N'C', 5, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000181')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000181', N'TM000003', N'C', 6, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000182')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000182', N'TM000003', N'C', 7, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000183')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000183', N'TM000003', N'C', 8, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000184')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000184', N'TM000003', N'C', 9, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000185')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000185', N'TM000003', N'C', 10, N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000186')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000186', N'TM000003', N'D', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000187')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000187', N'TM000003', N'D', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000188')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000188', N'TM000003', N'D', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000189')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000189', N'TM000003', N'D', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000190')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000190', N'TM000003', N'D', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000191')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000191', N'TM000003', N'D', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000192')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000192', N'TM000003', N'D', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000193')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000193', N'TM000003', N'D', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000194')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000194', N'TM000003', N'D', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000195')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000195', N'TM000003', N'D', 10, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000196')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000196', N'TM000003', N'E', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000197')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000197', N'TM000003', N'E', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000198')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000198', N'TM000003', N'E', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000199')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000199', N'TM000003', N'E', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000200')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000200', N'TM000003', N'E', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000201')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000201', N'TM000003', N'E', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000202')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000202', N'TM000003', N'E', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000203')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000203', N'TM000003', N'E', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000204')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000204', N'TM000003', N'E', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000205')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000205', N'TM000003', N'E', 10, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000206')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000206', N'TM000003', N'F', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000207')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000207', N'TM000003', N'F', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000208')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000208', N'TM000003', N'F', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000209')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000209', N'TM000003', N'F', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000210')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000210', N'TM000003', N'F', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000211')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000211', N'TM000003', N'F', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000212')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000212', N'TM000003', N'F', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000213')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000213', N'TM000003', N'F', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000214')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000214', N'TM000003', N'F', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000215')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000215', N'TM000003', N'F', 10, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000216')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000216', N'TM000003', N'G', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000217')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000217', N'TM000003', N'G', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000218')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000218', N'TM000003', N'G', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000219')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000219', N'TM000003', N'G', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000220')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000220', N'TM000003', N'G', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000221')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000221', N'TM000003', N'G', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000222')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000222', N'TM000003', N'G', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000223')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000223', N'TM000003', N'G', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000224')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000224', N'TM000003', N'G', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000225')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000225', N'TM000003', N'G', 10, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000226')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000226', N'TM000003', N'H', 1, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000227')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000227', N'TM000003', N'H', 2, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000228')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000228', N'TM000003', N'H', 3, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000229')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000229', N'TM000003', N'H', 4, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000230')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000230', N'TM000003', N'H', 5, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000231')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000231', N'TM000003', N'H', 6, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000232')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000232', N'TM000003', N'H', 7, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000233')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000233', N'TM000003', N'H', 8, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000234')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000234', N'TM000003', N'H', 9, N'VIP');
IF NOT EXISTS (SELECT 1 FROM dbo.[Template_Seat] WHERE [template_seat_id]='TS000235')
INSERT [dbo].[Template_Seat] ([template_seat_id], [template_id], [row_label], [seat_number], [seat_type]) VALUES (N'TS000235', N'TM000003', N'H', 10, N'VIP');
-- Support current String Room key and target INT IDENTITY, preserving IDs 1-3.
IF COLUMNPROPERTY(OBJECT_ID('dbo.Room'),'room_id','IsIdentity')=1 SET IDENTITY_INSERT dbo.Room ON;
IF EXISTS (SELECT 1 FROM dbo.Room WHERE room_id='1' AND (template_id IS NULL OR template_id<>'TM000001')) THROW 51201, 'Existing room has another template; reconcile manually.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.Room WHERE room_id='1') INSERT dbo.Room(room_id,template_id,status) VALUES ('1','TM000001','ACTIVE');
IF EXISTS (SELECT 1 FROM dbo.Room WHERE room_id='2' AND (template_id IS NULL OR template_id<>'TM000002')) THROW 51201, 'Existing room has another template; reconcile manually.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.Room WHERE room_id='2') INSERT dbo.Room(room_id,template_id,status) VALUES ('2','TM000002','ACTIVE');
IF EXISTS (SELECT 1 FROM dbo.Room WHERE room_id='3' AND (template_id IS NULL OR template_id<>'TM000003')) THROW 51201, 'Existing room has another template; reconcile manually.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.Room WHERE room_id='3') INSERT dbo.Room(room_id,template_id,status) VALUES ('3','TM000003','ACTIVE');
IF COLUMNPROPERTY(OBJECT_ID('dbo.Room'),'room_id','IsIdentity')=1 SET IDENTITY_INSERT dbo.Room OFF;
-- Reserve new fixture IDs: reruns never move existing showtimes to another date.
DECLARE @day DATE=DATEADD(day,1,CONVERT(date,SYSDATETIMEOFFSET() AT TIME ZONE 'SE Asia Standard Time'));
IF EXISTS (SELECT 1 FROM dbo.Room WHERE room_id IN ('1','2') AND UPPER(status)<>'ACTIVE') THROW 51202, 'Demo rooms must be ACTIVE; existing status was not changed.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE showtime_id='SW00023')
BEGIN
 IF EXISTS (SELECT 1 FROM dbo.Showtime WHERE room_id='1' AND show_date=@day AND start_time<'12:15:00' AND end_time>'10:00:00') THROW 51203, 'Demo slot overlaps an existing showtime.', 1;
 INSERT dbo.Showtime(showtime_id,movie_id,room_id,show_date,start_time,end_time,sales_status) VALUES ('SW00023','MV000016','1',@day,'10:00:00','12:15:00','OPEN');
END;
IF NOT EXISTS (SELECT 1 FROM dbo.Showtime WHERE showtime_id='SW00024')
BEGIN
 IF EXISTS (SELECT 1 FROM dbo.Showtime WHERE room_id='2' AND show_date=@day AND start_time<'12:15:00' AND end_time>'10:00:00') THROW 51203, 'Demo slot overlaps an existing showtime.', 1;
 INSERT dbo.Showtime(showtime_id,movie_id,room_id,show_date,start_time,end_time,sales_status) VALUES ('SW00024','MV000016','2',@day,'10:00:00','12:15:00','OPEN');
END;
-- Room 3 deliberately gets no demo showtime, to test status changes (other existing schedules still count).
COMMIT;
SELECT r.room_id,r.template_id,r.status,COUNT(s.template_seat_id) AS seats FROM dbo.Room r JOIN dbo.Template_Seat s ON s.template_id=r.template_id WHERE r.room_id IN ('1','2','3') GROUP BY r.room_id,r.template_id,r.status;
SELECT * FROM dbo.Showtime WHERE showtime_id IN ('SW00023','SW00024');
