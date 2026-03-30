-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Mar 30, 2026 at 08:31 AM
-- Server version: 8.4.3
-- PHP Version: 8.3.26

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `fitnesstracker`
--

-- --------------------------------------------------------

--
-- Table structure for table `activities`
--

CREATE TABLE `activities` (
  `id` int NOT NULL,
  `user_id` int NOT NULL,
  `activity_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `duration` int NOT NULL,
  `distance` decimal(10,2) DEFAULT '0.00',
  `calories` int DEFAULT '0',
  `notes` text COLLATE utf8mb4_general_ci,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `activities`
--

INSERT INTO `activities` (`id`, `user_id`, `activity_type`, `duration`, `distance`, `calories`, `notes`, `created_at`) VALUES
(1, 1, 'Walking', 0, 0.00, 0, 'Tracked via GPS', '2026-03-19 04:19:21'),
(2, 1, 'Weightlifting', 12, 10.00, 120, '', '2026-03-19 05:42:31'),
(3, 1, 'Swimming', 25, 0.00, 204, '', '2026-03-19 05:42:50'),
(4, 1, 'Walking', 0, 0.00, 0, 'Tracked via GPS', '2026-03-20 02:42:27'),
(5, 1, 'Running', 0, 0.00, 0, 'Tracked via GPS', '2026-03-20 02:43:10'),
(6, 1, 'Walking', 0, 0.00, 0, 'Tracked via GPS', '2026-03-20 03:57:03'),
(7, 1, 'Walking', 0, 0.00, 0, 'Tracked via GPS', '2026-03-20 03:57:41'),
(8, 1, 'Walking', 0, 0.00, 0, 'Tracked via GPS', '2026-03-20 04:00:50'),
(9, 1, 'Walking', 1, 0.00, 0, 'Tracked via GPS', '2026-03-20 04:38:34'),
(10, 1, 'Running', 0, 0.00, 0, 'Tracked via GPS', '2026-03-20 04:38:51'),
(11, 1, 'Walking', 0, 0.01, 0, 'Tracked via GPS', '2026-03-20 04:39:32'),
(12, 1, 'Running', 0, 0.00, 0, 'Tracked via GPS', '2026-03-25 09:39:20'),
(13, 1, 'Weightlifting', 15, 0.00, 135, '', '2026-03-25 09:39:32'),
(14, 1, 'Swimming', 30, 2.00, 245, '', '2026-03-25 09:39:46');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `name`, `email`, `password`, `created_at`) VALUES
(1, 'Test User', 'test@test.com', '$2y$10$xXyG7/his2Ggt/UhqS0hQObd1u2dEqMnHws73Vu3o1hq4odK6vKv2', '2026-03-19 04:00:57');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `activities`
--
ALTER TABLE `activities`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `activities`
--
ALTER TABLE `activities`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=15;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `activities`
--
ALTER TABLE `activities`
  ADD CONSTRAINT `activities_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
