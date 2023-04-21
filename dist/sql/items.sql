CREATE TABLE IF NOT EXISTS `items` (
  `owner_id` INT,
  `object_id` INT NOT NULL DEFAULT 0,
  `item_id` SMALLINT UNSIGNED NOT NULL,
  `count` INT UNSIGNED NOT NULL DEFAULT 0,
  `enchant_level` SMALLINT UNSIGNED NOT NULL DEFAULT 0,
  `loc` VARCHAR(10),
  `loc_data` INT,
  `custom_type1` INT NOT NULL DEFAULT 0,
  `custom_type2` INT NOT NULL DEFAULT 0,
  `mana_left` INT NOT NULL DEFAULT -1,
  `time` BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`object_id`)
);

CREATE TABLE `items_delayed` (
  `payment_id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) NOT NULL,
  `item_id` smallint(5) unsigned NOT NULL,
  `count` int(10) unsigned NOT NULL DEFAULT '1',
  `payment_status` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `description` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`payment_id`),
  KEY `key_owner_id` (`owner_id`),
  KEY `key_item_id` (`item_id`)
);