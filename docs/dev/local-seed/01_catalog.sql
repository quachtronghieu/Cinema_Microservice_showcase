USE [catalog_db];
-- LOCAL/DEV ONLY. Run against the named database after its schema exists.
-- Insert only: existing rows are not updated or deleted. Stop application writes while seeding.
SET NOCOUNT ON;
SET XACT_ABORT ON;
SET QUOTED_IDENTIFIER ON;

BEGIN TRAN;
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000002')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000002', N'CUC VANG CUA NGOAI', 120, N'Adventure', CAST(N'2026-10-16' AS Date), N'Inspired by the sweet memories of childhood, My Little Grandma tells a heartfelt story about the bond between a grandmother and her grandchild in a small, close-knit neighborhood. Mrs. Hau', N'T13', N'/assets/img/movies/CUC_VANG_CUA_NGOAI.jpg', N'https://www.youtube.com/watch?v=_cj77qa_wMc');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000003')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000003', N'CHI NGA EM NANG', 122, N'Drama', CAST(N'2025-10-20' AS Date), N'LIFT ME UP is an emotional story between siblings ', N'T13', N'/assets/img/movies/CHI_NGA_EM_NANG.jpg', N'https://www.youtube.com/watch?v=RXY460a2vOM');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000004')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000004', N'PHA DAM SINH NHAT ME', 133, N'Drama', CAST(N'2025-11-25' AS Date), N'Pha dam sinh nhat me la bo phim noi ve chu de chinh kich xay ra trong mot gia dinh ', N'T13', N'/assets/img/movies/PHA_DAM_SINH_NHAT_BAN.jpg', N'https://www.youtube.com/watch?v=49jApA-qvpE');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000005')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000005', N'LAM GIAU VOI MA: CUOC CHIEN HOT XOAN', 133, N'Comedy', CAST(N'2025-12-11' AS Date), N'Cuoc chien cua nhung con nguoi tham vong co duoc chiec nhan hot xoan 9 ty', N'T16', N'/assets/img/movies/LAM_GIU_VOI_MA_CUOC_CHIEN_HOT_XOAN.jpg', N'https://www.youtube.com/watch?v=Z12BkmUX57Q');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000007')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000007', N'TEE YOD: QUY AN TANG 3', 119, N'Horror', CAST(N'2025-12-15' AS Date), N'Yak and his family have to face the new horror when "Yee" the youngest sister disappeared mysteriously, Yak have to travel to "Bong Sa Noh Bian" the ghosts forest along with Yos, Yod and Papan to save Yee before The evil spirits are awakened once again.', N'T18', N'/assets/img/movies/TEE_YOD_QUY_AN_TANG_PHAN_3.jpg', N'https://www.youtube.com/watch?v=DMOGnGokm4c');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000008')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000008', N'NHA MA XO', 126, N'Horror', CAST(N'2025-11-24' AS Date), N'Ma Xo''s House revolves around the story of Mrs. Hien, a woman who raised 3 children alone after her husband''s accident died. It all started when the middle son, in a fishing net, picked up a crockery spout with a closed lid.', N'16', N'/assets/img/movies/NHA_MA_XO.jpg', N'https://www.youtube.com/watch?v=ZEq0D-Y0VeU');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000009')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000009', N'CU NHAY KI DIEU', 120, N'Crime', CAST(N'2025-11-30' AS Date), N'Phim rat hay', N'T13', N'/assets/img/movies/CU_NHAY_KY_DIEU.jpg', N'https://www.youtube.com/watch?v=DMOGnGokm4c');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000010')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000010', N'HEO 5 MONG', 120, N'Horror', CAST(N'2026-04-24' AS Date), N'Heo 5 mong', N'T18', N'/assets/img/movies/HEO_5_MONG.jpg', N'https://www.youtube.com/watch?v=GG6Zrjn0UpE');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000011')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000011', N'ANH HUNG', 120, N'Drama', CAST(N'2026-07-07' AS Date), N'kahsgd kjahsd ;kams dpaojsd', N'T13', N'/assets/img/movies/AVATAR_DONG_CHAY_CUA_NUOC.jpg', N'https://www.youtube.com/watch?v=Z12BkmUX57Q');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000012')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000012', N'ANH HUNG 2', 130, N'Action', CAST(N'2026-07-08' AS Date), N'hjusud uashdkjb', N'T13', N'/assets/img/movies/f4a5677f-41a5-41c5-9dd5-b40f75652208_AVATAR_3_LUA_VA_TRO_TAN.jpg', N'https://www.youtube.com/watch?v=GG6Zrjn0UpE');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000014')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000014', N'LENH HUONG 2', 123, N'Drama', CAST(N'2026-09-18' AS Date), N'An owner of a struggling funeral home and a young man desperate for cash find themselves bound by a ''fateful contract.'' As one fights to preserve her trade and the other scrambles to save his ill mother, a dark secret about a long-buried debt begins to unravel.', N'T18', N'/assets/img/movies/6cf378be-c57a-45db-8305-7e773e359afa_LEN_HUONG.jpg', N'https://www.youtube.com/watch?v=ZImBtmy3XrA');
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id = 'MV000015')
INSERT [dbo].[Movie] ([movie_id], [title], [duration], [genre], [release_date], [summary], [age_rating], [img], [trailer]) VALUES (N'MV000015', N'ABC', 123, N'Drama', CAST(N'2026-08-22' AS Date), N'dhusu nsjkmnc jasskm c knu', N'T13', N'/assets/img/movies/9340c2fe-f727-4295-a19d-ffa5b2596f5d_images (1).jpg', N'https://www.youtube.com/watch?v=_cj77qa_wMc');
-- Separate demo movie: source movie dates and IDs remain unchanged.
IF NOT EXISTS (SELECT 1 FROM dbo.Movie WHERE movie_id='MV000016')
INSERT dbo.Movie(movie_id,title,duration,genre,release_date,summary,age_rating,img,trailer,active,version)
VALUES ('MV000016','NGAN LOCAL DEMO',120,'Adventure',DATEADD(day,-1,CONVERT(date,SYSDATETIMEOFFSET() AT TIME ZONE 'SE Asia Standard Time')),'Local seat-map test fixture','T13','/assets/img/movies/CUC_VANG_CUA_NGOAI.jpg','https://www.youtube.com/watch?v=_cj77qa_wMc',1,0);
COMMIT;
SELECT movie_id,title,release_date,active FROM dbo.Movie ORDER BY movie_id;
