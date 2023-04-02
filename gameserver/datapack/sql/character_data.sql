DROP TABLE IF EXISTS `character_data`;
CREATE TABLE `character_data` (
  `charId` int(11) NOT NULL,
  `valueName` varchar(32) NOT NULL,
  `valueData` varchar(250) DEFAULT NULL,
  PRIMARY KEY (`charId`,`valueName`)
) DEFAULT CHARSET=utf8;