@echo off
title RUSaCis account manager console
@java -Djava.util.logging.config.file=config/console.cfg -cp ./libs/*; net.sf.l2j.accountmanager.SQLAccountManager
@pause
