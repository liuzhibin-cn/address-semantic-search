-- MySQL dump 10.13  Distrib 5.7.12, for osx10.11 (x86_64)
--
-- Host: localhost    Database: my_research
-- ------------------------------------------------------
-- Server version	5.7.12

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `addr_address`
--

DROP TABLE IF EXISTS `addr_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `addr_address` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT 'Address Record ID',
  `province_id` int(11) NOT NULL DEFAULT '0' COMMENT 'Province ID',
  `city_id` int(11) NOT NULL DEFAULT '0' COMMENT 'City ID',
  `county_id` int(11) NOT NULL DEFAULT '0' COMMENT 'County ID',
  `text` varchar(100) NOT NULL DEFAULT '' COMMENT 'Address Text',
  `town` varchar(20) NOT NULL DEFAULT '' COMMENT '镇',
  `village` varchar(5) NOT NULL DEFAULT '' COMMENT '村',
  `road` varchar(8) NOT NULL DEFAULT '' COMMENT '道路',
  `road_num` varchar(10) NOT NULL DEFAULT '' COMMENT '道路号码',
  `building_num` varchar(20) NOT NULL DEFAULT '' COMMENT '几号楼+几单元+房间号',
  `hash` int(11) NOT NULL DEFAULT '0' COMMENT 'Address Text Hash Code',
  `raw_text` varchar(150) NOT NULL DEFAULT '' COMMENT 'Original Address Text',
  `prop1` varchar(20) NOT NULL DEFAULT '' COMMENT '扩展字段：订单号',
  `prop2` varchar(20) NOT NULL DEFAULT '' COMMENT '扩展字段：片区ID',
  `create_time` date NOT NULL DEFAULT '1900-01-01',
  PRIMARY KEY (`id`),
  KEY `ix_hash` (`hash`),
  KEY `ix_pid_cid_did` (`province_id`,`city_id`,`county_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1053879 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `bas_region`
--

DROP TABLE IF EXISTS `bas_region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bas_region` (
  `id` int(11) unsigned NOT NULL DEFAULT '0',
  `parent_id` int(11) unsigned NOT NULL DEFAULT '0',
  `type` int(11) unsigned NOT NULL DEFAULT '420',
  `name` varchar(25) NOT NULL DEFAULT '',
  `alias` varchar(25) NOT NULL DEFAULT '',
  `zip` varchar(8) NOT NULL DEFAULT '',
  `source` varchar(5) NOT NULL DEFAULT 'JD' COMMENT '来源',
  `created` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ix_parentid` (`parent_id`,`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tmp_splitted_values`
--

DROP TABLE IF EXISTS `tmp_splitted_values`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `tmp_splitted_values` (
  `type` int(11) NOT NULL,
  `refid` int(11) NOT NULL,
  `val` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2016-10-18 21:01:14
